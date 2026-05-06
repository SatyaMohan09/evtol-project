"""
eVTOL Obstacle Detection Service — v5
======================================
No YOLO (avoids COCO false positives on 3-D renders).
Pure OpenCV colour-segmentation detector tuned for Three.js scenes.

Receives frames from the frontend at 10 fps, saves them to
  captured_frames/          ← raw PNGs
  captured_frames/annotated/ ← annotated PNGs (YOLO-style bboxes)

World-coordinate pipeline (quaternion-corrected):
  pixel → camera-space ray → depth from apparent height
  → rotate by eVTOL quaternion → translate by eVTOL position

Run:
    pip install flask flask-cors opencv-python numpy Pillow
    python yolo_service.py
"""

import os, base64, math, logging
from io import BytesIO
from datetime import datetime

import cv2
import numpy as np
from PIL import Image
from flask import Flask, request, jsonify
from flask_cors import CORS

# ── CONFIG ──────────────────────────────────────────────────────────────────
SAVE_DIR      = os.path.join(os.path.dirname(__file__), "captured_frames")
ANNOTATED_DIR = os.path.join(SAVE_DIR, "annotated")
CAMERA_HFOV   = 60.0   # degrees — Three.js PerspectiveCamera fov=60

H_REAL = {             # assumed obstacle real heights (metres)
    "building":  30.0,
    "wall":      30.0,
    "vehicle":    3.0,
    "structure": 10.0,
}

MIN_BBOX_W    = 30
MIN_BBOX_H    = 50
MIN_BBOX_AREA = 2500
NMS_IOU       = 0.50
MAX_DET       = 12

EMA_ALPHA     = 0.15   # temporal smoothing (lower = more stable)
GRID_CELL_M   = 10     # world-grid cell for obstacle identity

os.makedirs(SAVE_DIR,      exist_ok=True)
os.makedirs(ANNOTATED_DIR, exist_ok=True)

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s  %(levelname)s  %(message)s")
log = logging.getLogger(__name__)

app   = Flask(__name__)
CORS(app)
frame_counter = 0
smooth_state: dict = {}


# ══════════════════════════════════════════════════════════════════════════
#  QUATERNION HELPER
# ══════════════════════════════════════════════════════════════════════════

def quat_rotate(q: np.ndarray, v: np.ndarray) -> np.ndarray:
    """Rotate vector v by quaternion q=[x,y,z,w] (Three.js convention)."""
    qx, qy, qz, qw = q
    t = 2.0 * np.array([
        qy*v[2] - qz*v[1],
        qz*v[0] - qx*v[2],
        qx*v[1] - qy*v[0],
    ])
    return v + qw*t + np.array([
        qy*t[2] - qz*t[1],
        qz*t[0] - qx*t[2],
        qx*t[1] - qy*t[0],
    ])


# ══════════════════════════════════════════════════════════════════════════
#  GEOMETRY — pixel → world (quaternion-corrected)
# ══════════════════════════════════════════════════════════════════════════

def pixel_to_world(x_min, y_min, x_max, y_max,
                   img_W, img_H,
                   ex, ey, ez, qx, qy, qz, qw,
                   label="structure", hfov=CAMERA_HFOV):
    f    = (img_W / 2.0) / math.tan(math.radians(hfov) / 2.0)
    cx   = img_W / 2.0
    cy   = img_H / 2.0
    h_px = max(y_max - y_min, 1)
    w_px = max(x_max - x_min, 1)
    u    = (x_min + x_max) / 2.0
    v    = y_max   # base of bbox — most stable anchor

    h_real = H_REAL.get(label, 10.0)
    Z      = (f * h_real) / h_px

    # Camera-space point (Three.js: +X right, +Y up, -Z forward)
    P_cam = np.array([ (u-cx)/f*Z, -(v-cy)/f*Z, -Z ])

    # Rotate to world space
    q = np.array([qx, qy, qz, qw])
    n = np.linalg.norm(q)
    q = q / n if n > 1e-6 else np.array([0., 0., 0., 1.])
    P_rel = quat_rotate(q, P_cam)

    return {
        "X_world":  round(float(P_rel[0] + ex), 2),
        "Y_world":  round(float(P_rel[1] + ey), 2),
        "Z_world":  round(float(P_rel[2] + ez), 2),
        "distance": round(float(np.linalg.norm(P_cam)), 2),
        "radius":   round(float((w_px / img_W) * Z), 2),
    }


def smooth_coords(label, raw):
    gx = int(raw["X_world"] / GRID_CELL_M)
    gy = int(raw["Y_world"] / GRID_CELL_M)
    gz = int(raw["Z_world"] / GRID_CELL_M)
    key = (label, gx, gy, gz)
    if key in smooth_state:
        prev = smooth_state[key]
        merged = {k: EMA_ALPHA*raw[k] + (1-EMA_ALPHA)*prev[k]
                  for k in ("X_world","Y_world","Z_world","distance","radius")}
    else:
        merged = dict(raw)
    smooth_state[key] = merged
    return {k: round(merged[k], 2)
            for k in ("X_world","Y_world","Z_world","distance","radius")}


# ══════════════════════════════════════════════════════════════════════════
#  SCENE DETECTOR — colour-segmentation for Three.js renders
# ══════════════════════════════════════════════════════════════════════════

def make_obstacle_mask(img_rgb):
    H, W = img_rgb.shape[:2]
    hsv  = cv2.cvtColor(img_rgb, cv2.COLOR_RGB2HSV)

    sky = cv2.inRange(hsv,
        np.array([85,  8, 140], dtype=np.uint8),
        np.array([145,145, 255], dtype=np.uint8))
    sky[:int(H*0.12), :] = 255

    gnd = cv2.bitwise_or(
        cv2.inRange(hsv, np.array([22, 25, 25],dtype=np.uint8),
                         np.array([92,255,185],dtype=np.uint8)),
        cv2.inRange(hsv, np.array([ 5, 15, 25],dtype=np.uint8),
                         np.array([28,210,165],dtype=np.uint8))
    )
    gnd[int(H*0.80):, :] = 255

    horizon_y = int(H*0.12)
    for r in range(int(H*0.12), H//2):
        if np.mean(sky[r] > 0) < 0.20:
            horizon_y = r; break

    ground_y = int(H*0.80)
    for r in range(int(H*0.80), horizon_y, -1):
        if np.mean(gnd[r] > 0) < 0.20:
            ground_y = r; break

    mask = cv2.bitwise_and(cv2.bitwise_not(sky), cv2.bitwise_not(gnd))
    k    = cv2.getStructuringElement(cv2.MORPH_RECT, (5,5))
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN,  k, iterations=1)
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, k, iterations=2)
    return mask, horizon_y, ground_y


def scene_detect(img_rgb):
    H, W   = img_rgb.shape[:2]
    mask, horizon_y, ground_y = make_obstacle_mask(img_rgb)

    grey   = cv2.bilateralFilter(cv2.cvtColor(img_rgb, cv2.COLOR_RGB2GRAY), 7, 60, 60)
    edges  = cv2.bitwise_and(cv2.Canny(grey, 25, 75),
                              cv2.Canny(grey, 25, 75), mask=mask)
    closed = cv2.morphologyEx(
        cv2.bitwise_or(edges, mask),
        cv2.MORPH_CLOSE,
        cv2.getStructuringElement(cv2.MORPH_RECT, (9,9)),
        iterations=3)

    contours, _ = cv2.findContours(closed, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    contours     = sorted(contours, key=cv2.contourArea, reverse=True)

    detections, used = [], []
    for cnt in contours:
        if len(detections) >= MAX_DET: break
        x, y, w, h = cv2.boundingRect(cnt)
        if w < MIN_BBOX_W or h < MIN_BBOX_H or w*h < MIN_BBOX_AREA: continue
        if y >= ground_y or (y+h) <= horizon_y: continue
        if np.mean(mask[y:y+h, x:x+w] > 0) < 0.15: continue

        skip = False
        for (rx,ry,rw,rh) in used:
            ix = max(0, min(x+w,rx+rw) - max(x,rx))
            iy = max(0, min(y+h,ry+rh) - max(y,ry))
            inter = ix*iy
            union = w*h + rw*rh - inter
            if union > 0 and inter/union > NMS_IOU:
                skip = True; break
        if skip: continue
        used.append((x,y,w,h))

        aspect = w / max(h,1)
        if   aspect < 0.75 and h > 100: label, conf = "building", 0.82
        elif w > W*0.50:                 label, conf = "wall",     0.72
        elif aspect > 2.5 and h < 80:   label, conf = "vehicle",  0.68
        else:                            label, conf = "structure", 0.70

        detections.append({
            "label": label, "confidence": conf,
            "x_min": float(x), "y_min": float(y),
            "x_max": float(x+w), "y_max": float(y+h),
            "x_img": x + w/2.0, "y_img": y + h/2.0,
        })
    return detections


# ══════════════════════════════════════════════════════════════════════════
#  ANNOTATE — YOLO-style bboxes (filled label bar + cross-hair + base dot)
# ══════════════════════════════════════════════════════════════════════════

COLOURS = {
    "building":  (0,  80, 255),
    "wall":      (0, 165, 255),
    "structure": (0, 200, 255),
    "vehicle":   (0, 200,  50),
    "default":   (0,   0, 255),
}

def annotate_and_encode(img_rgb, detections, coords_list):
    img  = cv2.cvtColor(img_rgb, cv2.COLOR_RGB2BGR)
    FONT = cv2.FONT_HERSHEY_SIMPLEX
    PAD  = 5
    for det, c in zip(detections, coords_list):
        x1,y1 = int(det["x_min"]), int(det["y_min"])
        x2,y2 = int(det["x_max"]), int(det["y_max"])
        col   = COLOURS.get(det["label"], COLOURS["default"])

        cv2.rectangle(img, (x1,y1), (x2,y2), col, 2)

        tag = (f"{det['label']} {det['confidence']:.2f}  "
               f"W({c['X_world']:.0f},{c['Y_world']:.0f},{c['Z_world']:.0f})  "
               f"D:{c['distance']:.0f}m")
        (tw,th), _ = cv2.getTextSize(tag, FONT, 0.50, 1)
        bar_y1 = max(y1-th-2*PAD, 0); bar_y2 = max(y1, th+2*PAD)
        cv2.rectangle(img, (x1,bar_y1), (x1+tw+PAD*2,bar_y2), col, -1)
        cv2.putText(img, tag, (x1+PAD, bar_y2-PAD), FONT, 0.50, (255,255,255), 1, cv2.LINE_AA)

        cv2.drawMarker(img, (int(det["x_img"]),int(det["y_img"])), col,
                       cv2.MARKER_CROSS, 16, 2)
        cv2.circle(img, (int(det["x_img"]),int(det["y_max"])), 5, col, -1)

    ok, buf = cv2.imencode(".png", img)
    if not ok: return ""
    return "data:image/png;base64," + base64.b64encode(buf.tobytes()).decode()


# ══════════════════════════════════════════════════════════════════════════
#  FILE HELPERS
# ══════════════════════════════════════════════════════════════════════════

def save_raw(pil_img, idx, ts):
    p = os.path.join(SAVE_DIR, f"{ts}_frame_{idx:06d}.png")
    pil_img.save(p, "PNG"); return p

def save_annotated(b64, idx, ts):
    if not b64: return
    data = base64.b64decode(b64.split(",")[1])
    bgr  = cv2.imdecode(np.frombuffer(data, np.uint8), cv2.IMREAD_COLOR)
    if bgr is not None:
        cv2.imwrite(os.path.join(ANNOTATED_DIR,
                    f"{ts}_frame_{idx:06d}_annotated.png"), bgr)


# ══════════════════════════════════════════════════════════════════════════
#  FLASK ROUTES
# ══════════════════════════════════════════════════════════════════════════

@app.route("/health")
def health():
    return jsonify({"status":"ok","detector":"scene_v5",
                    "frames_saved":frame_counter,"smooth_slots":len(smooth_state)})


@app.route("/reset_smooth", methods=["POST"])
def reset_smooth():
    global smooth_state; smooth_state = {}
    return jsonify({"ok": True})


@app.route("/detect", methods=["POST"])
def detect():
    global frame_counter
    try:
        p    = request.get_json(force=True)
        b64  = p.get("frame", "")
        fidx = int(p.get("frameIndex", frame_counter))
        ev   = p.get("evtol",  {})
        cam  = p.get("camera", {})

        ex  = float(ev.get("x",  0)); ey = float(ev.get("y",  0)); ez = float(ev.get("z",  0))
        qx  = float(ev.get("qx", 0)); qy = float(ev.get("qy", 0))
        qz  = float(ev.get("qz", 0)); qw = float(ev.get("qw", 1))
        hfov= float(cam.get("fov", CAMERA_HFOV))

        if "," in b64: b64 = b64.split(",",1)[1]
        pil  = Image.open(BytesIO(base64.b64decode(b64))).convert("RGB")
        W, H = pil.size
        img  = np.array(pil)

        ts       = datetime.utcnow().strftime("%Y%m%d_%H%M%S")
        raw_path = save_raw(pil, fidx, ts)
        frame_counter += 1

        dets = scene_detect(img)
        coords_list = []
        for d in dets:
            raw  = pixel_to_world(d["x_min"],d["y_min"],d["x_max"],d["y_max"],
                                   W,H,ex,ey,ez,qx,qy,qz,qw,d["label"],hfov)
            coords_list.append(smooth_coords(d["label"], raw))

        ann_b64 = annotate_and_encode(img, dets, coords_list)
        if dets: save_annotated(ann_b64, fidx, ts)

        log.info(f"Frame {fidx:>4d}: {len(dets)} det(s)  "
                 f"pos=({ex:.0f},{ey:.0f},{ez:.0f})  {W}x{H}")
        for d,c in zip(dets,coords_list):
            log.info(f"  {d['label']:10s}  W({c['X_world']:8.1f},{c['Y_world']:6.1f},{c['Z_world']:8.1f})  D:{c['distance']:6.1f}m")

        obstacles = [
            {"label":d["label"],"confidence":d["confidence"],
             "bbox":{"x_min":round(d["x_min"],1),"y_min":round(d["y_min"],1),
                     "x_max":round(d["x_max"],1),"y_max":round(d["y_max"],1)},
             **c}
            for d,c in zip(dets,coords_list)
        ]
        return jsonify({"frameIndex":fidx,"savedPath":raw_path,
                        "obstacles":obstacles,"annotatedImg":ann_b64})

    except Exception as e:
        log.exception("Error in /detect")
        return jsonify({"error": str(e)}), 500


@app.route("/frames")
def list_frames():
    files = sorted(f for f in os.listdir(SAVE_DIR) if f.endswith(".png"))
    return jsonify({"count":len(files),"files":files})


@app.route("/clear", methods=["POST"])
def clear_frames():
    global frame_counter, smooth_state
    n = 0
    for folder in [SAVE_DIR, ANNOTATED_DIR]:
        for f in os.listdir(folder):
            if f.endswith(".png"):
                os.remove(os.path.join(folder,f)); n+=1
    frame_counter=0; smooth_state={}
    return jsonify({"deleted":n})


if __name__ == "__main__":
    log.info("="*60)
    log.info("  eVTOL Obstacle Detector  (scene v5 — quaternion-corrected)")
    log.info(f"  Frames → {os.path.abspath(SAVE_DIR)}")
    log.info("  Port: 5050")
    log.info("="*60)
    app.run(host="0.0.0.0", port=5050, debug=False)

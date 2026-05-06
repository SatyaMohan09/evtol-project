# eVTOL YOLO Service

## Setup & Run
```bash
cd yolo_service
pip install -r requirements.txt
python yolo_service.py
```
Service runs on **http://localhost:5050**

## What it does
- Receives frames (base64 PNG) at 10fps from the frontend
- Saves raw frames → `captured_frames/`
- Detects buildings/walls/vehicles/structures using OpenCV colour-segmentation (no YOLO/COCO — avoids false positives on 3-D renders)
- Computes world-space coordinates using quaternion-corrected camera→world transform
- Saves annotated frames (YOLO-style bboxes) → `captured_frames/annotated/`
- Returns JSON with obstacle list + annotated image to frontend

## Folder structure after running
```
yolo_service/
├── yolo_service.py
├── requirements.txt
└── captured_frames/
    ├── 20250429_120000_frame_000001.png   ← raw frames
    └── annotated/
        └── 20250429_120000_frame_000001_annotated.png
```

## API
| Method | Path           | Description                    |
|--------|----------------|--------------------------------|
| GET    | /health        | Service status                 |
| POST   | /detect        | Send frame, get obstacles      |
| POST   | /reset_smooth  | Clear EMA state (call on reset)|
| GET    | /frames        | List saved frames              |
| POST   | /clear         | Delete all saved frames        |

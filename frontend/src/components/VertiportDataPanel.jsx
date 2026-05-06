import { useState } from "react";

/**
 * VertiportDataPanel
 * Shows:
 *  - Live downward-camera feed (canvas updated from outside)
 *  - Height vs Apparent-Radius table for takeoff & landing
 */
export default function VertiportDataPanel({ heightRadiusLog, downCamCanvasRef }) {
  const [tab, setTab] = useState("camera"); // "camera" | "table"

  const tabBtn = (id, label, color) => ({
    padding: "6px 14px",
    borderRadius: "6px",
    border: "none",
    cursor: "pointer",
    fontFamily: "Orbitron, sans-serif",
    fontSize: "11px",
    fontWeight: 600,
    background: tab === id ? color : "#1e293b",
    color: tab === id ? "#fff" : "#94a3b8",
    transition: "all 0.2s",
  });

  return (
    <div style={{
      position: "absolute",
      bottom: 10,
      right: 10,
      width: 380,
      maxHeight: 420,
      background: "rgba(8,12,28,0.93)",
      border: "1px solid #1e293b",
      borderRadius: 10,
      zIndex: 50,
      display: "flex",
      flexDirection: "column",
      overflow: "hidden",
      backdropFilter: "blur(8px)",
      boxShadow: "0 4px 32px rgba(0,0,0,0.6)",
    }}>
      {/* Header */}
      <div style={{
        display: "flex", alignItems: "center", justifyContent: "space-between",
        padding: "7px 10px", background: "#0f172a",
        borderBottom: "1px solid #1e293b",
      }}>
        <span style={{ color: "#38bdf8", fontFamily: "Orbitron, sans-serif", fontSize: 12, fontWeight: 700 }}>
          📷 VERTIPORT VISION
        </span>
        <div style={{ display: "flex", gap: 6 }}>
          <button style={tabBtn("camera", "Camera", "#2563eb")} onClick={() => setTab("camera")}>
            CAMERA
          </button>
          <button style={tabBtn("table", "Table", "#7c3aed")} onClick={() => setTab("table")}>
            H/R TABLE
          </button>
        </div>
      </div>

      {/* Camera tab */}
      {tab === "camera" && (
        <div style={{ padding: 8, display: "flex", flexDirection: "column", gap: 6 }}>
          <div style={{ color: "#94a3b8", fontFamily: "monospace", fontSize: 10, marginBottom: 2 }}>
            ⬇ Downward-facing camera — real-time vertiport view
          </div>
          <canvas
            ref={downCamCanvasRef}
            width={360}
            height={200}
            style={{
              width: "100%",
              borderRadius: 6,
              border: "1px solid #334155",
              background: "#000",
              imageRendering: "pixelated",
            }}
          />
          <div style={{ color: "#475569", fontFamily: "monospace", fontSize: 9, textAlign: "center" }}>
            As altitude ↑ → vertiport circle appears smaller | As altitude ↓ → circle appears larger
          </div>
        </div>
      )}

      {/* H/R Table tab */}
      {tab === "table" && (
        <div style={{ padding: 8, overflowY: "auto", flex: 1 }}>
          <div style={{ color: "#94a3b8", fontFamily: "monospace", fontSize: 10, marginBottom: 6 }}>
            Apparent radius (px) of vertiport circle at each sampled height
          </div>
          <table style={{ width: "100%", borderCollapse: "collapse", fontFamily: "monospace", fontSize: 11 }}>
            <thead>
              <tr style={{ background: "#0f172a", color: "#38bdf8" }}>
                <th style={th}>#</th>
                <th style={th}>Phase</th>
                <th style={th}>Height (m)</th>
                <th style={th}>Apparent R (px)</th>
                <th style={th}>R/H ratio</th>
              </tr>
            </thead>
            <tbody>
              {heightRadiusLog.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ ...td, color: "#334155", textAlign: "center", padding: 16 }}>
                    No data yet — play the simulation
                  </td>
                </tr>
              ) : (
                heightRadiusLog.map((row, i) => (
                  <tr
                    key={i}
                    style={{ background: i % 2 === 0 ? "#0a0f1e" : "#060b16" }}
                  >
                    <td style={{ ...td, color: "#475569" }}>{i + 1}</td>
                    <td style={{
                      ...td,
                      color: row.phase === "TAKEOFF" ? "#4ade80" : "#f87171",
                      fontWeight: 600,
                    }}>{row.phase}</td>
                    <td style={{ ...td, color: "#e2e8f0" }}>{row.height.toFixed(1)}</td>
                    <td style={{ ...td, color: "#fbbf24" }}>{row.apparentRadius.toFixed(1)}</td>
                    <td style={{ ...td, color: "#94a3b8" }}>{row.ratio.toFixed(4)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          {heightRadiusLog.length > 0 && (
            <div style={{
              marginTop: 8, padding: "6px 10px", background: "#0f172a",
              borderRadius: 6, border: "1px solid #1e293b",
              fontFamily: "monospace", fontSize: 10, color: "#94a3b8",
            }}>
              <strong style={{ color: "#38bdf8" }}>Physics note: </strong>
              Apparent radius R ∝ 1/H — as height doubles, vertiport circle halves in size.
              The R×H product (constant) = <strong style={{ color: "#fbbf24" }}>
                {(heightRadiusLog.reduce((s, r) => s + r.apparentRadius * r.height, 0) /
                  heightRadiusLog.length).toFixed(0)}
              </strong> px·m (avg)
            </div>
          )}
        </div>
      )}
    </div>
  );
}

const th = {
  padding: "5px 8px",
  textAlign: "left",
  borderBottom: "1px solid #1e293b",
  fontWeight: 700,
  fontSize: 10,
};
const td = {
  padding: "4px 8px",
  borderBottom: "1px solid #0f172a",
};

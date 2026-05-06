import { useEffect, useRef, useState } from "react";
import PlaybackControls from "./controls/PlaybackControls";
import UnifiedVisualizationScene from "./UnifiedVisualizationScene";
import { FiCamera } from "react-icons/fi";
export default function VisualizationSection() {
  const sectionRef = useRef();
  const sceneRef = useRef();
  const [visible, setVisible] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [missionStatus, setMissionStatus] = useState("IDLE");
  const [alerts, setAlerts] = useState([]);
  // const [mode, setMode] = useState("3D");
  const [cameraMode, setCameraMode] = useState("OVERVIEW");
  const [dronePosition, setDronePosition] = useState({
    x: 0, y: 0, z: 0, speed: 0, altitude: 0, time: 0, distance: 0, heading: 0,
  });

  useEffect(() => {
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) setVisible(true);
    }, { threshold: 0.3 });
    if (sectionRef.current) observer.observe(sectionRef.current);
    return () => observer.disconnect();
  }, []);

  
  function getHeadingDirection(angle) {
    const directions = ["N", "NE", "E", "SE", "S", "SW", "W", "NW"];
    const index = Math.round(angle / 45) % 8;
    return directions[index < 0 ? index + 8 : index];
  }

  return (
    <section
      id="visualization"
      ref={sectionRef}
      style={{
        ...styles.section,
        opacity: visible ? 1 : 0,
        transform: visible ? "translateY(0)" : "translateY(80px)",
        transition: "all 1s ease",
      }}
    >
        <div style={styles.header}>
        {/* Left side title */}
        <div>EVTOL Mission Command Center</div>
 
        {/* Right side: REC indicator + capture button */}
        <div style={{ display:"flex", alignItems:"center", gap:"8px" }}>
          {isPlaying && (
            <span style={{ color:"#f87171", fontSize:"12px", fontFamily:"monospace",
                           display:"flex", alignItems:"center", gap:4 }}>
              ● REC 10fps
            </span>
          )}
          <button
            onClick={() => sceneRef.current?.exportHeightRadiusCSV?.()}
            style={{
              background: "rgba(124,58,237,0.3)",
              border: "1px solid #7c3aed",
              borderRadius: "6px",
              cursor: "pointer",
              padding: "5px 10px",
              color: "#c4b5fd",
              fontFamily: "Orbitron, sans-serif",
              fontSize: "11px",
              fontWeight: 600,
            }}
            title="Export H/R Table as CSV"
          >
            ⬇ H/R CSV
          </button>
          <button
            onClick={() => sceneRef.current?.captureFPV()}
            style={{
              background: "transparent",
              border: "none",
              cursor: "pointer",
              display: "flex",
              alignItems: "",
              padding: "6px",
              transform: "translateY(8px)",
              color: "#d8e4dc",
            }}
            title="Capture FPV"
          >
            <FiCamera size={28} />
          </button>
        </div>
      </div>
 

  <div style={styles.leftPanel}>
  <div style={styles.cameraControls}>
  <button
    onClick={() => setCameraMode("OVERVIEW")}
    style={styles.cameraButton}
  >
    Overview
  </button>

  <button
    onClick={() => setCameraMode("CHASE")}
    style={{
      ...styles.cameraButton,
      background: "#16a34a"
    }}
  >
    Chase
  </button>
  <button
    onClick={() => setCameraMode("FPV")}
    style={{
      ...styles.cameraButton,
      background: "#166fa3"
    }}
  >
    FPV
  </button>
  <button
    onClick={() => setCameraMode("HFOV")}
    style={{
      ...styles.cameraButton,
      background: "#b2b41c"
    }}
  >
    HFOV
  </button>
</div>
        <div style={styles.controlsArea}>
          

          <PlaybackControls
            play={() => {
              const started = sceneRef.current?.play();
              setIsPlaying(Boolean(started));
            }}
            pause={() => { sceneRef.current?.pause(); setIsPlaying(false); }}
            reset={() => { setIsPlaying(false); sceneRef.current?.reset(); }}
            setSpeed={(v) => sceneRef.current?.setSpeed(v)}
            isPlaying={isPlaying}
          />
        </div>

        <div style={styles.telemetry}>
          <div style={styles.card}><strong>Pos:</strong> {dronePosition.x.toFixed(1)}, {dronePosition.y.toFixed(1)}, {dronePosition.z.toFixed(1)}</div>
          <div style={styles.card}><strong>Alt:</strong> {dronePosition.altitude.toFixed(1)} m</div>
          <div style={styles.card}><strong>Spd:</strong> {dronePosition.speed.toFixed(1)} m/s</div>
          <div style={styles.card}><strong>Dir:</strong> {getHeadingDirection(dronePosition.heading)} ({dronePosition.heading.toFixed(0)}°)</div>
          <div style={styles.card}><strong>Status:</strong> {missionStatus}</div>
        </div>
      </div>
    
      <div style={styles.visualArea}>
        <UnifiedVisualizationScene
          ref={sceneRef}
          // mode={mode}
          cameraMode = {cameraMode}
          missionMode={true}
          onPositionUpdate={setDronePosition}
          onMissionUpdate={setMissionStatus}
        />
      </div>
    </section>
  );
}

// ... styles remain same as your original ...

const styles = {
   header: {
    position: "absolute",
    top: 0,
    left: 0,
    width: "100%",
    height: "60px",
    display: "flex",
    alignItems: "center",
 
    justifyContent: "space-between", // added
    paddingLeft: "25px",
    paddingRight: "25px", // added
 
    fontSize: "20px",
    fontFamily: "Orbitron, sans-serif",
    fontWeight: "600",
    letterSpacing: "1px",
    background: "rgba(2,6,23,0.8)",
    backdropFilter: "blur(10px)",
    borderBottom: "1px solid rgba(255,255,255,0.08)",
    zIndex: 100,
  },
 

  section: {
    height: "100vh",
    width: "100%",
    display: "flex",
    overflow: "hidden",
    background:
      "linear-gradient(180deg, #6baed6 0%, #4a90c2 30%, #1e3a5f 70%, #020617 100%)",
    color: "white",
  },

  leftPanel: {
    width: "320px",
    padding: "80px 20px 20px 20px",
    background: "rgba(15,23,42,0.7)",
    borderRight: "1px solid rgba(255,255,255,0.08)",
    display: "flex",
    flexDirection: "column",
    justifyContent: "flex-start",
    gap: "25px",
    fontFamily: "Orbitron, sans-serif",
 
  },
  cameraControls: {
  display: "flex",
  flexDirection: "column",
  gap: "12px",
  background: "rgba(20,20,20,0.7)",
  padding: "15px",
  borderRadius: "10px",
  backdropFilter: "blur(6px)",
  border: "1px solid rgba(255,255,255,0.1)",
  fontFamily: "Orbitron, sans-serif"
},

cameraButton: {
  padding: "10px 16px",
  borderRadius: "8px",
  border: "none",
  background: "#2563eb",
  color: "white",
  fontSize: "14px",
  fontWeight: "600",
  cursor: "pointer",
  transition: "all 0.2s ease",
  fontFamily: "Orbitron, sans-serif"
},

  controlsArea: {
    display: "flex",
    flexDirection: "column",
    gap: "20px",
  },

  zoomControls: {
    display: "flex",
    gap: "10px",
    marginTop: "5px",
    marginleft: "10px",
  },

  zoomButton: {
    width: "50px",
    height: "26px",
    borderRadius: "8px",
    border: "1px solid rgba(255,255,255,0.1)",
    background: "rgba(30,41,59,0.8)",
    color: "white",
    fontSize: "18px",
    cursor: "pointer",
    transition: "all 0.2s ease",
    hover: {
      background: "#38bdf8",
    },
  },

  telemetry: {
    display: "flex",
    flexDirection: "column",
    gap: "10px",
    marginTop: "10px",
  },

  card: {
    background: "rgba(30,41,59,0.7)",
    padding: "10px",
    borderRadius: "10px",
    border: "1px solid rgba(255,255,255,0.08)",
    backdropFilter: "blur(10px)",
  },

  visualArea: {
    flex: 1,
    height: "100%",
    position: "relative",
    overflow: "hidden",
  },

  // grid: {
  //   position: "absolute",
  //   width: "100%",
  //   height: "100%",
  //   backgroundImage:
  //     "linear-gradient(rgba(255,255,255,0.04) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.04) 1px, transparent 1px)",
  //   backgroundSize: "50px 50px",
  //   pointerEvents: "none",
  // },

  toggleContainer: {
    display: "flex",
    alignItems: "center",
    gap: "10px",
    marginBottom: "15px",
    fontFamily: "Orbitron, sans-serif",
 
  },

  toggleLabel: {
    fontSize: "19px",
    opacity: 0.8,
  },

  switch: {
    width: "50px",
    height: "24px",
    background: "#020617",
    border: "1px solid #334155",
    borderRadius: "20px",
    display: "flex",
    alignItems: "center",
    padding: "2px",
    cursor: "pointer",
    transition: "all 0.3s ease",
  },

  knob: {
    width: "20px",
    height: "20px",
    borderRadius: "50%",
    background: "#38bdf8",
    transition: "all 0.3s ease",
  },
};

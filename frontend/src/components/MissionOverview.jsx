import evtolImage from "../assets/evtol_image.jpg";

export default function MissionOverview() {
  const scrollToSimulation = () => {
    const section = document.getElementById("visualization");
    if (section) {
      section.scrollIntoView({ behavior: "smooth" });
    }
  };

  return (
    <div style={styles.page}>
      {/* Animated Grid Background */}
      <div style={styles.grid}></div>

      {/* MAIN CONTENT */}
      <div style={styles.container}>
        {/* LEFT SIDE */}
        <div style={styles.left}>
          <h1 style={styles.title}>EVTOL Mission Simulation</h1>

          <p style={styles.description}>
            Analyze trajectory data and visualize EVTOL flight in an interactive
            simulation environment with playback controls, telemetry monitoring
            and advanced 2D / 3D visualization.
          </p>

          <button style={styles.startButton} onClick={scrollToSimulation}>
            Start Mission Simulation
          </button>

          <div style={styles.features}>
            <div style={styles.card}>📡 Trajectory Data</div>
            <div style={styles.card}>🎮 Playback Control</div>
            <div style={styles.card}>🛰 2D / 3D Visualization</div>
            <div style={styles.card}>📊 Telemetry Monitoring</div>
          </div>
        </div>

        {/* RIGHT SIDE IMAGE */}
        <div style={styles.right}>
          <img src={evtolImage} alt="EVTOL vehicles" style={styles.image} />
        </div>
      </div>
    </div>
  );
}

const styles = {
  page: {
    minHeight: "110vh",
    width: "100%",
    position: "relative",
    overflow: "hidden",
    background:
      "linear-gradient(180deg,#6baed6 0%,#4a90c2 40%,#1e3a5f 70%,#020617 100%)",
    color: "white",
    display: "flex",
    flexDirection: "column",
  },

  container: {
    flex: 1,
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    padding: "40px 80px",
    gap: "80px",
  },

  left: {
    flex: 1,
    display: "flex",
    flexDirection: "column",
    alignItems: "flex-start",
  },

  right: {
    flex: 1.2,
    display: "flex",
    justifyContent: "center",
  },

  title: {
    fontSize: "56px",
    marginBottom: "25px",
    fontFamily: "Orbitron, sans-serif",
    letterSpacing: "2px",
  },

  description: {
    maxWidth: "550px",
    fontSize: "18px",
    lineHeight: "1.7",
    marginBottom: "30px",
    opacity: 0.9,
  },

  startButton: {
    padding: "14px 28px",
    marginBottom: "30px",
    borderRadius: "10px",
    border: "none",
    background: "#38bdf8",
    color: "#020617",
    fontSize: "16px",
    fontWeight: "600",
    cursor: "pointer",
    fontFamily: "Orbitron, sans-serif",
    transition: "all 0.3s ease",
  },

  features: {
    display: "flex",
    flexWrap: "wrap",
    gap: "18px",
  },

  card: {
    background: "rgba(255,255,255,0.12)",
    padding: "14px 20px",
    borderRadius: "10px",
    backdropFilter: "blur(10px)",
    fontFamily: "Orbitron, sans-serif",
    fontSize: "14px",
  },

  image: {
    width: "100%",
    maxWidth: "900px",
    borderRadius: "14px",
    boxShadow: "0 25px 60px rgba(0,0,0,0.4)",
  },
  grid: {
    position: "absolute",
    width: "100%",
    height: "100%",
    top: 0,
    left: 0,
    backgroundImage:
      "linear-gradient(rgba(255,255,255,0.05) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.05) 1px, transparent 1px)",
    backgroundSize: "60px 60px",
    animation: "moveGrid 20s linear infinite",
    pointerEvents: "none",
  },
};

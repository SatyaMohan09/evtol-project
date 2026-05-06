import { useEffect, useState } from "react";
import ThreeHeroModel from "./ThreeHeroModel";

export default function HeroSection() {
  const [offset, setOffset] = useState(0);
  useEffect(() => {
    const handleScroll = () => {
      setOffset(window.scrollY);
    };

    window.addEventListener("scroll", handleScroll);

    return () => window.removeEventListener("scroll", handleScroll);
  }, []);
  return (
    <section style={styles.hero}>
      <div style={styles.modelContainer}>
        <div className="radar-grid"></div>
        <div
          style={{
            transform: `translateY(-${offset * 0.7}px)`,
            transition: "transform 0.1s",
          }}
        >
          <ThreeHeroModel />
        </div>
      </div>
      <div className="radar-glow"></div>
      <div style={styles.textContainer}>
        <h1 className="hero-title">EVTOL SIMULATION</h1>
        <p
          className="scroll-arrow"
          onClick={() =>
            document
              .getElementById("simulation")
              .scrollIntoView({ behavior: "smooth" })
          }
        >
          ↓ Scroll Down
        </p>
      </div>
    </section>
  );
}
const styles = {
  hero: {
    height: "100vh",
    width: "100%",
    background: "linear-gradient(to top, #4DA6FF, #E6F7FF)",
    display: "flex",
    flexDirection: "column",
    justifyContent: "space-between",
  },
  modelContainer: {
    height: "70%",
    width: "100%",
  },
  textContainer: {
    height: "70%",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    color: "white",
  },
  title: {
    fontSize: "10.5rem",
    margin: 0,
    letterSpacing: "6px",
    fontFamily: "Orbitron, sans-serif",
    fontWeight: "800",
    textShadow: "0 0 20px rgba(255,255,255,0.3)",
  },
  scroll: {
    marginTop: "10px",
    textDecoration: "none",
    color: "white",
    fontSize: "1.2rem",
  },
};

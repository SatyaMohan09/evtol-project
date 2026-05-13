import { useEffect, useState } from "react";

export default function useSmoothValue(target, speed = 0.1) {
  const [value, setValue] = useState(target);

  useEffect(() => {
    let animationFrame;

    const animate = () => {
      setValue((prev) => prev + (target - prev) * speed);
      animationFrame = requestAnimationFrame(animate);
    };

    animate();

    return () => cancelAnimationFrame(animationFrame);
  }, [target, speed]);

  return value;
}

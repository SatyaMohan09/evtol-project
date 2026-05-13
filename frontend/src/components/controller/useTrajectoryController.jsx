import { useState, useEffect } from "react";

export default function useTrajectoryController(trajectory) {

  const [index, setIndex] = useState(0);

  const [isPlaying, setIsPlaying] = useState(false);

  const [speed, setSpeed] = useState(1);

  const position = trajectory[index];

  // PLAY

  const play = () => {

  setIsPlaying(true);

  };

  // PAUSE

  const pause = () => {

  setIsPlaying(false);

  };

  // RESET

  const reset = () => {

  setIsPlaying(false);

  setIndex(0);

  };

  // SPEED

  const changeSpeed = (value) => {

  setSpeed(value);

  };

  //seek function:

  const seek = (i) => {

  setIndex(i);

  };

  // playback loop

  useEffect(() => {

  if (!isPlaying) return;

  const interval = setInterval(() => {

  setIndex((prev) => {

  if (prev >= trajectory.length - 1) {

  return prev;

  }

  return prev + 1;

  });

  }, 1000 / speed);

  return () => clearInterval(interval);

  }, [isPlaying, speed, trajectory]);

  return {

  position,

  play,

  pause,

  reset,

  setSpeed: changeSpeed,

  isPlaying,

  index,

  seek,

  };

}
import { useState } from "react";

import { Button, Slider, Box, Typography, Stack, Paper } from "@mui/material";
 
export default function PlaybackControls({

  play,

  pause,

  reset,

  setSpeed,

  isPlaying,

}) {

  const [speedValue, setSpeedValue] = useState(1);
 
  const handleSpeedChange = (event, newValue) => {

    setSpeedValue(newValue);

    setSpeed(newValue);

  };
 
  return (
<Paper

      elevation={6}

      sx={{

        padding: 3,

        width: "100%",

        maxWidth: 280,

        background: "rgba(55, 76, 110, 0.85)",

        backdropFilter: "blur(10px)",

        borderRadius: "12px",

        border: "1px solid rgba(255,255,255,0.08)",

        fontFamily: "Orbitron, sans-serif",

      }}
>

      {/* TITLE */}
<Typography

        variant="subtitle1"

        sx={{

          fontFamily: "Orbitron, sans-serif",

          letterSpacing: "1px",

          fontWeight: 500,

          marginBottom: 2,

        }}
>

        Playback Controls
</Typography>
 
      {/* BUTTONS */}
<Stack direction="row" spacing={1.5} marginBottom={3}>
<Button

          variant="contained"

          onClick={play}

          disabled={isPlaying}

          sx={{

            background: "#4ade80",

            color: "#022c22",

            fontFamily: "Orbitron, sans-serif",

            fontSize: "12px",

            "&:hover": { background: "#22c55e" },

          }}
>

          PLAY
</Button>
 
        <Button

          variant="contained"

          onClick={pause}

          disabled={!isPlaying}

          sx={{

            background: "#64748b",

            fontFamily: "Orbitron, sans-serif",

            fontSize: "12px",

            "&:hover": { background: "#f07240" },

          }}
>

          PAUSE
</Button>
 
        <Button

          variant="contained"

          onClick={reset}

          sx={{

            background: "#ef4444",

            fontFamily: "Orbitron, sans-serif",

            fontSize: "12px",

            "&:hover": { background: "#dc2626" },

          }}
>

          RESET
</Button>
</Stack>
 
      {/* SPEED CONTROL */}
<Box>
<Typography

          sx={{

            fontFamily: "Orbitron, sans-serif",

            fontSize: "14px",

            marginBottom: 1,

            opacity: 0.9,

          }}
>

          Speed: {speedValue}x
</Typography>
 
        <Slider

          value={speedValue}

          min={0.5}

          max={5}

          step={0.5}

          onChange={handleSpeedChange}

          valueLabelDisplay="auto"

          sx={{

            color: "#38bdf8",

            "& .MuiSlider-thumb": {

              width: 14,

              height: 14,

            },

          }}

        />
</Box>
</Paper>

  );

}
 
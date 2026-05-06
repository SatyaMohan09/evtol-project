package com.evtol.trajectoryengine.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TrajectoryPoint {

    private double t;
    private double x;
    private double y;
    private double z;

    public TrajectoryPoint(double t, double x, double y, double z) {
        this.t = t;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
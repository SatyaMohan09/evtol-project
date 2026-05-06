package com.evtol.trajectoryengine.domain;

import lombok.Getter;

@Getter
public class CubicSegment {

    private final double t0;
    private final double t1;

    private final double a;
    private final double b;
    private final double c;
    private final double d;

    public CubicSegment(double t0, double t1,
                        double a, double b,
                        double c, double d) {

        this.t0 = t0;
        this.t1 = t1;

        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public double evaluate(double t) {

        double dt = t - t0;

        return ((a * dt + b) * dt + c) * dt + d;
    }

    public boolean contains(double t) {
        return t >= t0 && t <= t1;
    }
}
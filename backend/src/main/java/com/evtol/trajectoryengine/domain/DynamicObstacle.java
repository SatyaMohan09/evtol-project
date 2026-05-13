package com.evtol.trajectoryengine.domain;

public class DynamicObstacle {

    private double t;
    private double x;
    private double y;
    private double z;
    private double radius;

    public DynamicObstacle() {
    }

    public DynamicObstacle(double t, double x, double y, double z, double radius) {
        this.t = t;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
    }

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    @Override
    public String toString() {
        return "DynamicObstacle{" +
                "t=" + t +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", radius=" + radius +
                '}';
    }
}
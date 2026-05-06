package com.evtol.trajectoryengine.domain;

import lombok.Getter;

import java.util.List;

@Getter
public class TrajectoryModel {

    /*
     * Cubic spline representation
     */
    private final List<CubicSegment> xSegments;
    private final List<CubicSegment> ySegments;
    private final List<CubicSegment> zSegments;

    /*
     * B-Spline representation
     */
    private final List<Waypoint> controlPoints;
    private final double[] knots;
    private final int degree;

    /*
     * Shared information
     */
    private final double totalDuration;

    /*
     * Constructor for Cubic Spline
     */
    public TrajectoryModel(
            List<CubicSegment> xSegments,
            List<CubicSegment> ySegments,
            List<CubicSegment> zSegments,
            double totalDuration) {

        this.xSegments = xSegments;
        this.ySegments = ySegments;
        this.zSegments = zSegments;

        this.controlPoints = null;
        this.knots = null;
        this.degree = 0;

        this.totalDuration = totalDuration;
    }

    /*
     * Constructor for B-Spline
     */
    public TrajectoryModel(
            List<Waypoint> controlPoints,
            double[] knots,
            int degree,
            double totalDuration) {

        this.xSegments = null;
        this.ySegments = null;
        this.zSegments = null;

        this.controlPoints = controlPoints;
        this.knots = knots;
        this.degree = degree;

        this.totalDuration = totalDuration;
    }

    /*
     * Helper methods
     */

    public boolean isCubicSpline() {
        return xSegments != null;
    }

    public boolean isBSpline() {
        return controlPoints != null;
    }

    public TrajectoryModel adjustForObstacle(Obstacle obstacle) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'adjustForObstacle'");
    }
}
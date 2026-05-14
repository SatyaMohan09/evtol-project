package com.evtol.trajectoryengine.service;

import com.evtol.trajectoryengine.bspline.BSplineCurveBuilder;
import com.evtol.trajectoryengine.datasource.CsvWaypointDataProvider;
import com.evtol.trajectoryengine.datasource.DynamicObstacleDataProvider;
import com.evtol.trajectoryengine.domain.DynamicObstacle;
import com.evtol.trajectoryengine.domain.Obstacle;
import com.evtol.trajectoryengine.domain.TrajectoryModel;
import com.evtol.trajectoryengine.domain.TrajectoryPoint;
import com.evtol.trajectoryengine.domain.Waypoint;
import com.evtol.trajectoryengine.dto.TrajectoryResponse;
import com.evtol.trajectoryengine.fitting.LeastSquaresFitter;
import com.evtol.trajectoryengine.planning.DynamicRrtStarPlanner;
//import com.evtol.trajectoryengine.validation.WaypointValidator;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DynamicTrajectoryTestService {

    private final CsvWaypointDataProvider dataProvider;

    private final DynamicObstacleDataProvider dynamicObstacleDataProvider;

    //private final WaypointValidator validator;

    private final BSplineCurveBuilder bSplineCurveBuilder;

    private final SamplingService samplingService;

    private final LeastSquaresFitter leastSquaresFitter;

    private final DynamicRrtStarPlanner dynamicRrtStarPlanner;

    /*
     * JSON log service
     */
    private final JsonLogService jsonLogService;

    @Value("${trajectory.sampling.interval}")
    private double samplingInterval;

    @Value("${trajectory.algorithm}")
    private String algorithm;

    /*
     * IMPORTANT DEBUG FLAGS
     */
    private static final boolean BYPASS_LEAST_SQUARES = true;

    private static final boolean BYPASS_BSPLINE = true;

    /*
     * Main testing method
     */
    public TrajectoryResponse generateDynamicTrajectory(

            double lambda,

            List<Obstacle> staticObstacles
    ) {

        long totalStart =
                System.currentTimeMillis();

        /*
         * 1. Load original waypoints
         */
        long loadStart =
                System.currentTimeMillis();

        List<Waypoint> rawWaypoints =
                dataProvider.loadWaypoints();

        long loadEnd =
                System.currentTimeMillis();

        /*
         * 2. Load dynamic obstacles
         */
        long dynamicLoadStart =
                System.currentTimeMillis();

        List<DynamicObstacle> dynamicObstacles =
                dynamicObstacleDataProvider
                        .loadDynamicObstacles();

        long dynamicLoadEnd =
                System.currentTimeMillis();

        /*
         * INPUT DEBUG
         */
        System.out.println(
                "\n=================================================="
        );

        System.out.println(
                "DYNAMIC TRAJECTORY TEST SERVICE"
        );

        System.out.println(
                "=================================================="
        );

        System.out.println(
                "\n[INPUT DATA]"
        );

        System.out.println(
                "Raw waypoint count: "
                        + rawWaypoints.size()
        );

        System.out.println(
                "Static obstacle count: "
                        + staticObstacles.size()
        );

        System.out.println(
                "Dynamic obstacle count: "
                        + dynamicObstacles.size()
        );

        System.out.println(
                "Waypoint load time(ms): "
                        + (loadEnd - loadStart)
        );

        System.out.println(
                "Dynamic obstacle load time(ms): "
                        + (dynamicLoadEnd - dynamicLoadStart)
        );

        /*
         * Waypoint range
         */
        if (!rawWaypoints.isEmpty()) {

            Waypoint first =
                    rawWaypoints.get(0);

            Waypoint last =
                    rawWaypoints.get(
                            rawWaypoints.size() - 1
                    );

            System.out.println(
                    "\n[WAYPOINT RANGE]"
            );

            System.out.println(
                    "Start waypoint: "
                            + first
            );

            System.out.println(
                    "Goal waypoint: "
                            + last
            );

            System.out.println(
                    "Waypoint time range: "
                            + first.getT()
                            + " -> "
                            + last.getT()
            );
        }

        /*
         * Dynamic obstacle range
         */
        if (!dynamicObstacles.isEmpty()) {

            DynamicObstacle first =
                    dynamicObstacles.get(0);

            DynamicObstacle last =
                    dynamicObstacles.get(
                            dynamicObstacles.size() - 1
                    );

            System.out.println(
                    "\n[DYNAMIC OBSTACLE RANGE]"
            );

            System.out.println(
                    "First dynamic obstacle: "
                            + first
            );

            System.out.println(
                    "Last dynamic obstacle: "
                            + last
            );

            System.out.println(
                    "Dynamic obstacle time range: "
                            + first.getT()
                            + " -> "
                            + last.getT()
            );
        }

        /*
         * IMPORTANT:
         * Path-obstacle proximity analysis
         */
        System.out.println(
                "\n[PATH vs DYNAMIC OBSTACLE ANALYSIS]"
        );

        double minimumDistance =
                Double.POSITIVE_INFINITY;

        Waypoint closestWaypoint = null;

        DynamicObstacle closestObstacle = null;

        for (Waypoint waypoint
                : rawWaypoints) {

            for (DynamicObstacle obstacle
                    : dynamicObstacles) {

                double dx =
                        waypoint.getX()
                                - obstacle.getX();

                double dz =
                        waypoint.getZ()
                                - obstacle.getZ();

                double distance =
                        Math.sqrt(
                                dx * dx
                                        + dz * dz
                        );

                if (distance < minimumDistance) {

                    minimumDistance = distance;

                    closestWaypoint = waypoint;

                    closestObstacle = obstacle;
                }
            }
        }

        System.out.println(
                "Minimum path-obstacle distance: "
                        + minimumDistance
        );

        if (closestWaypoint != null
                && closestObstacle != null) {

            System.out.println(
                    "Closest waypoint: "
                            + closestWaypoint
            );

            System.out.println(
                    "Closest obstacle: "
                            + closestObstacle
            );

            System.out.println(
                    "Time difference: "
                            + Math.abs(
                            closestWaypoint.getT()
                                    - closestObstacle.getT()
                    )
            );
        }

        /*
         * 3. Dynamic RRT*
         */
        System.out.println(
                "\n=================================================="
        );

        System.out.println(
                "STARTING DYNAMIC RRT*"
        );

        System.out.println(
                "=================================================="
        );

        long plannerStart =
                System.currentTimeMillis();

        List<Waypoint> avoidedWaypoints =
                applyDynamicObstacleAvoidance(
                        rawWaypoints,
                        staticObstacles,
                        dynamicObstacles
                );

        long plannerEnd =
                System.currentTimeMillis();

        /*
         * Planner debug
         */
        System.out.println(
                "\n[RRT* RESULT]"
        );

        System.out.println(
                "Planner execution time(ms): "
                        + (plannerEnd - plannerStart)
        );

        System.out.println(
                "Original waypoint count: "
                        + rawWaypoints.size()
        );

        System.out.println(
                "Avoided waypoint count: "
                        + avoidedWaypoints.size()
        );

        boolean fallback =
                rawWaypoints == avoidedWaypoints;

        System.out.println(
                "Fallback used: "
                        + fallback
        );

        /*
         * IMPORTANT:
         * Detect if planner changed path
         */
        int changedPoints = 0;

        double maxDeviation = 0.0;

        int compareSize =
                Math.min(
                        rawWaypoints.size(),
                        avoidedWaypoints.size()
                );

        for (int i = 0;
             i < compareSize;
             i++) {

            Waypoint a =
                    rawWaypoints.get(i);

            Waypoint b =
                    avoidedWaypoints.get(i);

            double dx =
                    a.getX() - b.getX();

            double dz =
                    a.getZ() - b.getZ();

            double deviation =
                    Math.sqrt(
                            dx * dx
                                    + dz * dz
                    );

            if (deviation > 1.0) {

                changedPoints++;
            }

            maxDeviation =
                    Math.max(
                            maxDeviation,
                            deviation
                    );
        }

        System.out.println(
                "Changed waypoint count: "
                        + changedPoints
        );

        System.out.println(
                "Maximum deviation: "
                        + maxDeviation
        );

        /*
         * IMPORTANT:
         * Temporary bypass
         */
        List<Waypoint> controlPoints;

        if (BYPASS_LEAST_SQUARES) {

            System.out.println(
                    "\n[DEBUG MODE]"
            );

            System.out.println(
                    "Least squares fitting BYPASSED"
            );

            controlPoints =
                    avoidedWaypoints;

        } else {

            /*
             * Normal fitting
             */
            long fittingStart =
                    System.currentTimeMillis();

            controlPoints =
                    leastSquaresFitter.fit(
                            avoidedWaypoints
                    );

            long fittingEnd =
                    System.currentTimeMillis();

            System.out.println(
                    "\n[FITTING]"
            );

            System.out.println(
                    "Control points generated: "
                            + controlPoints.size()
            );

            System.out.println(
                    "Fitting time(ms): "
                            + (fittingEnd - fittingStart)
            );
        }

        /*
         * IMPORTANT:
         * Temporary bypass spline
         */
        List<TrajectoryPoint> points;

        TrajectoryModel trajectoryModel = null;

        if (BYPASS_BSPLINE) {

            System.out.println(
                    "\n[DEBUG MODE]"
            );

            System.out.println(
                    "B-Spline generation BYPASSED"
            );

            points =
                    convertWaypointsToTrajectoryPoints(
                            controlPoints
                    );

        } else {

            /*
             * Build spline
             */
            long splineStart =
                    System.currentTimeMillis();

            trajectoryModel =
                    bSplineCurveBuilder.build(
                            controlPoints,
                            lambda
                    );

            if ("bspline".equalsIgnoreCase(
                    algorithm
            )) {

                trajectoryModel =
                        bSplineCurveBuilder.build(
                                controlPoints,
                                lambda
                        );
            }

            long splineEnd =
                    System.currentTimeMillis();

            System.out.println(
                    "\n[BSPLINE]"
            );

            System.out.println(
                    "Spline generation time(ms): "
                            + (splineEnd - splineStart)
            );

            /*
             * Sample trajectory
             */
            long samplingStart =
                    System.currentTimeMillis();

            points =
                    samplingService.sample(
                            trajectoryModel,
                            samplingInterval
                    );

            long samplingEnd =
                    System.currentTimeMillis();

            System.out.println(
                    "Sampling time(ms): "
                            + (samplingEnd - samplingStart)
            );
        }

        /*
         * Trajectory debug
         */
        System.out.println(
                "\n[FINAL TRAJECTORY]"
        );

        System.out.println(
                "Trajectory point count: "
                        + points.size()
        );

        if (!points.isEmpty()) {

            System.out.println(
                    "First trajectory point: "
                            + points.get(0)
            );

            System.out.println(
                    "Last trajectory point: "
                            + points.get(
                                    points.size() - 1
                            )
            );
        }

        /*
         * Response
         */
        TrajectoryResponse response =
                new TrajectoryResponse(
                        points,
                        avoidedWaypoints,
                        controlPoints,
                        trajectoryModel != null
                                ? trajectoryModel.getTotalDuration()
                                : 0.0,
                        staticObstacles
                );

        /*
         * Save logs
         */
        long saveStart =
                System.currentTimeMillis();

        jsonLogService.saveResponse(
                response,
                "dynamic_test"
        );

        long saveEnd =
                System.currentTimeMillis();

        /*
         * Final summary
         */
        long totalEnd =
                System.currentTimeMillis();

        System.out.println(
                "\n=================================================="
        );

        System.out.println(
                "FINAL SUMMARY"
        );

        System.out.println(
                "=================================================="
        );

        System.out.println(
                "Response saved successfully."
        );

        System.out.println(
                "JSON save time(ms): "
                        + (saveEnd - saveStart)
        );

        System.out.println(
                "TOTAL EXECUTION TIME(ms): "
                        + (totalEnd - totalStart)
        );

        System.out.println(
                "==================================================\n"
        );

        return response;
    }

    /*
 * Dynamic obstacle avoidance
 */
private List<Waypoint> applyDynamicObstacleAvoidance(

        List<Waypoint> sourceWaypoints,

        List<Obstacle> staticObstacles,

        List<DynamicObstacle> dynamicObstacles
) {

    if (sourceWaypoints == null
            || sourceWaypoints.size() < 3) {

        System.out.println(
                "Skipping Dynamic RRT*: insufficient waypoints."
        );

        return sourceWaypoints;
    }

    System.out.println(
            "Launching Dynamic RRT* planner..."
    );

    long plannerStart =
            System.currentTimeMillis();

    List<Waypoint> result =
            dynamicRrtStarPlanner.plan(
                    sourceWaypoints,
                    staticObstacles,
                    dynamicObstacles
            );

    long plannerEnd =
            System.currentTimeMillis();

    System.out.println(
            "Dynamic RRT* planner finished."
    );

    System.out.println(
            "Planner runtime(ms): "
                    + (plannerEnd - plannerStart)
    );

    /*
     * ==================================================
     * PATH CHANGE ANALYSIS
     * ==================================================
     */
    int changedPoints = 0;

    double maxDeviation = 0.0;

    double totalDeviation = 0.0;

    for (int i = 0;
         i < Math.min(
                 sourceWaypoints.size(),
                 result.size()
         );
         i++) {

        Waypoint original =
                sourceWaypoints.get(i);

        Waypoint updated =
                result.get(i);

        double dx =
                updated.getX()
                        - original.getX();

        double dz =
                updated.getZ()
                        - original.getZ();

        double deviation =
                Math.sqrt(
                        dx * dx
                                + dz * dz
                );

        totalDeviation += deviation;

        maxDeviation =
                Math.max(
                        maxDeviation,
                        deviation
                );

        if (deviation > 1.0) {
            changedPoints++;
        }
    }

    double averageDeviation =
            result.isEmpty()
                    ? 0.0
                    : totalDeviation / result.size();

    System.out.println(
            "\n========== PATH CHANGE ANALYSIS =========="
    );

    System.out.println(
            "Changed points: "
                    + changedPoints
    );

    System.out.println(
            "Average deviation: "
                    + averageDeviation
    );

    System.out.println(
            "Maximum deviation: "
                    + maxDeviation
    );

    System.out.println(
            "==========================================\n"
    );

    /*
     * ==================================================
     * POST VALIDATION AGAINST DYNAMIC OBSTACLES
     * ==================================================
     */
    double minimumDistance =
            Double.POSITIVE_INFINITY;

    Waypoint closestWaypoint = null;

    DynamicObstacle closestObstacle = null;

    boolean collisionDetected = false;

    for (Waypoint waypoint : result) {

        for (DynamicObstacle obstacle
                : dynamicObstacles) {

            /*
             * Time gate
             */
            double timeDifference =
                    Math.abs(
                            waypoint.getT()
                                    - obstacle.getT()
                    );

            if (timeDifference > 0.75) {
                continue;
            }

            double dx =
                    waypoint.getX()
                            - obstacle.getX();

            double dz =
                    waypoint.getZ()
                            - obstacle.getZ();

            double distance =
                    Math.sqrt(
                            dx * dx
                                    + dz * dz
                    );

            if (distance < minimumDistance) {

                minimumDistance = distance;

                closestWaypoint = waypoint;

                closestObstacle = obstacle;
            }

            /*
             * Collision radius
             */
            double collisionRadius =
                    obstacle.getRadius()
                            + 50.0;

            if (distance <= collisionRadius) {

                collisionDetected = true;

                System.out.println(
                        "\n========== FINAL COLLISION DETECTED =========="
                );

                System.out.println(
                        "Waypoint: "
                                + waypoint
                );

                System.out.println(
                        "Obstacle: "
                                + obstacle
                );

                System.out.println(
                        "Distance: "
                                + distance
                );

                System.out.println(
                        "Collision radius: "
                                + collisionRadius
                );

                System.out.println(
                        "Time difference: "
                                + timeDifference
                );

                System.out.println(
                        "==============================================\n"
                );
            }
        }
    }

    /*
     * Final safety summary
     */
    System.out.println(
            "\n========== FINAL SAFETY SUMMARY =========="
    );

    System.out.println(
            "Minimum dynamic clearance: "
                    + minimumDistance
    );

    System.out.println(
            "Collision detected: "
                    + collisionDetected
    );

    if (closestWaypoint != null) {

        System.out.println(
                "Closest waypoint: "
                        + closestWaypoint
        );
    }

    if (closestObstacle != null) {

        System.out.println(
                "Closest obstacle: "
                        + closestObstacle
        );
    }

    System.out.println(
            "==========================================\n"
    );

    /*
     * IMPORTANT
     *
     * If planner still collides,
     * fallback to original path for now.
     *
     * Later this can trigger
     * local avoidance.
     */
//     if (collisionDetected) {

//         System.out.println(
//                 "Planner output STILL COLLIDES."
//         );

//         System.out.println(
//                 "Returning ORIGINAL path."
//         );

//         return sourceWaypoints;
//     }

    return result;
}

    /*
     * DEBUG ONLY
     *
     * Converts waypoints directly
     * into trajectory points
     */
    private List<TrajectoryPoint>
    convertWaypointsToTrajectoryPoints(
            List<Waypoint> waypoints
    ) {

        List<TrajectoryPoint> points =
                new java.util.ArrayList<>();

        for (Waypoint waypoint : waypoints) {

            points.add(
                    new TrajectoryPoint(
                            waypoint.getT(),
                            waypoint.getX(),
                            waypoint.getY(),
                            waypoint.getZ()
                    )
            );
        }

        return points;
    }
}
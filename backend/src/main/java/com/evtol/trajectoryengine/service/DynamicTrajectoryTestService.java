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
import com.evtol.trajectoryengine.validation.WaypointValidator;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 * TESTING SERVICE
 *
 * Separate from production TrajectoryService.
 *
 * Used only for:
 * - Dynamic obstacle testing
 * - Dynamic RRT* testing
 * - Simulation experiments
 */
@Service
@RequiredArgsConstructor
public class DynamicTrajectoryTestService {

    private final CsvWaypointDataProvider dataProvider;

    private final DynamicObstacleDataProvider dynamicObstacleDataProvider;

    private final WaypointValidator validator;

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
     * Main testing method
     */
    public TrajectoryResponse generateDynamicTrajectory(
            double lambda,
            List<Obstacle> staticObstacles
    ) {

        /*
         * 1. Load original waypoints
         */
        List<Waypoint> rawWaypoints =
                dataProvider.loadWaypoints();

        /*
         * 2. Load dynamic obstacle dataset
         */
        List<DynamicObstacle> dynamicObstacles =
                dynamicObstacleDataProvider.loadDynamicObstacles();

        System.out.println(
                "Loaded Dynamic Obstacles: "
                        + dynamicObstacles.size()
        );

        /*
         * 3. Apply Dynamic RRT*
         */
        List<Waypoint> avoidedWaypoints =
                applyDynamicObstacleAvoidance(
                        rawWaypoints,
                        staticObstacles,
                        dynamicObstacles
                );

        /*
         * 4. Validate
         */
        validator.validate(avoidedWaypoints);

        /*
         * 5. Least squares fitting
         */
        List<Waypoint> controlPoints =
                leastSquaresFitter.fit(
                        avoidedWaypoints
                );

        /*
         * 6. Build B-Spline trajectory
         */
        TrajectoryModel trajectoryModel =
                bSplineCurveBuilder.build(
                        controlPoints,
                        lambda
                );

        if ("bspline".equalsIgnoreCase(algorithm)) {

            trajectoryModel =
                    bSplineCurveBuilder.build(
                            controlPoints,
                            lambda
                    );
        }

        /*
         * 7. Sample trajectory
         */
        List<TrajectoryPoint> points =
                samplingService.sample(
                        trajectoryModel,
                        samplingInterval
                );

        /*
         * 8. Debug logs
         */
        System.out.println(
                "Generated Dynamic Trajectory Points: "
                        + points.size()
        );

        /*
         * 9. Build response
         */
        TrajectoryResponse response =
                new TrajectoryResponse(
                        points,
                        avoidedWaypoints,
                        controlPoints,
                        trajectoryModel.getTotalDuration(),
                        staticObstacles
                );

        /*
         * 10. Save JSON log
         */
        jsonLogService.saveResponse(
                response,
                "dynamic_test"
        );

        /*
         * 11. Return response
         */
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

            return sourceWaypoints;
        }

        return dynamicRrtStarPlanner.plan(
                sourceWaypoints,
                staticObstacles,
                dynamicObstacles
        );
    }
}
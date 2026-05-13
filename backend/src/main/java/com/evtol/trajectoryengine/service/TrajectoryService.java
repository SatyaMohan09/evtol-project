package com.evtol.trajectoryengine.service;

import com.evtol.trajectoryengine.datasource.CsvWaypointDataProvider;
import com.evtol.trajectoryengine.domain.TrajectoryModel;
import com.evtol.trajectoryengine.domain.TrajectoryPoint;
import com.evtol.trajectoryengine.domain.Waypoint;
import com.evtol.trajectoryengine.dto.TrajectoryResponse;
//import com.evtol.trajectoryengine.spline.CubicSplineBuilder;
import com.evtol.trajectoryengine.bspline.BSplineCurveBuilder;
import com.evtol.trajectoryengine.validation.WaypointValidator;
import com.evtol.trajectoryengine.fitting.LeastSquaresFitter;
import com.evtol.trajectoryengine.domain.Obstacle;
import com.evtol.trajectoryengine.planning.RrtStarPlanner;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrajectoryService {

    private final CsvWaypointDataProvider dataProvider;
    private final WaypointValidator validator;

    //private final CubicSplineBuilder cubicSplineBuilder;
    private final BSplineCurveBuilder bSplineCurveBuilder;

    private final SamplingService samplingService;
    private final LeastSquaresFitter leastSquaresFitter;
    private final RrtStarPlanner rrtStarPlanner;
    private List<Obstacle> obstacles;

    private final JsonLogService jsonLogService;

    @Value("${trajectory.sampling.interval}")
    private double samplingInterval;

    @Value("${trajectory.algorithm}")
    private String algorithm;


    public TrajectoryResponse generateTrajectory(double lambda) {

        // 1. Load waypoints
        List<Waypoint> waypoints = dataProvider.loadWaypoints();

        // 2. Validate
        //validator.validate(waypoints);

        // 3. Least squares → control points
        List<Waypoint> controlPoints = leastSquaresFitter.fit(waypoints);

        // 4. Build trajectory
        TrajectoryModel trajectoryModel;

        
            trajectoryModel = bSplineCurveBuilder.build(controlPoints, lambda);
       

        // 5. Sample
        List<TrajectoryPoint> points =
                samplingService.sample(trajectoryModel, samplingInterval);

        // 6. Response
        TrajectoryResponse response = new TrajectoryResponse(
                points,
                waypoints,
                controlPoints,
                trajectoryModel.getTotalDuration(),
                obstacles
        );

        jsonLogService.saveResponse(response);

        return response;
    }

    public TrajectoryResponse generateTrajectory(double lambda, List<Obstacle> obstacles) {

        // 1. Load waypoints
        List<Waypoint> rawWaypoints = dataProvider.loadWaypoints();
        List<Waypoint> waypoints = applyObstacleAvoidance(rawWaypoints, obstacles);
        this.obstacles = obstacles;

        // 2. Validate
        validator.validate(waypoints);

        // 3. Least squares → control points
        List<Waypoint> controlPoints = leastSquaresFitter.fit(waypoints);

        // 4. Build trajectory
        TrajectoryModel trajectoryModel;

       
            trajectoryModel = bSplineCurveBuilder.build(controlPoints, lambda);
       

        // 5. Sample
        List<TrajectoryPoint> points =
                samplingService.sample(trajectoryModel, samplingInterval);

        // 6. Response
        TrajectoryResponse response = new TrajectoryResponse(
                points,
                waypoints,
                controlPoints,
                trajectoryModel.getTotalDuration(),
                obstacles

        );

        jsonLogService.saveResponse(response);

        return response;
    }

    private List<Waypoint> applyObstacleAvoidance(List<Waypoint> sourceWaypoints, List<Obstacle> obstacles) {
        if (sourceWaypoints == null || sourceWaypoints.size() < 3 || obstacles == null || obstacles.isEmpty()) {
            return sourceWaypoints;
        }

        return rrtStarPlanner.plan(sourceWaypoints, obstacles);
    }
}
 
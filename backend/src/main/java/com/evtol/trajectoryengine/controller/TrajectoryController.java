package com.evtol.trajectoryengine.controller;

import com.evtol.trajectoryengine.datasource.CsvObstacleDataProvider;
//import com.evtol.trajectoryengine.datasource.DynamicObstacleDataProvider;
import com.evtol.trajectoryengine.domain.Obstacle;
import com.evtol.trajectoryengine.dto.TrajectoryRequest;
import com.evtol.trajectoryengine.dto.TrajectoryResponse;
import com.evtol.trajectoryengine.service.DynamicTrajectoryTestService;
import com.evtol.trajectoryengine.service.TrajectoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TrajectoryController {

    private final TrajectoryService trajectoryService;

    private final DynamicTrajectoryTestService dynamicTrajectoryTestService;

    private final CsvObstacleDataProvider obstacleDataProvider;

    //private final DynamicObstacleDataProvider dynamicObstacleDataProvider;

    /*
     * GET trajectory
     */
    @GetMapping("/trajectory")
    public TrajectoryResponse getTrajectory(
            @RequestParam(defaultValue = "0.1") double lambda
    ) {

        return trajectoryService.generateTrajectory(lambda);
    }

    /*
     * POST trajectory
     */
    @PostMapping("/trajectory")
    public TrajectoryResponse createTrajectory(
            @RequestBody(required = false)
            TrajectoryRequest request
    ) {

        double lambda =
                request != null
                        && request.getLambda() != null
                        ? request.getLambda()
                        : 0.1;

        List<Obstacle> obstacles =
                request != null
                        && request.getObstacles() != null
                        ? request.getObstacles()
                        : List.of();

        return trajectoryService.generateTrajectory(
                lambda,
                obstacles
        );
    }

    /*
     * Static obstacles endpoint
     *
     * Frontend should now call:
     * http://localhost:8080/api/obstacles
     */
    @GetMapping("/obstacles")
    public List<Obstacle> getObstacles() {

        return obstacleDataProvider.loadObstacles();
    }

    /*
     * Dynamic obstacle testing endpoint
     */
    @GetMapping("/dynamic-test")
        public TrajectoryResponse generateDynamicTestTrajectory(
                @RequestParam(defaultValue = "0.5")
                double lambda
        ) {

        /*
        * Load static obstacles from CSV
        */
        List<Obstacle> staticObstacles =
                obstacleDataProvider.loadObstacles();

        return dynamicTrajectoryTestService
                .generateDynamicTrajectory(
                        lambda,
                        staticObstacles
                );
        }
}
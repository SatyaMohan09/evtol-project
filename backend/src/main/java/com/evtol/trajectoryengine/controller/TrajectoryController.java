package com.evtol.trajectoryengine.controller;

import com.evtol.trajectoryengine.dto.TrajectoryResponse;
import com.evtol.trajectoryengine.dto.TrajectoryRequest;
import com.evtol.trajectoryengine.domain.Obstacle;
import com.evtol.trajectoryengine.service.TrajectoryService;
import com.evtol.trajectoryengine.service.DynamicTrajectoryTestService;
//import com.evtol.trajectoryengine.service.SamplingService;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TrajectoryController {

    private final TrajectoryService trajectoryService;

    private final DynamicTrajectoryTestService dynamicTrajectoryTestService;

    //private final SamplingService samplingService;

    @GetMapping("/trajectory")
    public TrajectoryResponse getTrajectory(
            @RequestParam(defaultValue = "0.1") double lambda) {

        return trajectoryService.generateTrajectory(lambda);
    }

    @PostMapping("/trajectory")
    public TrajectoryResponse createTrajectory(
            @RequestBody(required = false) TrajectoryRequest request) {

        double lambda = request != null && request.getLambda() != null
                ? request.getLambda()
                : 0.1;

        List<Obstacle> obstacles = request != null && request.getObstacles() != null
                ? request.getObstacles()
                : List.of();

        return trajectoryService.generateTrajectory(lambda, obstacles);
    }

    @GetMapping("/dynamic-test")
        public TrajectoryResponse generateDynamicTestTrajectory(
                @RequestParam(defaultValue = "0.5") double lambda
        ) {

        /*
        * Temporary static obstacles
        */
        List<Obstacle> staticObstacles = List.of(

                new Obstacle(
                        2500,
                        0,
                        1200,
                        180
                ),

                new Obstacle(
                        4200,
                        0,
                        2100,
                        220
                )
        );

        return dynamicTrajectoryTestService
                .generateDynamicTrajectory(
                        lambda,
                        staticObstacles
                );
        }
}

package com.evtol.trajectoryengine;

import com.evtol.trajectoryengine.dto.TrajectoryResponse;
import com.evtol.trajectoryengine.domain.TrajectoryPoint;
import com.evtol.trajectoryengine.service.TrajectoryService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TestRunner implements CommandLineRunner {

    private final TrajectoryService trajectoryService;

    public TestRunner(TrajectoryService trajectoryService) {
        this.trajectoryService = trajectoryService;
    }

    @Override
    public void run(String... args) {

        System.out.println("Starting trajectory generation...");

        // ✅ Provide lambda manually
        double lambda = 0.1;

        TrajectoryResponse response =
                trajectoryService.generateTrajectory(lambda);

        System.out.println("Lambda used: " + lambda);
        System.out.println("Total duration: " + response.getTotalDuration());

        for (TrajectoryPoint p : response.getTrajectory()) {

            System.out.println(
                    "t=" + p.getT() +
                            " x=" + p.getX() +
                            " y=" + p.getY() +
                            " z=" + p.getZ()
            );
        }

        System.out.println("Trajectory generation completed.");
    }
}
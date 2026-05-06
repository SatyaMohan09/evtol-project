package com.evtol.trajectoryengine.dto;
import com.evtol.trajectoryengine.domain.Obstacle;
import java.util.List;
public class TrajectoryRequest {
    private Double lambda;
    private List<Obstacle> obstacles;

    public Double getLambda() { return lambda; }
    public void setLambda(Double lambda) { this.lambda = lambda; }

    public List<Obstacle> getObstacles() { return obstacles; }
    public void setObstacles(List<Obstacle> obstacles) { this.obstacles = obstacles; }
}

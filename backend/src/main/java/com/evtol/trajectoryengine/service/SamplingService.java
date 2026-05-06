package com.evtol.trajectoryengine.service;
 
import com.evtol.trajectoryengine.bspline.DeBoorEvaluator;
import com.evtol.trajectoryengine.datasource.CsvWaypointDataProvider;
import com.evtol.trajectoryengine.domain.TrajectoryModel;
import com.evtol.trajectoryengine.domain.TrajectoryPoint;
import com.evtol.trajectoryengine.domain.Waypoint;
 
import org.springframework.stereotype.Service;
 
import java.util.ArrayList;
import java.util.List;
 
@Service
public class SamplingService {
 
    private final DeBoorEvaluator deBoorEvaluator = new DeBoorEvaluator();
    private final CsvWaypointDataProvider csvWaypointDataProvider;
 
    public SamplingService(CsvWaypointDataProvider csvWaypointDataProvider) {
        this.csvWaypointDataProvider = csvWaypointDataProvider;
    }
 
    public List<TrajectoryPoint> sample(TrajectoryModel trajectoryModel,
                                        double timeStep) {
 
        List<TrajectoryPoint> forwardSamples = new ArrayList<>();
 
        List<Waypoint> controlPoints = trajectoryModel.getControlPoints();
        List<Waypoint> waypoints = csvWaypointDataProvider.loadWaypoints();
 
        double[] knots = trajectoryModel.getKnots();
        int degree = trajectoryModel.getDegree();
 
        // ✅ REAL TIME (from dataset)
        double tStart = waypoints.get(0).getT();
        double tEnd = waypoints.get(waypoints.size() - 1).getT();
        double totalDuration = tEnd - tStart;
 
        // ✅ spline param range
        double uStart = knots[degree];
        double uEnd = knots[knots.length - degree - 1];
        double uRange = uEnd - uStart;
 
        // =========================
        // 🔵 FORWARD SAMPLING
        // =========================
        for (double t = tStart; t <= tEnd; t += timeStep) {
 
            double uNorm = (t - tStart) / totalDuration;
            double u = uStart + uNorm * uRange;
 
            Waypoint p = deBoorEvaluator.evaluate(
                    u,
                    degree,
                    knots,
                    controlPoints
            );
 
            forwardSamples.add(new TrajectoryPoint(
                    t,
                    p.getX(),
                    p.getY(),
                    p.getZ()
            ));
        }
 
        // =========================
        // 🔁 RETURN PATH (REVERSE)
        // =========================
        List<TrajectoryPoint> returnSamples = new ArrayList<>();
 
        double lastTime = forwardSamples.get(forwardSamples.size() - 1).getT();
 
        // reverse (skip last to avoid duplicate peak)
        for (int i = forwardSamples.size() - 2; i >= 0; i--) {
 
            TrajectoryPoint p = forwardSamples.get(i);
 
            double newTime = lastTime + (forwardSamples.size() - i) * timeStep;
 
            returnSamples.add(new TrajectoryPoint(
                    newTime,
                    p.getX(),
                    p.getY(),
                    p.getZ()
            ));
        }
 
        // =========================
        // 🔗 MERGE
        // =========================
        List<TrajectoryPoint> fullPath = new ArrayList<>();
        fullPath.addAll(forwardSamples);
        fullPath.addAll(returnSamples);
 
        return fullPath;
    }
}
 
 
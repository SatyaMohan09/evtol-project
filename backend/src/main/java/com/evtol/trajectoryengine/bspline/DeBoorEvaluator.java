package com.evtol.trajectoryengine.bspline;

import com.evtol.trajectoryengine.domain.Waypoint;

import java.util.ArrayList;
import java.util.List;

public class DeBoorEvaluator {

    public Waypoint evaluate(double t, int degree, double[] knots, List<Waypoint> controlPoints) {

        int k = findKnotSpan(t, knots, controlPoints.size(), degree);

        List<Waypoint> d = new ArrayList<>();

        for (int j = 0; j <= degree; j++) {
            d.add(controlPoints.get(k - degree + j));
        }

        for (int r = 1; r <= degree; r++) {

            for (int j = degree; j >= r; j--) {

                int index = k - degree + j;

                //double alpha = (t - knots[index]) / (knots[index + degree - r + 1] - knots[index]);





                double denom = knots[index + degree - r + 1] - knots[index];

                double alpha = 0.0;

                if (denom != 0) {
                    alpha = (t - knots[index]) / denom;
                }




                Waypoint p1 = d.get(j - 1);
                Waypoint p2 = d.get(j);

                double x = (1 - alpha) * p1.getX() + alpha * p2.getX();

                double y = (1 - alpha) * p1.getY() + alpha * p2.getY();

                double z = (1 - alpha) * p1.getZ() + alpha * p2.getZ();

                d.set(j, new Waypoint(t,x, y, z));
            }
        }

        return d.get(degree);
    }

    private int findKnotSpan(double t, double[] knots, int numControlPoints, int degree) {

        int n = numControlPoints - 1;

        if (t == knots[n + 1]) {
            return n;
        }

        for (int i = degree; i <= n; i++) {
            if (t >= knots[i] && t < knots[i + 1]) {
                return i;
            }
        }

        //return degree;
        return numControlPoints - 1;
    }
}
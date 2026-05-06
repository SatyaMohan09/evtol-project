package com.evtol.trajectoryengine.bspline;

import org.springframework.stereotype.Component;

@Component

public class KnotVectorGenerator {

    public double[] generateClampedUniform(int controlPoints, int degree) {

        int knotCount = controlPoints + degree + 1;

        double[] knots = new double[knotCount];

        int interiorKnots = controlPoints - degree;

        for (int i = 0; i <= degree; i++) {

            knots[i] = 0.0;

        }

        for (int i = 1; i < interiorKnots; i++) {

            knots[i + degree] = (double) i / interiorKnots;

        }

        for (int i = knotCount - degree - 1; i < knotCount; i++) {

            knots[i] = 1.0;

        }

        return knots;

    }

}

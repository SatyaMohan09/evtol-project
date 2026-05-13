package com.evtol.trajectoryengine.bspline;

import com.evtol.trajectoryengine.domain.TrajectoryModel;
import com.evtol.trajectoryengine.domain.Waypoint;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BSplineCurveBuilder {

    private static final int DEGREE = 3;

    private final KnotVectorGenerator knotVectorGenerator;

    public BSplineCurveBuilder(KnotVectorGenerator knotVectorGenerator) {
        this.knotVectorGenerator = knotVectorGenerator;
    }

    public TrajectoryModel build(List<Waypoint> controlPoints, double lambda) {

        if (controlPoints == null || controlPoints.size() < DEGREE + 1) {
            throw new IllegalArgumentException("Not enough control points");
        }

        int n = controlPoints.size();

        List<Waypoint> smoothedControlPoints = smooth(controlPoints, lambda);

        double[] knots = knotVectorGenerator.generateClampedUniform(n, DEGREE);

        double totalDuration =
                knots[knots.length - DEGREE - 1] - knots[DEGREE];

        return new TrajectoryModel(
                smoothedControlPoints,
                knots,
                DEGREE,
                totalDuration
        );
    }

    // ------------------ SMOOTHING ------------------

    private List<Waypoint> smooth(List<Waypoint> points, double lambda) {

        int n = points.size();

        double[][] D = new double[n - 2][n];
        for (int i = 0; i < n - 2; i++) {
            D[i][i] = 1;
            D[i][i + 1] = -2;
            D[i][i + 2] = 1;
        }

        double[][] Dt = transpose(D);
        double[][] A = add(identity(n), scale(multiply(Dt, D), lambda));

        double[] bx = extract(points, 'x');
        double[] by = extract(points, 'y');
        double[] bz = extract(points, 'z');

        double[] Px = solve(A, bx);
        double[] Py = solve(A, by);
        double[] Pz = solve(A, bz);

        List<Waypoint> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(new Waypoint(
                    points.get(i).getT(),
                    Px[i],
                    Py[i],
                    Pz[i]
            ));
        }

        return result;
    }

    // ------------------ MATRIX UTILS ------------------

    private double[][] identity(int n) {
        double[][] I = new double[n][n];
        for (int i = 0; i < n; i++) I[i][i] = 1;
        return I;
    }

    private double[][] transpose(double[][] A) {
        int r = A.length, c = A[0].length;
        double[][] T = new double[c][r];
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++)
                T[j][i] = A[i][j];
        return T;
    }

    private double[][] multiply(double[][] A, double[][] B) {
        int r = A.length, c = B[0].length, n = B.length;
        double[][] M = new double[r][c];
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++)
                for (int k = 0; k < n; k++)
                    M[i][j] += A[i][k] * B[k][j];
        return M;
    }

    private double[][] add(double[][] A, double[][] B) {
        double[][] R = new double[A.length][A[0].length];
        for (int i = 0; i < A.length; i++)
            for (int j = 0; j < A[0].length; j++)
                R[i][j] = A[i][j] + B[i][j];
        return R;
    }

    private double[][] scale(double[][] A, double s) {
        double[][] R = new double[A.length][A[0].length];
        for (int i = 0; i < A.length; i++)
            for (int j = 0; j < A[0].length; j++)
                R[i][j] = A[i][j] * s;
        return R;
    }

    private double[] extract(List<Waypoint> w, char axis) {
        double[] arr = new double[w.size()];
        for (int i = 0; i < w.size(); i++) {
            arr[i] = (axis == 'x') ? w.get(i).getX()
                    : (axis == 'y') ? w.get(i).getY()
                    : w.get(i).getZ();
        }
        return arr;
    }

    // SAFE Gaussian elimination
    private double[] solve(double[][] A, double[] B) {

        int n = B.length;

        double[][] M = new double[n][n];
        double[] b = new double[n];

        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, M[i], 0, n);
            b[i] = B[i];
        }

        for (int i = 0; i < n; i++) {

            int max = i;
            for (int k = i + 1; k < n; k++)
                if (Math.abs(M[k][i]) > Math.abs(M[max][i])) max = k;

            double[] temp = M[i]; M[i] = M[max]; M[max] = temp;
            double t = b[i]; b[i] = b[max]; b[max] = t;

            if (Math.abs(M[i][i]) < 1e-8) {
                throw new RuntimeException("Matrix is singular");
            }

            for (int k = i + 1; k < n; k++) {
                double f = M[k][i] / M[i][i];
                for (int j = i; j < n; j++)
                    M[k][j] -= f * M[i][j];
                b[k] -= f * b[i];
            }
        }

        double[] x = new double[n];

        for (int i = n - 1; i >= 0; i--) {
            double sum = b[i];
            for (int j = i + 1; j < n; j++)
                sum -= M[i][j] * x[j];
            x[i] = sum / M[i][i];
        }

        return x;
    }
}
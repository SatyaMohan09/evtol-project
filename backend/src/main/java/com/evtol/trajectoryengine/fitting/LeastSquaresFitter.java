package com.evtol.trajectoryengine.fitting;

import com.evtol.trajectoryengine.domain.Waypoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LeastSquaresFitter {

    @Value("${trajectory.bspline.degree:3}")
    private int degree;

    @Value("${trajectory.bspline.controlPoints:40}")
    private int controlPointCount;

    public List<Waypoint> fit(List<Waypoint> waypoints) {

        int n = waypoints.size();
        if (n < degree + 1) return waypoints;

        // ---------------------------
        // CHORD-LENGTH PARAMETERIZATION
        // ---------------------------
        double[] tNorm = computeChordLengthParams(waypoints);

        double tMin = waypoints.get(0).getT();
        double tMax = waypoints.get(n - 1).getT();
        double range = tMax - tMin;

        // ---------------------------
        // DATA-DRIVEN KNOT VECTOR
        // ---------------------------
        double[] knots = generateDataKnots(tNorm, controlPointCount, degree);

        // ---------------------------
        // BASIS MATRIX
        // ---------------------------
        double[][] N = new double[n][controlPointCount];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < controlPointCount; j++) {
                N[i][j] = basis(j, degree, tNorm[i], knots);
            }
        }

        // ---------------------------
        // EXTRACT DATA
        // ---------------------------
        double[] x = extract(waypoints, 'x');
        double[] y = extract(waypoints, 'y');
        double[] z = extract(waypoints, 'z');

        // ---------------------------
        // QR-BASED LEAST SQUARES
        // ---------------------------
        double[] Px = qrSolve(N, x);
        double[] Py = qrSolve(N, y);
        double[] Pz = qrSolve(N, z);

        // ---------------------------
        // BUILD CONTROL POINTS
        // ---------------------------
        List<Waypoint> controlPoints = new ArrayList<>();

        for (int i = 0; i < controlPointCount; i++) {
            double tActual = tMin + (double) i / (controlPointCount - 1) * range;
            controlPoints.add(new Waypoint(tActual, Px[i], Py[i], Pz[i]));
        }

        return controlPoints;
    }

    // ---------------------------
    // CHORD-LENGTH PARAMETERIZATION
    // ---------------------------
    private double[] computeChordLengthParams(List<Waypoint> waypoints) {

        int n = waypoints.size();
        double[] t = new double[n];

        t[0] = 0.0;
        double total = 0.0;

        for (int i = 1; i < n; i++) {
            Waypoint p1 = waypoints.get(i - 1);
            Waypoint p2 = waypoints.get(i);

            double dx = p2.getX() - p1.getX();
            double dy = p2.getY() - p1.getY();
            double dz = p2.getZ() - p1.getZ();

            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            total += dist;
            t[i] = total;
        }

        if (total == 0) return t;

        for (int i = 0; i < n; i++) {
            t[i] /= total;
        }

        return t;
    }

    // ---------------------------
    // DATA-DRIVEN KNOT VECTOR
    // ---------------------------
    private double[] generateDataKnots(double[] t, int nCtrl, int degree) {

        int m = nCtrl + degree + 1;
        double[] knots = new double[m];

        int n = t.length - 1;

        // Clamped start
        for (int i = 0; i <= degree; i++) {
            knots[i] = 0.0;
        }

        // Interior knots
        for (int j = 1; j < nCtrl - degree; j++) {

            double u = (double) j * n / (nCtrl - degree);
            int idx = (int) Math.floor(u);

            double sum = 0.0;

            for (int k = idx; k < idx + degree; k++) {
                sum += t[Math.min(k, n)];
            }

            knots[j + degree] = sum / degree;
        }

        // Clamped end
        for (int i = m - degree - 1; i < m; i++) {
            knots[i] = 1.0;
        }

        return knots;
    }

    // ---------------------------
    // COX–DE BOOR BASIS
    // ---------------------------
    private double basis(int i, int p, double t, double[] knots) {

        if (p == 0) {
            if (t >= knots[i] && t < knots[i + 1]) return 1.0;
            if (t == 1.0 && i == knots.length - 2) return 1.0;
            return 0.0;
        }

        double left = 0.0, right = 0.0;

        double denom1 = knots[i + p] - knots[i];
        if (denom1 != 0) {
            left = (t - knots[i]) / denom1 * basis(i, p - 1, t, knots);
        }

        double denom2 = knots[i + p + 1] - knots[i + 1];
        if (denom2 != 0) {
            right = (knots[i + p + 1] - t) / denom2 * basis(i + 1, p - 1, t, knots);
        }

        return left + right;
    }

    // ---------------------------
    // QR SOLVER (Gram-Schmidt)
    // ---------------------------
    private double[] qrSolve(double[][] A, double[] b) {

        int m = A.length;
        int n = A[0].length;

        double[][] Q = new double[m][n];
        double[][] R = new double[n][n];

        for (int j = 0; j < n; j++) {

            double[] v = new double[m];
            for (int i = 0; i < m; i++) {
                v[i] = A[i][j];
            }

            for (int k = 0; k < j; k++) {

                double dot = 0;
                for (int i = 0; i < m; i++) {
                    dot += Q[i][k] * A[i][j];
                }

                R[k][j] = dot;

                for (int i = 0; i < m; i++) {
                    v[i] -= dot * Q[i][k];
                }
            }

            double norm = 0;
            for (double val : v) norm += val * val;
            norm = Math.sqrt(norm);

            if (norm == 0) continue;

            R[j][j] = norm;

            for (int i = 0; i < m; i++) {
                Q[i][j] = v[i] / norm;
            }
        }

        // Qᵀb
        double[] Qt_b = new double[n];
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < m; i++) {
                Qt_b[j] += Q[i][j] * b[i];
            }
        }

        // Back substitution
        double[] x = new double[n];

        for (int i = n - 1; i >= 0; i--) {
            double sum = Qt_b[i];

            for (int j = i + 1; j < n; j++) {
                sum -= R[i][j] * x[j];
            }

            x[i] = sum / R[i][i];
        }

        return x;
    }

    // ---------------------------
    // UTIL
    // ---------------------------
    private double[] extract(List<Waypoint> w, char axis) {
        double[] arr = new double[w.size()];
        for (int i = 0; i < w.size(); i++) {
            arr[i] = (axis == 'x') ? w.get(i).getX()
                    : (axis == 'y') ? w.get(i).getY()
                    : w.get(i).getZ();
        }
        return arr;
    }
}
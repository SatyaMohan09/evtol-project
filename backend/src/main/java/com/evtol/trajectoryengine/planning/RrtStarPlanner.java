package com.evtol.trajectoryengine.planning;

import com.evtol.trajectoryengine.domain.Obstacle;
import com.evtol.trajectoryengine.domain.Waypoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
public class RrtStarPlanner {

    /*
     * Clearance
     */
    private static final double CLEARANCE_BUFFER = 90.0;

    /*
     * Planner tuning
     */
    private static final double STEP_SIZE = 60.0;

    private static final double GOAL_THRESHOLD = 220.0;

    private static final double NEIGHBOR_RADIUS = 140.0;

    private static final int MAX_ITERATIONS = 12000;

    private static final int SHORTCUT_PASSES = 150;

    private static final double GOAL_SAMPLE_RATE = 0.22;

    /*
     * Corridor bias
     */
    private static final double LATERAL_BIAS_DISTANCE = 600.0;

    public List<Waypoint> plan(
            List<Waypoint> sourceWaypoints,
            List<Obstacle> obstacles
    ) {

        if (sourceWaypoints == null
                || sourceWaypoints.size() < 2) {

            return sourceWaypoints;
        }

        Waypoint start =
                sourceWaypoints.get(0);

        Waypoint goal =
                sourceWaypoints.get(
                        sourceWaypoints.size() - 1
                );

        Bounds bounds =
                buildBounds(
                        sourceWaypoints,
                        obstacles
                );

        Random random =
                new Random(42L);

        List<Node> nodes =
                new ArrayList<>();

        Node startNode =
                new Node(
                        start.getX(),
                        start.getZ(),
                        null,
                        0.0
                );

        nodes.add(startNode);

        Node bestGoalNode = null;

        for (int i = 0;
             i < MAX_ITERATIONS;
             i++) {

            Sample sample =
                    sample(
                            random,
                            bounds,
                            start,
                            goal
                    );

            Node nearest =
                    nearest(
                            nodes,
                            sample.x,
                            sample.z
                    );

            Node candidate =
                    steer(
                            nearest,
                            sample.x,
                            sample.z
                    );

            if (!isSegmentCollisionFree(
                    nearest.x,
                    nearest.z,
                    candidate.x,
                    candidate.z,
                    obstacles
            )) {

                continue;
            }

            List<Node> neighbors =
                    near(
                            nodes,
                            candidate.x,
                            candidate.z,
                            NEIGHBOR_RADIUS
                    );

            Node parent = nearest;

            double bestCost =
                    nearest.cost
                            +
                            distance(
                                    nearest.x,
                                    nearest.z,
                                    candidate.x,
                                    candidate.z
                            );

            /*
             * Best parent selection
             */
            for (Node neighbor : neighbors) {

                if (!isSegmentCollisionFree(
                        neighbor.x,
                        neighbor.z,
                        candidate.x,
                        candidate.z,
                        obstacles
                )) {

                    continue;
                }

                double cost =
                        neighbor.cost
                                +
                                distance(
                                        neighbor.x,
                                        neighbor.z,
                                        candidate.x,
                                        candidate.z
                                );

                if (cost < bestCost) {

                    parent = neighbor;

                    bestCost = cost;
                }
            }

            candidate.parent = parent;

            candidate.cost = bestCost;

            nodes.add(candidate);

            /*
             * Rewiring
             */
            for (Node neighbor : neighbors) {

                if (neighbor == parent) {
                    continue;
                }

                if (!isSegmentCollisionFree(
                        candidate.x,
                        candidate.z,
                        neighbor.x,
                        neighbor.z,
                        obstacles
                )) {

                    continue;
                }

                double rewiredCost =
                        candidate.cost
                                +
                                distance(
                                        candidate.x,
                                        candidate.z,
                                        neighbor.x,
                                        neighbor.z
                                );

                if (rewiredCost < neighbor.cost) {

                    neighbor.parent = candidate;

                    neighbor.cost = rewiredCost;
                }
            }

            /*
             * Goal connection
             */
            if (distance(
                    candidate.x,
                    candidate.z,
                    goal.getX(),
                    goal.getZ()
            ) <= GOAL_THRESHOLD
                    &&
                    isSegmentCollisionFree(
                            candidate.x,
                            candidate.z,
                            goal.getX(),
                            goal.getZ(),
                            obstacles
                    )) {

                Node goalNode =
                        new Node(
                                goal.getX(),
                                goal.getZ(),
                                candidate,
                                candidate.cost
                                        +
                                        distance(
                                                candidate.x,
                                                candidate.z,
                                                goal.getX(),
                                                goal.getZ()
                                        )
                        );

                if (bestGoalNode == null
                        ||
                        goalNode.cost
                                < bestGoalNode.cost) {

                    bestGoalNode = goalNode;
                }
            }
        }

        /*
         * Fallback
         */
        if (bestGoalNode == null) {

            System.out.println(
                    "RRT* failed. Returning source path."
            );

            return sourceWaypoints;
        }

        /*
         * Extract path
         */
        List<Point2> path =
                extractPath(bestGoalNode);

        /*
         * Smooth path
         */
        path =
                shortcutPath(
                        path,
                        obstacles
                );

        /*
         * Resample
         */
        return resamplePath(
                path,
                sourceWaypoints
        );
    }

    /*
     * Bounds
     */
    private Bounds buildBounds(
            List<Waypoint> sourceWaypoints,
            List<Obstacle> obstacles
    ) {

        double minX =
                Double.POSITIVE_INFINITY;

        double maxX =
                Double.NEGATIVE_INFINITY;

        double minZ =
                Double.POSITIVE_INFINITY;

        double maxZ =
                Double.NEGATIVE_INFINITY;

        for (Waypoint waypoint
                : sourceWaypoints) {

            minX =
                    Math.min(
                            minX,
                            waypoint.getX()
                    );

            maxX =
                    Math.max(
                            maxX,
                            waypoint.getX()
                    );

            minZ =
                    Math.min(
                            minZ,
                            waypoint.getZ()
                    );

            maxZ =
                    Math.max(
                            maxZ,
                            waypoint.getZ()
                    );
        }

        for (Obstacle obstacle
                : obstacles) {

            double radius =
                    obstacle.getRadius()
                            + CLEARANCE_BUFFER;

            minX =
                    Math.min(
                            minX,
                            obstacle.getX() - radius
                    );

            maxX =
                    Math.max(
                            maxX,
                            obstacle.getX() + radius
                    );

            minZ =
                    Math.min(
                            minZ,
                            obstacle.getZ() - radius
                    );

            maxZ =
                    Math.max(
                            maxZ,
                            obstacle.getZ() + radius
                    );
        }

        double margin = 500.0;

        return new Bounds(
                minX - margin,
                maxX + margin,
                minZ - margin,
                maxZ + margin
        );
    }

    /*
     * Smart sampling
     */
    private Sample sample(
            Random random,
            Bounds bounds,
            Waypoint start,
            Waypoint goal
    ) {

        /*
         * Goal bias
         */
        if (random.nextDouble()
                < GOAL_SAMPLE_RATE) {

            return new Sample(
                    goal.getX(),
                    goal.getZ()
            );
        }

        /*
         * Corridor-guided sampling
         */
        if (random.nextDouble() < 0.70) {

            double alpha =
                    random.nextDouble();

            double centerX =
                    lerp(
                            start.getX(),
                            goal.getX(),
                            alpha
                    );

            double centerZ =
                    lerp(
                            start.getZ(),
                            goal.getZ(),
                            alpha
                    );

            /*
             * Perpendicular direction
             */
            double dx =
                    goal.getX()
                            - start.getX();

            double dz =
                    goal.getZ()
                            - start.getZ();

            double length =
                    Math.hypot(dx, dz);

            if (length > 1e-6) {

                dx /= length;
                dz /= length;

                double perpX = -dz;
                double perpZ = dx;

                double lateral =
                        (
                                random.nextDouble() * 2.0
                                        - 1.0
                        )
                                * LATERAL_BIAS_DISTANCE;

                centerX += perpX * lateral;
                centerZ += perpZ * lateral;
            }

            return new Sample(
                    centerX,
                    centerZ
            );
        }

        /*
         * Random exploration
         */
        double x =
                bounds.minX
                        +
                        random.nextDouble()
                                *
                                (
                                        bounds.maxX
                                                - bounds.minX
                                );

        double z =
                bounds.minZ
                        +
                        random.nextDouble()
                                *
                                (
                                        bounds.maxZ
                                                - bounds.minZ
                                );

        return new Sample(x, z);
    }

    /*
     * Nearest node
     */
    private Node nearest(
            List<Node> nodes,
            double x,
            double z
    ) {

        Node best =
                nodes.get(0);

        double bestDistance =
                distance(
                        best.x,
                        best.z,
                        x,
                        z
                );

        for (Node node : nodes) {

            double d =
                    distance(
                            node.x,
                            node.z,
                            x,
                            z
                    );

            if (d < bestDistance) {

                best = node;

                bestDistance = d;
            }
        }

        return best;
    }

    /*
     * Steer
     */
    private Node steer(
            Node from,
            double targetX,
            double targetZ
    ) {

        double dx =
                targetX - from.x;

        double dz =
                targetZ - from.z;

        double length =
                Math.hypot(dx, dz);

        if (length <= STEP_SIZE) {

            return new Node(
                    targetX,
                    targetZ,
                    from,
                    from.cost + length
            );
        }

        double scale =
                STEP_SIZE / length;

        return new Node(
                from.x + dx * scale,
                from.z + dz * scale,
                from,
                from.cost + STEP_SIZE
        );
    }

    /*
     * Nearby nodes
     */
    private List<Node> near(
            List<Node> nodes,
            double x,
            double z,
            double radius
    ) {

        List<Node> neighbors =
                new ArrayList<>();

        for (Node node : nodes) {

            if (distance(
                    node.x,
                    node.z,
                    x,
                    z
            ) <= radius) {

                neighbors.add(node);
            }
        }

        return neighbors;
    }

    /*
     * Collision checking
     */
    private boolean isSegmentCollisionFree(
            double x1,
            double z1,
            double x2,
            double z2,
            List<Obstacle> obstacles
    ) {

        for (Obstacle obstacle
                : obstacles) {

            double radius =
                    obstacle.getRadius()
                            + CLEARANCE_BUFFER;

            double d =
                    segmentDistanceToPoint(
                            x1,
                            z1,
                            x2,
                            z2,
                            obstacle.getX(),
                            obstacle.getZ()
                    );

            if (d < radius) {

                return false;
            }
        }

        return true;
    }

    /*
     * Segment-point distance
     */
    private double segmentDistanceToPoint(
            double x1,
            double z1,
            double x2,
            double z2,
            double px,
            double pz
    ) {

        double dx = x2 - x1;

        double dz = z2 - z1;

        double lengthSq =
                dx * dx + dz * dz;

        if (lengthSq < 1e-9) {

            return distance(
                    x1,
                    z1,
                    px,
                    pz
            );
        }

        double t =
                (
                        ((px - x1) * dx)
                                +
                                ((pz - z1) * dz)
                ) / lengthSq;

        t =
                Math.max(
                        0.0,
                        Math.min(1.0, t)
                );

        double projX =
                x1 + t * dx;

        double projZ =
                z1 + t * dz;

        return distance(
                projX,
                projZ,
                px,
                pz
        );
    }

    /*
     * Extract final path
     */
    private List<Point2> extractPath(
            Node goalNode
    ) {

        List<Point2> path =
                new ArrayList<>();

        Node current =
                goalNode;

        while (current != null) {

            path.add(
                    new Point2(
                            current.x,
                            current.z
                    )
            );

            current =
                    current.parent;
        }

        Collections.reverse(path);

        return path;
    }

    /*
     * Path shortcutting
     */
    private List<Point2> shortcutPath(
            List<Point2> path,
            List<Obstacle> obstacles
    ) {

        if (path.size() < 3) {
            return path;
        }

        List<Point2> simplified =
                new ArrayList<>(path);

        for (int pass = 0;
             pass < SHORTCUT_PASSES;
             pass++) {

            boolean changed = false;

            for (int i = 0;
                 i < simplified.size() - 2;
                 i++) {

                for (int j =
                     simplified.size() - 1;
                     j > i + 1;
                     j--) {

                    Point2 a =
                            simplified.get(i);

                    Point2 b =
                            simplified.get(j);

                    if (!isSegmentCollisionFree(
                            a.x,
                            a.z,
                            b.x,
                            b.z,
                            obstacles
                    )) {

                        continue;
                    }

                    List<Point2> next =
                            new ArrayList<>();

                    next.addAll(
                            simplified.subList(
                                    0,
                                    i + 1
                            )
                    );

                    next.addAll(
                            simplified.subList(
                                    j,
                                    simplified.size()
                            )
                    );

                    simplified = next;

                    changed = true;

                    break;
                }

                if (changed) {
                    break;
                }
            }

            if (!changed) {
                break;
            }
        }

        return simplified;
    }

    /*
     * Resample path
     */
    private List<Waypoint> resamplePath(
            List<Point2> path,
            List<Waypoint> sourceWaypoints
    ) {

        if (path.size() < 2) {
            return sourceWaypoints;
        }

        double[] cumulative =
                new double[path.size()];

        for (int i = 1;
             i < path.size();
             i++) {

            Point2 prev =
                    path.get(i - 1);

            Point2 current =
                    path.get(i);

            cumulative[i] =
                    cumulative[i - 1]
                            +
                            distance(
                                    prev.x,
                                    prev.z,
                                    current.x,
                                    current.z
                            );
        }

        double totalLength =
                cumulative[
                        cumulative.length - 1
                        ];

        if (totalLength < 1e-6) {
            return sourceWaypoints;
        }

        List<Waypoint> resampled =
                new ArrayList<>(
                        sourceWaypoints.size()
                );

        int pathIndex = 1;

        for (int i = 0;
             i < sourceWaypoints.size();
             i++) {

            Waypoint source =
                    sourceWaypoints.get(i);

            double alpha =
                    sourceWaypoints.size() == 1
                            ? 0.0
                            :
                            (double) i
                                    /
                                    (
                                            sourceWaypoints.size()
                                                    - 1
                                    );

            double targetDistance =
                    alpha * totalLength;

            while (
                    pathIndex
                            < cumulative.length - 1
                            &&
                            cumulative[pathIndex]
                                    < targetDistance
            ) {

                pathIndex++;
            }

            Point2 p0 =
                    path.get(
                            Math.max(
                                    pathIndex - 1,
                                    0
                            )
                    );

            Point2 p1 =
                    path.get(pathIndex);

            double segmentLength =
                    cumulative[pathIndex]
                            -
                            cumulative[
                                    Math.max(
                                            pathIndex - 1,
                                            0
                                    )
                                    ];

            double localAlpha =
                    segmentLength > 1e-6
                            ?
                            (
                                    targetDistance
                                            -
                                            cumulative[
                                                    Math.max(
                                                            pathIndex - 1,
                                                            0
                                                    )
                                                    ]
                            )
                                    /
                                    segmentLength
                            :
                            0.0;

            double x =
                    lerp(
                            p0.x,
                            p1.x,
                            localAlpha
                    );

            double z =
                    lerp(
                            p0.z,
                            p1.z,
                            localAlpha
                    );

            resampled.add(
                    new Waypoint(
                            source.getT(),
                            x,
                            source.getY(),
                            z
                    )
            );
        }

        /*
         * Preserve endpoints
         */
        resampled.set(
                0,
                sourceWaypoints.get(0)
        );

        resampled.set(
                resampled.size() - 1,
                sourceWaypoints.get(
                        sourceWaypoints.size() - 1
                )
        );

        return resampled;
    }

    /*
     * Distance
     */
    private double distance(
            double x1,
            double z1,
            double x2,
            double z2
    ) {

        return Math.hypot(
                x2 - x1,
                z2 - z1
        );
    }

    /*
     * Linear interpolation
     */
    private double lerp(
            double a,
            double b,
            double alpha
    ) {

        return a + (b - a) * alpha;
    }

    /*
     * Bounds
     */
    private static class Bounds {

        final double minX;

        final double maxX;

        final double minZ;

        final double maxZ;

        Bounds(
                double minX,
                double maxX,
                double minZ,
                double maxZ
        ) {

            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }
    }

    /*
     * Sampling point
     */
    private static class Sample {

        final double x;

        final double z;

        Sample(
                double x,
                double z
        ) {

            this.x = x;
            this.z = z;
        }
    }

    /*
     * Path point
     */
    private static class Point2 {

        final double x;

        final double z;

        Point2(
                double x,
                double z
        ) {

            this.x = x;
            this.z = z;
        }
    }

    /*
     * Tree node
     */
    private static class Node {

        final double x;

        final double z;

        Node parent;

        double cost;

        Node(
                double x,
                double z,
                Node parent,
                double cost
        ) {

            this.x = x;
            this.z = z;
            this.parent = parent;
            this.cost = cost;
        }
    }
}
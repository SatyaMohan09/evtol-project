package com.evtol.trajectoryengine.planning;

import com.evtol.trajectoryengine.domain.DynamicObstacle;
import com.evtol.trajectoryengine.domain.Obstacle;
import com.evtol.trajectoryengine.domain.Waypoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DynamicRrtStarPlanner {

    /*
     * Safety clearance
     */
    private static final double CLEARANCE_BUFFER = 20.0;

    /*
     * Lateral avoidance offset
     */
    private static final double LATERAL_OFFSET = 350.0;

    /*
     * Smooth blending window
     */
    private static final int AVOIDANCE_WINDOW = 40;

    /*
     * Time synchronization tolerance
     */
    private static final double TEMPORAL_WINDOW = 2.0;

    public List<Waypoint> plan(

            List<Waypoint> sourceWaypoints,

            List<Obstacle> staticObstacles,

            List<DynamicObstacle> dynamicObstacles
    ) {

        if (sourceWaypoints == null
                || sourceWaypoints.size() < 3) {

            return sourceWaypoints;
        }

        /*
         * Normalize obstacle time
         */
        if (!dynamicObstacles.isEmpty()) {

            double t0 =
                    dynamicObstacles.get(0).getT();

            for (DynamicObstacle obstacle
                    : dynamicObstacles) {

                obstacle.setT(
                        obstacle.getT() - t0
                );
            }
        }

        System.out.println(
                "\n========== LOCAL DYNAMIC AVOIDANCE =========="
        );

        List<Waypoint> modified =
                new ArrayList<>();

        /*
         * Copy original path
         */
        for (Waypoint waypoint
                : sourceWaypoints) {

            modified.add(
                    new Waypoint(
                            waypoint.getT(),
                            waypoint.getX(),
                            waypoint.getY(),
                            waypoint.getZ()
                    )
            );
        }

        int collisionsDetected = 0;

        /*
         * Scan trajectory
         */
        for (int i = 1;
             i < modified.size() - 1;
             i++) {

            Waypoint current =
                    modified.get(i);

            DynamicObstacle collisionObstacle =
                    findCollision(
                            current,
                            dynamicObstacles
                    );

            if (collisionObstacle == null) {
                continue;
            }

            collisionsDetected++;

            System.out.println(
                    "\nCOLLISION DETECTED"
            );

            System.out.println(
                    "Waypoint index: " + i
            );

            System.out.println(
                    "Waypoint: " + current
            );

            System.out.println(
                    "Obstacle: " + collisionObstacle
            );

            /*
             * Path tangent
             */
            Waypoint prev =
                    modified.get(i - 1);

            Waypoint next =
                    modified.get(i + 1);

            double tx =
                    next.getX() - prev.getX();

            double tz =
                    next.getZ() - prev.getZ();

            double tangentLength =
                    Math.sqrt(
                            tx * tx + tz * tz
                    );

            if (tangentLength < 1e-6) {
                continue;
            }

            tx /= tangentLength;
            tz /= tangentLength;

            /*
             * Left normal
             */
            double leftNx = -tz;
            double leftNz = tx;

            /*
             * Right normal
             */
            double rightNx = tz;
            double rightNz = -tx;

            /*
             * Test left side
             */
            boolean leftFree =
                    isSideFree(
                            current,
                            leftNx,
                            leftNz,
                            dynamicObstacles
                    );

            /*
             * Test right side
             */
            boolean rightFree =
                    isSideFree(
                            current,
                            rightNx,
                            rightNz,
                            dynamicObstacles
                    );

            /*
             * Choose direction
             */
            double nx;
            double nz;

            if (leftFree) {

                nx = leftNx;
                nz = leftNz;

                System.out.println(
                        "Avoidance direction: LEFT"
                );

            } else if (rightFree) {

                nx = rightNx;
                nz = rightNz;

                System.out.println(
                        "Avoidance direction: RIGHT"
                );

            } else {

                System.out.println(
                        "No free lateral side found."
                );

                continue;
            }

            /*
             * Apply smooth lateral deviation
             */
            int start =
                    Math.max(
                            0,
                            i - AVOIDANCE_WINDOW
                    );

            int end =
                    Math.min(
                            modified.size() - 1,
                            i + AVOIDANCE_WINDOW
                    );

            for (int j = start;
                 j <= end;
                 j++) {

                Waypoint w =
                        modified.get(j);

                /*
                 * Smooth blend
                 */
                double distanceFromCenter =
                        Math.abs(j - i);

                double alpha =
                        1.0
                                - (
                                distanceFromCenter
                                        / AVOIDANCE_WINDOW
                        );

                alpha =
                        Math.max(0.0, alpha);

                /*
                 * Bell-shaped offset
                 */
                double offset =
                        alpha * LATERAL_OFFSET;

                double newX =
                        w.getX()
                                + nx * offset;

                double newZ =
                        w.getZ()
                                + nz * offset;

                modified.set(
                        j,
                        new Waypoint(
                                w.getT(),
                                newX,
                                w.getY(),
                                newZ
                        )
                );
            }

            /*
             * Skip already modified region
             */
            i += AVOIDANCE_WINDOW;
        }

        System.out.println(
                "\n========== AVOIDANCE SUMMARY =========="
        );

        System.out.println(
                "Collisions detected: "
                        + collisionsDetected
        );

        System.out.println(
                "Original waypoint count: "
                        + sourceWaypoints.size()
        );

        System.out.println(
                "Modified waypoint count: "
                        + modified.size()
        );

        System.out.println(
                "=======================================\n"
        );

        return modified;
    }

    /*
     * Find collision at waypoint
     */
    private DynamicObstacle findCollision(

            Waypoint waypoint,

            List<DynamicObstacle> obstacles
    ) {

        for (DynamicObstacle obstacle
                : obstacles) {

            /*
             * Temporal gate
             */
            double timeDifference =
                    Math.abs(
                            waypoint.getT()
                                    - obstacle.getT()
                    );

            if (timeDifference
                    > TEMPORAL_WINDOW) {

                continue;
            }

            /*
             * Spatial distance
             */
            double dx =
                    waypoint.getX()
                            - obstacle.getX();

            double dz =
                    waypoint.getZ()
                            - obstacle.getZ();

            double distance =
                    Math.sqrt(
                            dx * dx
                                    + dz * dz
                    );

            /*
        * Dynamic obstacles should NOT
        * behave like giant static cylinders.
        *
        * Use smaller effective radius.
        */
        double collisionRadius =
                Math.min(
                        80.0,
                        obstacle.getRadius() * 0.35
                );

            if (distance <= collisionRadius) {

                return obstacle;
            }
        }

        return null;
    }

    /*
     * Check lateral side
     */
    private boolean isSideFree(

            Waypoint waypoint,

            double nx,
            double nz,

            List<DynamicObstacle> obstacles
    ) {

        double testX =
                waypoint.getX()
                        + nx * LATERAL_OFFSET;

        double testZ =
                waypoint.getZ()
                        + nz * LATERAL_OFFSET;

        for (DynamicObstacle obstacle
                : obstacles) {

            double timeDifference =
                    Math.abs(
                            waypoint.getT()
                                    - obstacle.getT()
                    );

            if (timeDifference
                    > TEMPORAL_WINDOW) {

                continue;
            }

            double dx =
                    testX - obstacle.getX();

            double dz =
                    testZ - obstacle.getZ();

            double distance =
                    Math.sqrt(
                            dx * dx
                                    + dz * dz
                    );

            if (distance <=
                    obstacle.getRadius()
                            + CLEARANCE_BUFFER) {

                return false;
            }
        }

        return true;
    }
}
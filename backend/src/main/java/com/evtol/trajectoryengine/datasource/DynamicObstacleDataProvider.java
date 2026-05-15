package com.evtol.trajectoryengine.datasource;

import com.evtol.trajectoryengine.domain.DynamicObstacle;
//import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class DynamicObstacleDataProvider {

    // @Value("${dynamic.obstacle.file.path}")
    // private String dynamicObstacleFilePath;

    public List<DynamicObstacle> loadDynamicObstacles() {

        List<DynamicObstacle> dynamicObstacles =
                new ArrayList<>();

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new FileReader("C:\\Users\\satya\\Desktop\\evtol_project\\data-generation\\dynamic-obstacles\\crossing_dynamic_obstacles 2.csv")
                        ) 
        ) {

            /*
             * Skip CSV header
             */
            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {

                String[] values = line.split(",");

                /*
                 * Expected:
                 * t,x,y,z,radius
                 */
                if (values.length < 5) {
                    continue;
                }

                double t =
                        Double.parseDouble(
                                values[0].trim()
                        );

                double x =
                        Double.parseDouble(
                                values[1].trim()
                        );

                double y =
                        Double.parseDouble(
                                values[2].trim()
                        );

                double z =
                        Double.parseDouble(
                                values[3].trim()
                        );

                double radius =
                        Double.parseDouble(
                                values[4].trim()
                        );

                DynamicObstacle obstacle =
                        new DynamicObstacle(
                                t,
                                x,
                                y,
                                z,
                                radius
                        );

                dynamicObstacles.add(obstacle);

                /*
                 * Optional debug
                 */
                System.out.println(
                        "Loaded Dynamic Obstacle: "
                                + obstacle
                );
            }

        } catch (Exception e) {

            System.err.println(
                    "Failed to load dynamic obstacles"
            );

            e.printStackTrace();
        }

        return dynamicObstacles;
    }
}
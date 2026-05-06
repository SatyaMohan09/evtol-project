package com.evtol.trajectoryengine.datasource;

import com.evtol.trajectoryengine.domain.Obstacle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvObstacleDataProvider {

    @Value("${C:\\Users\\satya\\Downloads\\evtol_project_final\\evtol_project\\data-generation\\sample-data\\Obstacles_UPDATED_2.csv}")
    private String obstacleFilePath;

    public List<Obstacle> loadObstacles() {
        List<Obstacle> obstacles = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(obstacleFilePath))) {
            String line = reader.readLine(); // skip header

            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length < 4) {
                    continue;
                }

                double x = Double.parseDouble(values[0].trim());
                double y = Double.parseDouble(values[1].trim());
                double z = Double.parseDouble(values[2].trim());
                double radius = Double.parseDouble(values[3].trim());

                obstacles.add(new Obstacle(x, y, z, radius));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return obstacles;
    }
}

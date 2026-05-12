package com.evtol.trajectoryengine.datasource;

import com.evtol.trajectoryengine.domain.Waypoint;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvWaypointDataProvider {

    public List<Waypoint> loadWaypoints() {

        List<Waypoint> waypoints = new ArrayList<>();

        try {

            BufferedReader reader = new BufferedReader(
                    new FileReader("C:\\Users\\satya\\Desktop\\evtol_project\\data-generation\\sample-data\\rrt_dense_collision_test_waypoints (2).csv")
            );

            String line;

            reader.readLine(); // skip header

            while ((line = reader.readLine()) != null) {

                String[] values = line.split(",");

                double t = Double.parseDouble(values[0]);
                double x = Double.parseDouble(values[1]);
                double y = Double.parseDouble(values[2]);
                double z =  Double.parseDouble(values[3]);

                waypoints.add(new Waypoint(t, x, y, z));
            }

            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // to be removed later (just for testing)
//        for (Waypoint wp : waypoints) {
//            System.out.println(
//                    "t=" + wp.getT() +
//                            ", x=" + wp.getX() +
//                            ", y=" + wp.getY() +
//                            ", z=" + wp.getZ()
//            );
//        }

        return waypoints;
    }
}
package com.evtol.trajectoryengine.validation;

import com.evtol.trajectoryengine.domain.Waypoint;
import com.evtol.trajectoryengine.exception.InvalidInputException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class WaypointValidator {

    public void validate(List<Waypoint> waypoints){

        Set<String> uniqueCheck = new HashSet<>();

        // 1. Minimum 2 waypoints
        if (waypoints == null || waypoints.size()<2) {
            throw new InvalidInputException("Atleast 2 waypoints are required.");
        }

        for(int i=0;i<waypoints.size();i++){

            Waypoint wp = waypoints.get(i);

            // 2. No NAN/Infinite values
            if(!isFinite(wp.getT())
            || !isFinite(wp.getX())
            || !isFinite(wp.getY())
            || !isFinite(wp.getZ())){
                throw new InvalidInputException( "Invalid numeric value at waypoint index " + i);
            }

            // 3. strictly increasing timestamps
            if(i>0)
            {
                double previousTimeStamp = waypoints.get(i-1).getT();
                if(wp.getT() <= previousTimeStamp){
                    throw new InvalidInputException("Timestamps must be strictly increasing at index " + i);
                }

            }

            // 4. No duplicate waypoints
            String key = wp.getT() + "|" + wp.getX() + "|" + wp.getY() + "|" + wp.getZ();
            if(!uniqueCheck.add(key)){
                throw new InvalidInputException("Duplicate waypoint detected at index " + i);
            }

        }
    }
    private boolean isFinite(Double value){
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}

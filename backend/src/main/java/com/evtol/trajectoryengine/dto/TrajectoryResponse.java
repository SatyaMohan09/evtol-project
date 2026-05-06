package com.evtol.trajectoryengine.dto;

import com.evtol.trajectoryengine.domain.TrajectoryPoint;
import com.evtol.trajectoryengine.domain.Waypoint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrajectoryResponse {

    private List<TrajectoryPoint> trajectory;
    private List <Waypoint> waypoints;
    private List<Waypoint> controlPoints;
    private double totalDuration;
}

package com.evtol.trajectoryengine.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Waypoint {

    private final double t;
    private final double x;
    private final double y;
    private final double z;

}
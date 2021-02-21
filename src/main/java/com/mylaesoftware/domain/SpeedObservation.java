package com.mylaesoftware.domain;

import lombok.Value;

@Value
public class SpeedObservation {
    public String radarId;
    public String licensePlate;
    public int metersPerSecond;
}

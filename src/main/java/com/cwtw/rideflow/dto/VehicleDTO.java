package com.cwtw.rideflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleDTO {
    private Long id;
    private String plateNumber;
    private String model;
    private String status;
    private Long driverId;
    private Long assignedDriverId;
    private String driverEmail;
    private String assignedDriverEmail;
    private String driverName;
    private String assignedDriverName;
}
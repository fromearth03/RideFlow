package com.cwtw.rideflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverDTO {
    private Long id;
    private Long userId;
    private String email;
    private String licenseNumber;
    private boolean isAvailable;
    private boolean approved;
    private List<Long> vehicleIds;
    private List<String> vehicleModels;
}

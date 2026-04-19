package com.cwtw.rideflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideResponseDTO {
    private Long id;
    private String pickupLocation;
    private String dropLocation;
    private BigDecimal fare;
    private boolean interCity;
    private String status;
    private Long driverId;
}

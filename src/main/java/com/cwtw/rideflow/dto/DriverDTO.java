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
public class DriverDTO {
    private Long id;
    private Long userId;
    private String email;
    private String licenseNumber;
    private boolean isAvailable;
    private boolean approved;
}

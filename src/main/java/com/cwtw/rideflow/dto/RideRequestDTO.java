package com.cwtw.rideflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestDTO {

    @NotBlank(message = "Pickup location is required")
    private String pickupLocation;

    @NotBlank(message = "Drop location is required")
    private String dropLocation;

    @NotNull(message = "Scheduled time is required")
    private LocalDateTime scheduledTime;

    @NotNull(message = "Fare is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Fare must be greater than or equal to 0")
    private BigDecimal fare;

    private Boolean interCity;

    private Long customerUserId;

    private String customerEmail;
}

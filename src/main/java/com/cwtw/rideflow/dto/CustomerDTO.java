package com.cwtw.rideflow.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {
    private Long id;
    private Long userId;
    private String email;
    private String phoneNumber;
}

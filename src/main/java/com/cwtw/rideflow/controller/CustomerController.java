package com.cwtw.rideflow.controller;

import com.cwtw.rideflow.dto.CustomerDTO;
import com.cwtw.rideflow.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/profile")
    public ResponseEntity<CustomerDTO> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(customerService.getCustomerProfile(userDetails.getUsername()));
    }

    @PutMapping("/profile/phone")
    public ResponseEntity<CustomerDTO> updatePhoneNumber(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(customerService.updatePhoneNumber(userDetails.getUsername(), body.get("phoneNumber")));
    }
}

package com.cwtw.rideflow.controller;

import com.cwtw.rideflow.dto.CustomerDTO;
import com.cwtw.rideflow.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class CustomerLookupController {

    private final CustomerService customerService;

    public CustomerLookupController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/customers/user-id")
    public ResponseEntity<Map<String, Object>> getCustomerUserIdByEmail(@RequestParam String email) {
        Long userId = customerService.getCustomerUserIdByEmail(email);
        return ResponseEntity.ok(Map.of("email", email, "userId", userId));
    }
}

package com.cwtw.rideflow.controller;

import com.cwtw.rideflow.dto.RideRequestDTO;
import com.cwtw.rideflow.dto.RideResponseDTO;
import com.cwtw.rideflow.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    public ResponseEntity<RideResponseDTO> createRide(
            @Valid @RequestBody RideRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(rideService.createRide(request, userDetails.getUsername()));
    }

    @PostMapping("/{rideId}/assign/{driverId}")
    public ResponseEntity<RideResponseDTO> assignDriver(
            @PathVariable Long rideId,
            @PathVariable Long driverId) {
        return ResponseEntity.ok(rideService.assignDriver(rideId, driverId));
    }

    @PatchMapping("/{rideId}/status")
    public ResponseEntity<RideResponseDTO> updateStatus(
            @PathVariable Long rideId,
            @RequestParam String status) {
        return ResponseEntity.ok(rideService.updateStatus(rideId, status));
    }

    @GetMapping
    public ResponseEntity<List<RideResponseDTO>> getAllRides() {
        return ResponseEntity.ok(rideService.getAllRides());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RideResponseDTO>> getRidesByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(rideService.getRidesByUserId(userId));
    }
}

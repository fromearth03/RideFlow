package com.cwtw.rideflow.controller;

import com.cwtw.rideflow.dto.DispatcherDTO;
import com.cwtw.rideflow.dto.RideRequestDTO;
import com.cwtw.rideflow.dto.RideResponseDTO;
import com.cwtw.rideflow.service.DispatcherService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dispatcher")
public class DispatcherController {

    private final DispatcherService dispatcherService;

    public DispatcherController(DispatcherService dispatcherService) {
        this.dispatcherService = dispatcherService;
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public ResponseEntity<DispatcherDTO> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(dispatcherService.getDispatcherProfile(userDetails.getUsername()));
    }

    // ── Ride dispatching ──────────────────────────────────────────────────────

    @PostMapping("/rides")
    public ResponseEntity<RideResponseDTO> createRideForCustomer(
            @Valid @RequestBody RideRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(dispatcherService.createRideForCustomer(request, userDetails.getUsername()));
    }

    @PostMapping("/rides/{rideId}/assign/{driverId}")
    public ResponseEntity<RideResponseDTO> assignDriver(
            @PathVariable Long rideId,
            @PathVariable Long driverId) {
        return ResponseEntity.ok(dispatcherService.assignDriver(rideId, driverId));
    }

    @PostMapping("/rides/{rideId}/auto-assign")
    public ResponseEntity<RideResponseDTO> autoAssignDriver(@PathVariable Long rideId) {
        return ResponseEntity.ok(dispatcherService.autoAssignDriver(rideId));
    }
}

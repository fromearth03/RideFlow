package com.cwtw.rideflow.controller;

import com.cwtw.rideflow.dto.DriverDTO;
import com.cwtw.rideflow.service.DriverService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    public ResponseEntity<DriverDTO> createDriver(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String licenseNumber = body.get("licenseNumber").toString();
        return ResponseEntity.ok(driverService.createDriver(userId, licenseNumber));
    }

    @PatchMapping("/{driverId}/availability")
    public ResponseEntity<DriverDTO> updateAvailability(
            @PathVariable Long driverId,
            @RequestParam boolean available) {
        return ResponseEntity.ok(driverService.updateAvailability(driverId, available));
    }

    @GetMapping
    public ResponseEntity<List<DriverDTO>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }
}

package com.cwtw.rideflow.controller;

import com.cwtw.rideflow.dto.CustomerDTO;
import com.cwtw.rideflow.dto.DispatcherDTO;
import com.cwtw.rideflow.dto.DriverDTO;
import com.cwtw.rideflow.model.MaintenanceRecord;
import com.cwtw.rideflow.model.User;
import com.cwtw.rideflow.model.Vehicle;
import com.cwtw.rideflow.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ── Vehicle ──────────────────────────────────────────────────────────────

    @PostMapping("/vehicles")
    public ResponseEntity<Vehicle> addVehicle(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.addVehicle(
                body.get("plateNumber"),
                body.get("model"),
                body.get("status")));
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        return ResponseEntity.ok(adminService.getAllVehicles());
    }

    @PostMapping("/vehicles/{vehicleId}/maintenance")
    public ResponseEntity<MaintenanceRecord> addMaintenanceRecord(
            @PathVariable Long vehicleId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(adminService.addMaintenanceRecord(vehicleId, body.get("description")));
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    // ── Drivers ───────────────────────────────────────────────────────────────

    @GetMapping("/drivers")
    public ResponseEntity<List<DriverDTO>> getAllDrivers() {
        return ResponseEntity.ok(adminService.getAllDrivers());
    }

    @DeleteMapping("/drivers/{driverId}")
    public ResponseEntity<Void> deleteDriver(@PathVariable Long driverId) {
        adminService.deleteDriver(driverId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/drivers/{driverId}/approve")
    public ResponseEntity<DriverDTO> approveDriver(@PathVariable Long driverId) {
        return ResponseEntity.ok(adminService.approveDriver(driverId));
    }

    // ── Dispatchers ───────────────────────────────────────────────────────────

    @GetMapping("/dispatchers")
    public ResponseEntity<List<DispatcherDTO>> getAllDispatchers() {
        return ResponseEntity.ok(adminService.getAllDispatchers());
    }

    @DeleteMapping("/dispatchers/{dispatcherId}")
    public ResponseEntity<Void> deleteDispatcher(@PathVariable Long dispatcherId) {
        adminService.deleteDispatcher(dispatcherId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/dispatchers/{dispatcherId}/approve")
    public ResponseEntity<DispatcherDTO> approveDispatcher(@PathVariable Long dispatcherId) {
        return ResponseEntity.ok(adminService.approveDispatcher(dispatcherId));
    }

    // ── Customers ─────────────────────────────────────────────────────────────

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        return ResponseEntity.ok(adminService.getAllCustomers());
    }

    @DeleteMapping("/customers/{customerId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long customerId) {
        adminService.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }
}

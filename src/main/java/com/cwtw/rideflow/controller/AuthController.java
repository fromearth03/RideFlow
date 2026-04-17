package com.cwtw.rideflow.controller;

import com.cwtw.rideflow.dto.AuthRequestDTO;
import com.cwtw.rideflow.dto.AuthResponseDTO;
import com.cwtw.rideflow.dto.DriverRegisterRequestDTO;
import com.cwtw.rideflow.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody AuthRequestDTO request) {
        return ResponseEntity.ok(authService.register(request, "ROLE_CUSTOMER"));
    }

    @PostMapping("/register/driver")
    public ResponseEntity<AuthResponseDTO> registerDriver(
            @Valid @RequestBody DriverRegisterRequestDTO request) {
        return ResponseEntity.ok(authService.registerDriver(request));
    }

    @PostMapping("/register/dispatcher")
    public ResponseEntity<AuthResponseDTO> registerDispatcher(
            @Valid @RequestBody AuthRequestDTO request) {
        return ResponseEntity.ok(authService.register(request, "ROLE_DISPATCHER"));
    }

    @PostMapping("/register/admin")
    public ResponseEntity<AuthResponseDTO> registerAdmin(
            @RequestHeader("X-Admin-Secret-Key") String adminSecretKey,
            @Valid @RequestBody AuthRequestDTO request) {
        return ResponseEntity.ok(authService.registerWithAdminKey(request, "ROLE_ADMIN", adminSecretKey));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }
}

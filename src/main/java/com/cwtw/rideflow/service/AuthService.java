package com.cwtw.rideflow.service;

import com.cwtw.rideflow.dto.AuthRequestDTO;
import com.cwtw.rideflow.dto.AuthResponseDTO;
import com.cwtw.rideflow.dto.DriverRegisterRequestDTO;
import com.cwtw.rideflow.exception.CustomException;
import com.cwtw.rideflow.model.Customer;
import com.cwtw.rideflow.model.Dispatcher;
import com.cwtw.rideflow.model.Driver;
import com.cwtw.rideflow.model.User;
import com.cwtw.rideflow.model.Vehicle;
import com.cwtw.rideflow.repository.CustomerRepository;
import com.cwtw.rideflow.repository.DispatcherRepository;
import com.cwtw.rideflow.repository.DriverRepository;
import com.cwtw.rideflow.repository.UserRepository;
import com.cwtw.rideflow.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import com.cwtw.rideflow.repository.VehicleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomerRepository customerRepository;
    private final DispatcherRepository dispatcherRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final String adminSecret;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CustomerRepository customerRepository,
            DispatcherRepository dispatcherRepository,
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository,
            @Value("${admin.secret:}") String adminSecret) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.customerRepository = customerRepository;
        this.dispatcherRepository = dispatcherRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.adminSecret = adminSecret;
    }

    /**
     * Register for customer, dispatcher, or admin (no extra profile fields needed).
     */
    @Transactional
    public AuthResponseDTO register(AuthRequestDTO request, String role) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new CustomException("Email already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);

        if ("ROLE_CUSTOMER".equals(role)) {
            customerRepository.save(Customer.builder().user(user).build());
        } else if ("ROLE_DISPATCHER".equals(role)) {
            dispatcherRepository.save(Dispatcher.builder().user(user).build());
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponseDTO(token, user.getRole());
    }

    @Transactional
    public AuthResponseDTO registerWithAdminKey(AuthRequestDTO request, String role, String providedAdminSecretKey) {
        validateAdminSecretKey(providedAdminSecretKey);
        return register(request, role);
    }

    /**
     * Register a driver — requires a licenseNumber in addition to email/password.
     */
    @Transactional
    public AuthResponseDTO registerDriver(DriverRegisterRequestDTO request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new CustomException("Email already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_DRIVER")
                .build();

        userRepository.save(user);

        Driver driver = Driver.builder()
                .user(user)
                .licenseNumber(request.getLicenseNumber())
                .isAvailable(true)
                .build();

        driverRepository.save(driver);

        if (request.getVehiclePlateNumber() != null && !request.getVehiclePlateNumber().isBlank()) {
            String plateNumber = request.getVehiclePlateNumber().trim();

            Vehicle vehicle = vehicleRepository.findByPlateNumber(plateNumber)
                    .orElseGet(() -> vehicleRepository.save(Vehicle.builder()
                            .plateNumber(plateNumber)
                            .model(null)
                            .status(null)
                            .build()));

            if (vehicle.getDriver() != null) {
                throw new CustomException("Vehicle is already assigned to another driver", HttpStatus.CONFLICT);
            }

            vehicle.setDriver(driver);
            vehicleRepository.save(vehicle);
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponseDTO(token, user.getRole());
    }

    @Transactional
    public AuthResponseDTO registerDriver(DriverRegisterRequestDTO request, String providedAdminSecretKey) {
        validateAdminSecretKey(providedAdminSecretKey);
        return registerDriver(request);
    }

    public AuthResponseDTO login(AuthRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponseDTO(token, user.getRole());
    }

    private void validateAdminSecretKey(String providedAdminSecretKey) {
        if (providedAdminSecretKey == null || providedAdminSecretKey.isBlank()) {
            throw new CustomException("Admin secret key is required", HttpStatus.BAD_REQUEST);
        }

        boolean matches = MessageDigest.isEqual(
                providedAdminSecretKey.getBytes(StandardCharsets.UTF_8),
                adminSecret.getBytes(StandardCharsets.UTF_8));

        if (!matches) {
            throw new CustomException("Invalid admin secret key", HttpStatus.FORBIDDEN);
        }
    }
}

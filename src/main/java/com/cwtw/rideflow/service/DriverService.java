package com.cwtw.rideflow.service;

import com.cwtw.rideflow.dto.DriverDTO;
import com.cwtw.rideflow.exception.CustomException;
import com.cwtw.rideflow.model.Driver;
import com.cwtw.rideflow.model.User;
import com.cwtw.rideflow.repository.DriverRepository;
import com.cwtw.rideflow.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;

    public DriverService(DriverRepository driverRepository, UserRepository userRepository) {
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
    }

    public DriverDTO createDriver(Long userId, String licenseNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        Driver driver = Driver.builder()
                .user(user)
                .licenseNumber(licenseNumber)
                .isAvailable(true)
                .build();

        driverRepository.save(driver);
        return mapToDTO(driver);
    }

    public DriverDTO updateAvailability(Long driverId, boolean available) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new CustomException("Driver not found", HttpStatus.NOT_FOUND));

        driver.setAvailable(available);
        driverRepository.save(driver);
        return mapToDTO(driver);
    }

    public List<DriverDTO> getAllDrivers() {
        return driverRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private DriverDTO mapToDTO(Driver driver) {
        return DriverDTO.builder()
                .id(driver.getId())
                .userId(driver.getUser().getId())
                .email(driver.getUser().getEmail())
                .licenseNumber(driver.getLicenseNumber())
                .isAvailable(driver.isAvailable())
                .approved(driver.isApproved())
                .build();
    }
}

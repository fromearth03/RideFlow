package com.cwtw.rideflow.service;

import com.cwtw.rideflow.dto.DriverDTO;
import com.cwtw.rideflow.exception.CustomException;
import com.cwtw.rideflow.model.Driver;
import com.cwtw.rideflow.model.User;
import com.cwtw.rideflow.repository.DriverRepository;
import com.cwtw.rideflow.repository.UserRepository;
import com.cwtw.rideflow.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cwtw.rideflow.model.Vehicle;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public DriverService(DriverRepository driverRepository, UserRepository userRepository,
            VehicleRepository vehicleRepository) {
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
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

    public DriverDTO updateAssignedVehicleDetails(Long driverId, Long vehicleId, String model, String status) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new CustomException("Driver not found", HttpStatus.NOT_FOUND));

        Vehicle vehicle = vehicleRepository.findByIdAndDriverId(vehicleId, driverId)
                .orElseThrow(() -> new CustomException("Vehicle is not assigned to this driver", HttpStatus.NOT_FOUND));

        if (model != null) {
            vehicle.setModel(model.trim().isEmpty() ? null : model.trim());
        }

        if (status != null) {
            vehicle.setStatus(status.trim().isEmpty() ? null : status.trim().toUpperCase());
        }

        vehicleRepository.save(vehicle);
        driverRepository.save(driver);
        return mapToDTO(driver);
    }

    private DriverDTO mapToDTO(Driver driver) {
        List<Vehicle> assignedVehicles = vehicleRepository.findByDriverId(driver.getId());
        List<Long> vehicleIds = assignedVehicles.stream().map(Vehicle::getId).collect(Collectors.toList());
        List<String> vehicleModels = assignedVehicles.stream().map(Vehicle::getModel).collect(Collectors.toList());

        return DriverDTO.builder()
                .id(driver.getId())
                .userId(driver.getUser().getId())
                .email(driver.getUser().getEmail())
                .licenseNumber(driver.getLicenseNumber())
                .isAvailable(driver.isAvailable())
                .approved(driver.isApproved())
                .vehicleIds(vehicleIds)
            .vehicleModels(vehicleModels)
                .build();
    }
}

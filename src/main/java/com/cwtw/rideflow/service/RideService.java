package com.cwtw.rideflow.service;

import com.cwtw.rideflow.dto.RideRequestDTO;
import com.cwtw.rideflow.dto.RideResponseDTO;
import com.cwtw.rideflow.exception.CustomException;
import com.cwtw.rideflow.model.Driver;
import com.cwtw.rideflow.model.Ride;
import com.cwtw.rideflow.model.User;
import com.cwtw.rideflow.repository.DriverRepository;
import com.cwtw.rideflow.repository.RideRepository;
import com.cwtw.rideflow.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;

    public RideService(RideRepository rideRepository, UserRepository userRepository,
            DriverRepository driverRepository) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
    }

    public RideResponseDTO createRide(RideRequestDTO request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        Ride ride = Ride.builder()
                .pickupLocation(request.getPickupLocation())
                .dropLocation(request.getDropLocation())
                .scheduledTime(request.getScheduledTime())
            .interCity(Boolean.TRUE.equals(request.getInterCity()))
                .status(Ride.RideStatus.PENDING)
                .user(user)
                .build();

        rideRepository.save(ride);
        return mapToDTO(ride);
    }

    public RideResponseDTO assignDriver(Long rideId, Long driverId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new CustomException("Ride not found", HttpStatus.NOT_FOUND));

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new CustomException("Driver not found", HttpStatus.NOT_FOUND));

        if (!driver.isAvailable()) {
            throw new CustomException("Driver is not available", HttpStatus.BAD_REQUEST);
        }

        ride.setDriver(driver);
        ride.setStatus(Ride.RideStatus.ASSIGNED);
        driver.setAvailable(false);

        driverRepository.save(driver);
        rideRepository.save(ride);
        return mapToDTO(ride);
    }

    public RideResponseDTO updateStatus(Long rideId, String status) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new CustomException("Ride not found", HttpStatus.NOT_FOUND));

        try {
            ride.setStatus(Ride.RideStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new CustomException("Invalid ride status: " + status, HttpStatus.BAD_REQUEST);
        }

        if (ride.getStatus() == Ride.RideStatus.COMPLETED || ride.getStatus() == Ride.RideStatus.CANCELLED) {
            if (ride.getDriver() != null) {
                ride.getDriver().setAvailable(true);
                driverRepository.save(ride.getDriver());
            }
        }

        rideRepository.save(ride);
        return mapToDTO(ride);
    }

    public List<RideResponseDTO> getAllRides() {
        return rideRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<RideResponseDTO> getRidesByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException("User not found", HttpStatus.NOT_FOUND);
        }

        return rideRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private RideResponseDTO mapToDTO(Ride ride) {
        return RideResponseDTO.builder()
                .id(ride.getId())
                .pickupLocation(ride.getPickupLocation())
                .dropLocation(ride.getDropLocation())
            .interCity(ride.isInterCity())
                .status(ride.getStatus().name())
                .driverId(ride.getDriver() != null ? ride.getDriver().getId() : null)
                .build();
    }
}

package com.cwtw.rideflow.service;

import com.cwtw.rideflow.dto.DispatcherDTO;
import com.cwtw.rideflow.exception.CustomException;
import com.cwtw.rideflow.model.Dispatcher;
import com.cwtw.rideflow.model.User;
import com.cwtw.rideflow.repository.DispatcherRepository;
import com.cwtw.rideflow.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DispatcherService {

    private final DispatcherRepository dispatcherRepository;
    private final UserRepository userRepository;
    private final RideService rideService;
    private final com.cwtw.rideflow.repository.DriverRepository driverRepository;

    public DispatcherService(
            DispatcherRepository dispatcherRepository,
            UserRepository userRepository,
            RideService rideService,
            com.cwtw.rideflow.repository.DriverRepository driverRepository) {
        this.dispatcherRepository = dispatcherRepository;
        this.userRepository = userRepository;
        this.rideService = rideService;
        this.driverRepository = driverRepository;
    }

    // --- Ride dispatching operations ---

    public com.cwtw.rideflow.dto.RideResponseDTO createRideForCustomer(
            com.cwtw.rideflow.dto.RideRequestDTO request, String userEmail) {
        return rideService.createRide(request, userEmail);
    }

    public com.cwtw.rideflow.dto.RideResponseDTO assignDriver(Long rideId, Long driverId) {
        return rideService.assignDriver(rideId, driverId);
    }

    public com.cwtw.rideflow.dto.RideResponseDTO autoAssignDriver(Long rideId) {
        List<com.cwtw.rideflow.model.Driver> availableDrivers = driverRepository.findByIsAvailable(true);
        if (availableDrivers.isEmpty()) {
            throw new CustomException("No available drivers at the moment", HttpStatus.NOT_FOUND);
        }
        return rideService.assignDriver(rideId, availableDrivers.get(0).getId());
    }

    // --- Dispatcher profile operations ---

    public DispatcherDTO createDispatcher(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if (dispatcherRepository.findByUserId(userId).isPresent()) {
            throw new CustomException("Dispatcher profile already exists for this user", HttpStatus.CONFLICT);
        }

        Dispatcher dispatcher = Dispatcher.builder()
                .user(user)
                .build();

        dispatcherRepository.save(dispatcher);
        return mapToDTO(dispatcher);
    }

    public DispatcherDTO getDispatcherProfile(String email) {
        Dispatcher dispatcher = dispatcherRepository.findByUserEmail(email)
                .orElseThrow(() -> new CustomException("Dispatcher profile not found", HttpStatus.NOT_FOUND));
        return mapToDTO(dispatcher);
    }

    public List<DispatcherDTO> getAllDispatchers() {
        return dispatcherRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public void deleteDispatcher(Long dispatcherId) {
        Dispatcher dispatcher = dispatcherRepository.findById(dispatcherId)
                .orElseThrow(() -> new CustomException("Dispatcher not found", HttpStatus.NOT_FOUND));
        dispatcherRepository.delete(dispatcher);
    }

    public DispatcherDTO mapToDTO(Dispatcher dispatcher) {
        return DispatcherDTO.builder()
                .id(dispatcher.getId())
                .userId(dispatcher.getUser().getId())
                .email(dispatcher.getUser().getEmail())
                .approved(dispatcher.isApproved())
                .build();
    }
}

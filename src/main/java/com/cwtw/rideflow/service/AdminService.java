package com.cwtw.rideflow.service;

import com.cwtw.rideflow.dto.CustomerDTO;
import com.cwtw.rideflow.dto.DispatcherDTO;
import com.cwtw.rideflow.dto.DriverDTO;
import com.cwtw.rideflow.exception.CustomException;
import com.cwtw.rideflow.model.*;
import com.cwtw.rideflow.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AdminService {

    private final VehicleRepository vehicleRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final DispatcherRepository dispatcherRepository;
    private final CustomerRepository customerRepository;

    public AdminService(
            VehicleRepository vehicleRepository,
            MaintenanceRecordRepository maintenanceRecordRepository,
            UserRepository userRepository,
            DriverRepository driverRepository,
            DispatcherRepository dispatcherRepository,
            CustomerRepository customerRepository) {
        this.vehicleRepository = vehicleRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.dispatcherRepository = dispatcherRepository;
        this.customerRepository = customerRepository;
    }

    // ── Vehicle ──────────────────────────────────────────────────────────────

    public Vehicle addVehicle(String plateNumber, String model, String status) {
        Vehicle vehicle = Vehicle.builder()
                .plateNumber(plateNumber)
                .model(model)
                .status(status != null ? status : "ACTIVE")
                .build();
        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public MaintenanceRecord addMaintenanceRecord(Long vehicleId, String description) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new CustomException("Vehicle not found", HttpStatus.NOT_FOUND));

        MaintenanceRecord record = MaintenanceRecord.builder()
                .vehicle(vehicle)
                .description(description)
                .date(LocalDate.now())
                .build();

        return maintenanceRecordRepository.save(record);
    }

    // ── Users (all types) ─────────────────────────────────────────────────────

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException("User not found", HttpStatus.NOT_FOUND);
        }
        // Cascade: delete linked profile first if exists
        driverRepository.findAll().stream()
                .filter(d -> d.getUser().getId().equals(userId))
                .findFirst()
                .ifPresent(driverRepository::delete);

        dispatcherRepository.findAll().stream()
                .filter(d -> d.getUser().getId().equals(userId))
                .findFirst()
                .ifPresent(dispatcherRepository::delete);

        customerRepository.findAll().stream()
                .filter(c -> c.getUser().getId().equals(userId))
                .findFirst()
                .ifPresent(customerRepository::delete);

        userRepository.deleteById(userId);
    }

    // ── Drivers ───────────────────────────────────────────────────────────────

    public List<DriverDTO> getAllDrivers() {
        return driverRepository.findAll().stream()
                .map(this::toDriverDTO)
                .toList();
    }

    @Transactional
    public void deleteDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new CustomException("Driver not found", HttpStatus.NOT_FOUND));
        driverRepository.delete(driver);
    }

    public DriverDTO approveDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new CustomException("Driver not found", HttpStatus.NOT_FOUND));
        driver.setApproved(true);
        driverRepository.save(driver);
        return toDriverDTO(driver);
    }

    // ── Dispatchers ───────────────────────────────────────────────────────────

    public List<DispatcherDTO> getAllDispatchers() {
        return dispatcherRepository.findAll().stream()
                .map(this::toDispatcherDTO)
                .toList();
    }

    @Transactional
    public void deleteDispatcher(Long dispatcherId) {
        Dispatcher dispatcher = dispatcherRepository.findById(dispatcherId)
                .orElseThrow(() -> new CustomException("Dispatcher not found", HttpStatus.NOT_FOUND));
        dispatcherRepository.delete(dispatcher);
    }

    public DispatcherDTO approveDispatcher(Long dispatcherId) {
        Dispatcher dispatcher = dispatcherRepository.findById(dispatcherId)
                .orElseThrow(() -> new CustomException("Dispatcher not found", HttpStatus.NOT_FOUND));
        dispatcher.setApproved(true);
        dispatcherRepository.save(dispatcher);
        return toDispatcherDTO(dispatcher);
    }

    // ── Customers ─────────────────────────────────────────────────────────────

    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::toCustomerDTO)
                .toList();
    }

    @Transactional
    public void deleteCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomException("Customer not found", HttpStatus.NOT_FOUND));
        customerRepository.delete(customer);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private DriverDTO toDriverDTO(Driver d) {
        return DriverDTO.builder()
                .id(d.getId())
                .userId(d.getUser().getId())
                .email(d.getUser().getEmail())
                .licenseNumber(d.getLicenseNumber())
                .isAvailable(d.isAvailable())
                .approved(d.isApproved())
                .build();
    }

    private DispatcherDTO toDispatcherDTO(Dispatcher d) {
        return DispatcherDTO.builder()
                .id(d.getId())
                .userId(d.getUser().getId())
                .email(d.getUser().getEmail())
                .approved(d.isApproved())
                .build();
    }

    private CustomerDTO toCustomerDTO(Customer c) {
        return CustomerDTO.builder()
                .id(c.getId())
                .userId(c.getUser().getId())
                .email(c.getUser().getEmail())
                .phoneNumber(c.getPhoneNumber())
                .build();
    }
}

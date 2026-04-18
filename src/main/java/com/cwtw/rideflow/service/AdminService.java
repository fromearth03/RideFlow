package com.cwtw.rideflow.service;

import com.cwtw.rideflow.dto.CustomerDTO;
import com.cwtw.rideflow.dto.DispatcherDTO;
import com.cwtw.rideflow.dto.DriverDTO;
import com.cwtw.rideflow.dto.VehicleDTO;
import com.cwtw.rideflow.exception.CustomException;
import com.cwtw.rideflow.model.*;
import com.cwtw.rideflow.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final VehicleRepository vehicleRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final DispatcherRepository dispatcherRepository;
    private final CustomerRepository customerRepository;
        private final RideRepository rideRepository;

    public AdminService(
            VehicleRepository vehicleRepository,
            MaintenanceRecordRepository maintenanceRecordRepository,
            UserRepository userRepository,
            DriverRepository driverRepository,
            DispatcherRepository dispatcherRepository,
                        CustomerRepository customerRepository,
                        RideRepository rideRepository) {
        this.vehicleRepository = vehicleRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.dispatcherRepository = dispatcherRepository;
        this.customerRepository = customerRepository;
                this.rideRepository = rideRepository;
    }

    // ── Vehicle ──────────────────────────────────────────────────────────────

    public VehicleDTO addVehicle(String plateNumber, String model, String status) {
        Vehicle vehicle = Vehicle.builder()
                .plateNumber(plateNumber)
                .model(model)
                .status(status != null ? status : "ACTIVE")
                .build();
        return toVehicleDTO(vehicleRepository.save(vehicle));
    }

    public List<VehicleDTO> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(this::toVehicleDTO)
                .toList();
    }

    @Transactional
    public void deleteVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new CustomException("Vehicle not found", HttpStatus.NOT_FOUND));

        vehicle.setDriver(null);
        vehicleRepository.save(vehicle);

        maintenanceRecordRepository.deleteByVehicleId(vehicleId);
        vehicleRepository.delete(vehicle);
    }

    @Transactional
    public VehicleDTO disableVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new CustomException("Vehicle not found", HttpStatus.NOT_FOUND));

        vehicle.setStatus("INACTIVE");
        return toVehicleDTO(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleDTO enableVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new CustomException("Vehicle not found", HttpStatus.NOT_FOUND));

        vehicle.setStatus("ACTIVE");
        return toVehicleDTO(vehicleRepository.save(vehicle));
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

                detachRidesFromUser(userId);

        // Cascade: delete linked profile first if exists
        driverRepository.findAll().stream()
                .filter(d -> d.getUser().getId().equals(userId))
                .findFirst()
                .ifPresent(driver -> {
                                        detachRidesFromDriver(driver.getId());
                    List<Vehicle> assignedVehicles = vehicleRepository.findByDriverId(driver.getId());
                    assignedVehicles.forEach(vehicle -> vehicle.setDriver(null));
                    vehicleRepository.saveAll(assignedVehicles);
                    driverRepository.delete(driver);
                });

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

                Long userId = driver.getUser().getId();

        detachRidesFromDriver(driverId);
                detachRidesFromUser(userId);

        List<Vehicle> assignedVehicles = vehicleRepository.findByDriverId(driverId);
        assignedVehicles.forEach(vehicle -> vehicle.setDriver(null));
        vehicleRepository.saveAll(assignedVehicles);

        driverRepository.delete(driver);
                userRepository.deleteById(userId);
    }

    public DriverDTO approveDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new CustomException("Driver not found", HttpStatus.NOT_FOUND));
        driver.setApproved(true);
        driverRepository.save(driver);
        return toDriverDTO(driver);
    }

    @Transactional
    public DriverDTO assignVehicleToDriver(Long driverId, Long vehicleId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new CustomException("Driver not found", HttpStatus.NOT_FOUND));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new CustomException("Vehicle not found", HttpStatus.NOT_FOUND));

        if (vehicle.getDriver() != null && !vehicle.getDriver().getId().equals(driverId)) {
            throw new CustomException("Vehicle is already assigned to another driver", HttpStatus.CONFLICT);
        }

        vehicle.setDriver(driver);
        vehicleRepository.save(vehicle);
        return toDriverDTO(driver);
    }

    @Transactional
    public DriverDTO unassignVehicleFromDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new CustomException("Driver not found", HttpStatus.NOT_FOUND));

        List<Vehicle> assignedVehicles = vehicleRepository.findByDriverId(driverId);
        assignedVehicles.forEach(vehicle -> vehicle.setDriver(null));
        vehicleRepository.saveAll(assignedVehicles);
        return toDriverDTO(driver);
    }

    @Transactional
    public DriverDTO unassignSpecificVehicleFromDriver(Long driverId, Long vehicleId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new CustomException("Driver not found", HttpStatus.NOT_FOUND));

        Vehicle vehicle = vehicleRepository.findByIdAndDriverId(vehicleId, driverId)
                .orElseThrow(() -> new CustomException("Vehicle is not assigned to this driver", HttpStatus.NOT_FOUND));

        vehicle.setDriver(null);
        vehicleRepository.save(vehicle);
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

                Long userId = dispatcher.getUser().getId();

        dispatcherRepository.delete(dispatcher);
                userRepository.deleteById(userId);
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

                Long userId = customer.getUser().getId();

                detachRidesFromUser(userId);

        customerRepository.delete(customer);
                userRepository.deleteById(userId);
    }

        private void detachRidesFromUser(Long userId) {
                List<Ride> rides = rideRepository.findByUserId(userId);
                if (rides.isEmpty()) {
                        return;
                }
                rides.forEach(ride -> ride.setUser(null));
                rideRepository.saveAll(rides);
        }

        private void detachRidesFromDriver(Long driverId) {
                List<Ride> rides = rideRepository.findByDriverId(driverId);
                if (rides.isEmpty()) {
                        return;
                }
                rides.forEach(ride -> ride.setDriver(null));
                rideRepository.saveAll(rides);
        }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private DriverDTO toDriverDTO(Driver d) {
        List<Vehicle> assignedVehicles = vehicleRepository.findByDriverId(d.getId());
        List<Long> vehicleIds = assignedVehicles.stream().map(Vehicle::getId).collect(Collectors.toList());
        List<String> vehicleModels = assignedVehicles.stream().map(Vehicle::getModel).collect(Collectors.toList());

        return DriverDTO.builder()
                .id(d.getId())
                .userId(d.getUser().getId())
                .email(d.getUser().getEmail())
                .licenseNumber(d.getLicenseNumber())
                .isAvailable(d.isAvailable())
                .approved(d.isApproved())
                .vehicleIds(vehicleIds)
                .vehicleModels(vehicleModels)
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

    private VehicleDTO toVehicleDTO(Vehicle vehicle) {
        Long assignedDriverId = vehicle.getDriver() != null ? vehicle.getDriver().getId() : null;
        String assignedDriverEmail = vehicle.getDriver() != null && vehicle.getDriver().getUser() != null
                ? vehicle.getDriver().getUser().getEmail()
                : null;

        return VehicleDTO.builder()
                .id(vehicle.getId())
                .plateNumber(vehicle.getPlateNumber())
                .model(vehicle.getModel())
                .status(vehicle.getStatus())
                .driverId(assignedDriverId)
                .assignedDriverId(assignedDriverId)
                .driverEmail(assignedDriverEmail)
                .assignedDriverEmail(assignedDriverEmail)
                .driverName(assignedDriverEmail)
                .assignedDriverName(assignedDriverEmail)
                .build();
    }
}

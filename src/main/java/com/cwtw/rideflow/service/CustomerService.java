package com.cwtw.rideflow.service;

import com.cwtw.rideflow.dto.CustomerDTO;
import com.cwtw.rideflow.exception.CustomException;
import com.cwtw.rideflow.model.Customer;
import com.cwtw.rideflow.model.User;
import com.cwtw.rideflow.repository.CustomerRepository;
import com.cwtw.rideflow.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    public CustomerDTO createCustomer(Long userId, String phoneNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if (customerRepository.findByUserId(userId).isPresent()) {
            throw new CustomException("Customer profile already exists for this user", HttpStatus.CONFLICT);
        }

        Customer customer = Customer.builder()
                .user(user)
                .phoneNumber(phoneNumber)
                .build();

        customerRepository.save(customer);
        return mapToDTO(customer);
    }

    public CustomerDTO getCustomerProfile(String email) {
        Customer customer = customerRepository.findByUserEmail(email)
                .orElseThrow(() -> new CustomException("Customer profile not found", HttpStatus.NOT_FOUND));
        return mapToDTO(customer);
    }

    public CustomerDTO updatePhoneNumber(String email, String phoneNumber) {
        Customer customer = customerRepository.findByUserEmail(email)
                .orElseThrow(() -> new CustomException("Customer profile not found", HttpStatus.NOT_FOUND));
        customer.setPhoneNumber(phoneNumber);
        customerRepository.save(customer);
        return mapToDTO(customer);
    }

    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public void deleteCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomException("Customer not found", HttpStatus.NOT_FOUND));
        customerRepository.delete(customer);
    }

    public CustomerDTO mapToDTO(Customer customer) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .userId(customer.getUser().getId())
                .email(customer.getUser().getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .build();
    }
}

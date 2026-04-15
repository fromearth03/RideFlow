package com.cwtw.rideflow.service;

import com.cwtw.rideflow.dto.AuthRequestDTO;
import com.cwtw.rideflow.dto.AuthResponseDTO;
import com.cwtw.rideflow.exception.CustomException;
import com.cwtw.rideflow.model.Customer;
import com.cwtw.rideflow.model.Dispatcher;
import com.cwtw.rideflow.model.User;
import com.cwtw.rideflow.repository.CustomerRepository;
import com.cwtw.rideflow.repository.DispatcherRepository;
import com.cwtw.rideflow.repository.UserRepository;
import com.cwtw.rideflow.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomerRepository customerRepository;
    private final DispatcherRepository dispatcherRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CustomerRepository customerRepository,
            DispatcherRepository dispatcherRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.customerRepository = customerRepository;
        this.dispatcherRepository = dispatcherRepository;
    }

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

        // Auto-create the linked profile for customer and dispatcher
        if ("ROLE_CUSTOMER".equals(role)) {
            Customer customer = Customer.builder()
                    .user(user)
                    .build();
            customerRepository.save(customer);
        } else if ("ROLE_DISPATCHER".equals(role)) {
            Dispatcher dispatcher = Dispatcher.builder()
                    .user(user)
                    .build();
            dispatcherRepository.save(dispatcher);
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponseDTO(token, user.getRole());
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
}

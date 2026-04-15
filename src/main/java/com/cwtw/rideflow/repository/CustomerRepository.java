package com.cwtw.rideflow.repository;

import com.cwtw.rideflow.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUserEmail(String email);

    Optional<Customer> findByUserId(Long userId);
}

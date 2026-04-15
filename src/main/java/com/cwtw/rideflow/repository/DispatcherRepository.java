package com.cwtw.rideflow.repository;

import com.cwtw.rideflow.model.Dispatcher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DispatcherRepository extends JpaRepository<Dispatcher, Long> {
    Optional<Dispatcher> findByUserEmail(String email);

    Optional<Dispatcher> findByUserId(Long userId);
}

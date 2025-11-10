package com.smartretail.servicecustomer.repository;

import com.smartretail.servicecustomer.model.CustomerPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerPointRepository extends JpaRepository<CustomerPoint, Long> {
    Optional<CustomerPoint> findByCustomerId(Long customerId);
}



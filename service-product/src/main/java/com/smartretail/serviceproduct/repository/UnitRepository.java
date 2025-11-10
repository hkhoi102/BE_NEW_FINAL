package com.smartretail.serviceproduct.repository;

import com.smartretail.serviceproduct.model.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    Optional<Unit> findByName(String name);

    List<Unit> findByActiveTrue();

    boolean existsByName(String name);
}

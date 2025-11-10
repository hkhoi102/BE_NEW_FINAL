package com.smartretail.inventoryservice.repository;

import com.smartretail.inventoryservice.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByName(String name);

    boolean existsByName(String name);

    List<Warehouse> findByActiveTrue();

    @Query("SELECT w FROM Warehouse w WHERE w.active = true AND (w.name LIKE %:keyword% OR w.description LIKE %:keyword%)")
    List<Warehouse> searchActiveWarehouses(@Param("keyword") String keyword);

    boolean existsByNameAndIdNot(String name, Long id);
}

package com.smartretail.inventoryservice.repository;

import com.smartretail.inventoryservice.model.StockLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockLocationRepository extends JpaRepository<StockLocation, Long> {

    List<StockLocation> findByWarehouseIdAndActiveTrue(Long warehouseId);

    List<StockLocation> findByWarehouseId(Long warehouseId);

    @Query("SELECT sl FROM StockLocation sl WHERE sl.active = true AND sl.warehouse.id = :warehouseId AND (sl.name LIKE %:keyword% OR sl.description LIKE %:keyword%)")
    List<StockLocation> searchActiveLocationsInWarehouse(@Param("warehouseId") Long warehouseId, @Param("keyword") String keyword);

    @Query("SELECT sl FROM StockLocation sl JOIN sl.warehouse w WHERE sl.active = true AND w.active = true")
    List<StockLocation> findAllActiveLocationsWithActiveWarehouse();

    boolean existsByNameAndWarehouseIdAndIdNot(String name, Long warehouseId, Long id);
}

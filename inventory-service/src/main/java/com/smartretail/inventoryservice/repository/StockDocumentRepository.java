package com.smartretail.inventoryservice.repository;

import com.smartretail.inventoryservice.model.StockDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockDocumentRepository extends JpaRepository<StockDocument, Long> {
    List<StockDocument> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);
    List<StockDocument> findAllByOrderByCreatedAtDesc();
}



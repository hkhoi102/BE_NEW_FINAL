package com.smartretail.inventoryservice.repository;

import com.smartretail.inventoryservice.model.StockDocumentLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockDocumentLineRepository extends JpaRepository<StockDocumentLine, Long> {
    List<StockDocumentLine> findByDocument_Id(Long documentId);
}



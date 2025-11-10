package com.smartretail.inventoryservice.dto;

import java.time.LocalDateTime;
import java.util.List;

public class StockDocumentDto {

    public Long id;
    public String type;        // INBOUND, OUTBOUND, TRANSFER, ADJUSTMENT
    public String status;      // DRAFT, APPROVED, CANCELLED
    public Long warehouseId;
    public Long stockLocationId;
    public String referenceNumber;
    public String note;
    public LocalDateTime createdAt;
    public LocalDateTime approvedAt;
    public List<Line> lines;

    public static class Line {
        public Long id;
        public Long productUnitId;
        public Integer quantity;
        // Optional lot fields for INBOUND
        public String lotNumber;
        public java.time.LocalDate expiryDate;
        public java.time.LocalDate manufacturingDate;
        public String supplierName;
        public String supplierBatchNumber;
    }
}



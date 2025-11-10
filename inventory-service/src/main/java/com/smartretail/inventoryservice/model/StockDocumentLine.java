package com.smartretail.inventoryservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "stock_document_lines")
public class StockDocumentLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private StockDocument document;

    @Column(nullable = false)
    private Long productUnitId;

    @Column(nullable = false)
    private Integer quantity; // as requested quantity in unit of productUnit

    // Optional lot fields for INBOUND documents
    private String lotNumber;
    private java.time.LocalDate expiryDate;
    private java.time.LocalDate manufacturingDate;
    private String supplierName;
    private String supplierBatchNumber;

    // Reservation fields for OUTBOUND documents
    @Column(name = "reserved_lot_info", columnDefinition = "TEXT")
    private String reservedLotInfo; // JSON string containing lot reservations

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public StockDocument getDocument() { return document; }
    public void setDocument(StockDocument document) { this.document = document; }
    public Long getProductUnitId() { return productUnitId; }
    public void setProductUnitId(Long productUnitId) { this.productUnitId = productUnitId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }
    public java.time.LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(java.time.LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public java.time.LocalDate getManufacturingDate() { return manufacturingDate; }
    public void setManufacturingDate(java.time.LocalDate manufacturingDate) { this.manufacturingDate = manufacturingDate; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getSupplierBatchNumber() { return supplierBatchNumber; }
    public void setSupplierBatchNumber(String supplierBatchNumber) { this.supplierBatchNumber = supplierBatchNumber; }

    public String getReservedLotInfo() { return reservedLotInfo; }
    public void setReservedLotInfo(String reservedLotInfo) { this.reservedLotInfo = reservedLotInfo; }
}



package com.smartretail.serviceproduct.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "barcode_mapping", uniqueConstraints = {
        @UniqueConstraint(name = "uk_barcode_code", columnNames = {"code"})
})
public class BarcodeMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_unit_id", nullable = false)
    private ProductUnit productUnit;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "type", length = 16)
    private String type; // EAN13, UPC, QR...

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ProductUnit getProductUnit() { return productUnit; }
    public void setProductUnit(ProductUnit productUnit) { this.productUnit = productUnit; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}



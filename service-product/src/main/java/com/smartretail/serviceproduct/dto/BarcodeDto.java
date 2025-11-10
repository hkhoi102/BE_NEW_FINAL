package com.smartretail.serviceproduct.dto;

import java.time.Instant;

public class BarcodeDto {
    private Long id;
    private Long productUnitId;
    private String code;
    private String type;
    private Instant createdAt;

    public BarcodeDto() {}

    public BarcodeDto(Long id, Long productUnitId, String code, String type, Instant createdAt) {
        this.id = id;
        this.productUnitId = productUnitId;
        this.code = code;
        this.type = type;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductUnitId() {
        return productUnitId;
    }

    public void setProductUnitId(Long productUnitId) {
        this.productUnitId = productUnitId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

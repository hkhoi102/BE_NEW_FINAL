package com.smartretail.serviceproduct.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PriceListDto {

    private Long id;
    private Long productUnitId;
    private Long productId;
    private String productName;
    private Long unitId;
    private String unitName;
    private String unitCode; // ProductUnit.code (maSP theo đơn vị)
    private Long priceHeaderId;
    private BigDecimal price;
    // Removed timeStart/timeEnd from DTO
    private Boolean active;
    private LocalDateTime createdAt;

    // Constructors
    public PriceListDto() {}

    public PriceListDto(Long id, Long productUnitId, Long productId, String productName,
                       Long unitId, String unitName, String unitCode, Long priceHeaderId, BigDecimal price, LocalDateTime timeStart,
                       LocalDateTime timeEnd, Boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.productUnitId = productUnitId;
        this.productId = productId;
        this.productName = productName;
        this.unitId = unitId;
        this.unitName = unitName;
        this.unitCode = unitCode;
        this.priceHeaderId = priceHeaderId;
        this.price = price;
        // keep constructor signature for compatibility, fields removed
        this.active = active;
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

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }

    public Long getPriceHeaderId() { return priceHeaderId; }
    public void setPriceHeaderId(Long priceHeaderId) { this.priceHeaderId = priceHeaderId; }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    // timeStart/timeEnd removed

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "PriceListDto{" +
                "id=" + id +
                ", productUnitId=" + productUnitId +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", unitId=" + unitId +
                ", unitName='" + unitName + '\'' +
                ", priceHeaderId=" + priceHeaderId +
                ", price=" + price +

                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}

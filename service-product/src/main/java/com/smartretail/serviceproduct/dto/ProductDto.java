package com.smartretail.serviceproduct.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProductDto {

    private Long id;
    private String name;
    private String description;
    private String code; // MaSP (tùy chọn khi tạo)
    private LocalDate expirationDate;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean active;

    // Optional import quantity for inbound after creation/upsert
    private Integer quantity;

    // ID của đơn vị tính cơ bản (từ danh sách Units)
    private Long defaultUnitId;

    // Thông tin giá và đơn vị
    private java.util.List<com.smartretail.serviceproduct.dto.ProductUnitInfo> productUnits;

    // Optional: barcodes to attach to default ProductUnit on creation
    public static class BarcodeInput {
        public String code;
        public String type; // EAN13, UPC, QR...
    }
    private java.util.List<BarcodeInput> barcodes;

    // Output: list of existing barcodes for this product (aggregated from its units)
    private java.util.List<com.smartretail.serviceproduct.dto.BarcodeDto> barcodeList;

    // Constructors
    public ProductDto() {}

    public ProductDto(Long id, String name, String description,
                     LocalDate expirationDate, Long categoryId, String categoryName,
                     LocalDateTime createdAt, LocalDateTime updatedAt, Boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.expirationDate = expirationDate;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.active = active;
        this.productUnits = new java.util.ArrayList<>();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }


    public Long getDefaultUnitId() {
        return defaultUnitId;
    }

    public void setDefaultUnitId(Long defaultUnitId) {
        this.defaultUnitId = defaultUnitId;
    }

    public java.util.List<com.smartretail.serviceproduct.dto.ProductUnitInfo> getProductUnits() {
        return productUnits;
    }

    public void setProductUnits(java.util.List<com.smartretail.serviceproduct.dto.ProductUnitInfo> productUnits) {
        this.productUnits = productUnits;
    }

    public java.util.List<BarcodeInput> getBarcodes() { return barcodes; }
    public void setBarcodes(java.util.List<BarcodeInput> barcodes) { this.barcodes = barcodes; }

    public java.util.List<com.smartretail.serviceproduct.dto.BarcodeDto> getBarcodeList() { return barcodeList; }
    public void setBarcodeList(java.util.List<com.smartretail.serviceproduct.dto.BarcodeDto> barcodeList) { this.barcodeList = barcodeList; }
}

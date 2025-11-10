package com.smartretail.serviceproduct.dto;

import java.time.LocalDateTime;

public class PriceHeaderDto {

    private Long id;
    private Long productUnitId;
    private String productName;
    private Long unitId;
    private String unitName;
    private String name;
    private String description;
    private LocalDateTime timeStart;
    private LocalDateTime timeEnd;
    private Boolean active;
    private LocalDateTime createdAt;

    public PriceHeaderDto() {}

    public PriceHeaderDto(Long id, Long productUnitId, String productName, Long unitId, String unitName,
                          String name, String description, LocalDateTime timeStart, LocalDateTime timeEnd,
                          Boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.productUnitId = productUnitId;
        this.productName = productName;
        this.unitId = unitId;
        this.unitName = unitName;
        this.name = name;
        this.description = description;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductUnitId() { return productUnitId; }
    public void setProductUnitId(Long productUnitId) { this.productUnitId = productUnitId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getTimeStart() { return timeStart; }
    public void setTimeStart(LocalDateTime timeStart) { this.timeStart = timeStart; }
    public LocalDateTime getTimeEnd() { return timeEnd; }
    public void setTimeEnd(LocalDateTime timeEnd) { this.timeEnd = timeEnd; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}



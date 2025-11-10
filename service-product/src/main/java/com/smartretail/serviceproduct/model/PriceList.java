package com.smartretail.serviceproduct.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_lists")
public class PriceList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_unit_id", nullable = false)
    private ProductUnit productUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_header_id")
    private PriceHeader priceHeader;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    // Removed timeStart/timeEnd to simplify pricing model

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public PriceList() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    public PriceList(ProductUnit productUnit, BigDecimal price, LocalDateTime timeStart) {
        this.productUnit = productUnit;
        this.price = price;
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProductUnit getProductUnit() {
        return productUnit;
    }

    public void setProductUnit(ProductUnit productUnit) {
        this.productUnit = productUnit;
    }

    public PriceHeader getPriceHeader() {
        return priceHeader;
    }

    public void setPriceHeader(PriceHeader priceHeader) {
        this.priceHeader = priceHeader;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }


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
}

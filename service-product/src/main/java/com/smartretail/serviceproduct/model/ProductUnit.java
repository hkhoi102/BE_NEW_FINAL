package com.smartretail.serviceproduct.model;

import jakarta.persistence.*;

@Entity
@Table(name = "product_units")
public class ProductUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_product_units_product"))
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_product_units_unit"))
    private Unit unit;

    @Column(name = "conversion_rate", nullable = false)
    private Integer conversionRate;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "image_url")
    private String imageUrl;

    // Constructors
    public ProductUnit() {
        this.active = true;
    }

    public ProductUnit(Product product, Unit unit, Integer conversionRate) {
        this.product = product;
        this.unit = unit;
        this.conversionRate = conversionRate;
        this.isDefault = false;
        this.active = true;
    }

    public ProductUnit(Product product, Unit unit, Integer conversionRate, Boolean isDefault) {
        this.product = product;
        this.unit = unit;
        this.conversionRate = conversionRate;
        this.isDefault = isDefault;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public Integer getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(Integer conversionRate) {
        this.conversionRate = conversionRate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}

package com.smartretail.serviceproduct.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductUnitInfo {

    private Long id;
    private Long unitId;
    private String unitName;
    private String unitDescription;
    private BigDecimal conversionRate;
    private BigDecimal currentPrice;
    private LocalDateTime priceValidFrom;
    private LocalDateTime priceValidTo;
    private Boolean isDefault;
    private Boolean active;

    // Giá chuyển đổi về đơn vị cơ bản (Lon)
    private BigDecimal convertedPrice;

    // Tồn kho
    private Integer quantity;           // Tổng tồn
    private Integer availableQuantity;  // Có thể bán

    // Ảnh sản phẩm
    private String imageUrl;

    // Constructors
    public ProductUnitInfo() {}

    public ProductUnitInfo(Long id, Long unitId, String unitName, String unitDescription,
                          BigDecimal conversionRate, BigDecimal currentPrice,
                          LocalDateTime priceValidFrom, LocalDateTime priceValidTo, Boolean isDefault) {
        this.id = id;
        this.unitId = unitId;
        this.unitName = unitName;
        this.unitDescription = unitDescription;
        this.conversionRate = conversionRate;
        this.currentPrice = currentPrice;
        this.priceValidFrom = priceValidFrom;
        this.priceValidTo = priceValidTo;
        this.isDefault = isDefault;
        this.active = true; // Mặc định active = true

        // Tự động tính giá chuyển đổi về đơn vị cơ bản
        this.convertedPrice = calculateConvertedPrice(currentPrice, conversionRate);
    }

    public ProductUnitInfo(Long id, Long unitId, String unitName, String unitDescription,
                          BigDecimal conversionRate, BigDecimal currentPrice, Boolean isDefault, Boolean active) {
        this.id = id;
        this.unitId = unitId;
        this.unitName = unitName;
        this.unitDescription = unitDescription;
        this.conversionRate = conversionRate;
        this.currentPrice = currentPrice;
        this.isDefault = isDefault;
        this.active = active;

        // Tự động tính giá chuyển đổi về đơn vị cơ bản
        this.convertedPrice = calculateConvertedPrice(currentPrice, conversionRate);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getUnitDescription() {
        return unitDescription;
    }

    public void setUnitDescription(String unitDescription) {
        this.unitDescription = unitDescription;
    }

    public BigDecimal getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(BigDecimal conversionRate) {
        this.conversionRate = conversionRate;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public LocalDateTime getPriceValidFrom() {
        return priceValidFrom;
    }

    public void setPriceValidFrom(LocalDateTime priceValidFrom) {
        this.priceValidFrom = priceValidFrom;
    }

    public LocalDateTime getPriceValidTo() {
        return priceValidTo;
    }

    public void setPriceValidTo(LocalDateTime priceValidTo) {
        this.priceValidTo = priceValidTo;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public BigDecimal getConvertedPrice() {
        return convertedPrice;
    }

    public void setConvertedPrice(BigDecimal convertedPrice) {
        this.convertedPrice = convertedPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

        // Tính giá chuyển đổi về đơn vị cơ bản (Lon)
    private BigDecimal calculateConvertedPrice(BigDecimal currentPrice, BigDecimal conversionRate) {
        if (currentPrice == null || conversionRate == null || conversionRate.compareTo(BigDecimal.ZERO) == 0) {
            return currentPrice;
        }

        // Giá chuyển đổi = Giá hiện tại / Conversion Rate
        // Ví dụ: 35000 VND/thùng ÷ 24 = 1458.33 VND/lon (giá thực tế)
        // So với mua từng lon: 1500 VND/lon
        // => Mua thùng rẻ hơn: 1500 - 1458.33 = 41.67 VND/lon
        return currentPrice.divide(conversionRate, 2, java.math.RoundingMode.HALF_UP);
    }
}

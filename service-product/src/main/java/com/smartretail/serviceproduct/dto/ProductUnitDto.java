package com.smartretail.serviceproduct.dto;


public class ProductUnitDto {

    private Long id;
    private Long productId;
    private String productName;
    private Long unitId;
    private String unitName;
    private String unitDescription;
    private Integer conversionRate;
    private Boolean isDefault;
    private Boolean active;
    private String imageUrl;

    // Constructors
    public ProductUnitDto() {}

    public ProductUnitDto(Long productId, Long unitId, Integer conversionRate, Boolean isDefault) {
        this.productId = productId;
        this.unitId = unitId;
        this.conversionRate = conversionRate;
        this.isDefault = isDefault;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }

    public String getUnitDescription() { return unitDescription; }
    public void setUnitDescription(String unitDescription) { this.unitDescription = unitDescription; }

    public Integer getConversionRate() { return conversionRate; }
    public void setConversionRate(Integer conversionRate) { this.conversionRate = conversionRate; }


    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}

package com.smartretail.inventoryservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "service-product", url = "${service.product.url:http://localhost:8085}")
public interface ProductServiceClient {

    @GetMapping("/api/products/units/{id}")
    ProductUnitResponse getProductUnitById(@PathVariable("id") Long id);

    @GetMapping("/api/products/units")
    ProductUnitResponse getProductUnitByProductAndUnit(
        @RequestParam("productId") Long productId,
        @RequestParam("unitId") Long unitId
    );

    // Lấy danh sách đơn vị theo sản phẩm (để tìm đơn vị mặc định)
    @GetMapping("/api/products/units/list")
    java.util.List<ProductUnitResponse> getProductUnitsByProductId(@RequestParam("productId") Long productId);

    // DTO class for response
    class ProductUnitResponse {
        private Long id;
        private Long productId;
        private Long unitId;
        private Double conversionRate;
        private Boolean isDefault;
        private Boolean active;
        private String unitName; // optional if API provides
        private String productName; // optional if API provides
        private Double unitPrice; // giá của đơn vị này

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Long getUnitId() { return unitId; }
        public void setUnitId(Long unitId) { this.unitId = unitId; }

        public Double getConversionRate() { return conversionRate; }
        public void setConversionRate(Double conversionRate) { this.conversionRate = conversionRate; }

        public Boolean getIsDefault() { return isDefault; }
        public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        public String getUnitName() { return unitName; }
        public void setUnitName(String unitName) { this.unitName = unitName; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }
    }

    @GetMapping("/api/uoms/{id}")
    UnitResponse getUnitById(@PathVariable("id") Long id);

    class UnitResponse {
        private Long id;
        private String name;
        private String description;
        private Boolean isDefault;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Boolean getIsDefault() { return isDefault; }
        public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    }

    @GetMapping("/api/products/{id}")
    ProductResponse getProductById(@PathVariable("id") Long id);

    class ProductResponse {
        private Long id;
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // Lấy giá hiện tại của product unit
    @GetMapping("/api/products/{productId}/prices/current")
    PriceResponse getCurrentPrice(@PathVariable("productId") Long productId, @RequestParam("productUnitId") Long productUnitId);

    class PriceResponse {
        private Boolean success;
        private java.math.BigDecimal data;
        private String message;

        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public java.math.BigDecimal getData() { return data; }
        public void setData(java.math.BigDecimal data) { this.data = data; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

}

package com.smartretail.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockSummaryDto {
    private Long productUnitId;
    private Long productId;
    private String productName;
    private String unitName;
    private Double unitPrice;
    private Long totalQuantity;
    private Long availableQuantity;
    private Long reservedQuantity;
    private Double totalValue;
    private Double availableValue;
}



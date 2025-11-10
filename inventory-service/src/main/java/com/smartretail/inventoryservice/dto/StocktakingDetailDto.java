package com.smartretail.inventoryservice.dto;

import lombok.Data;

@Data
public class StocktakingDetailDto {
    private Long id;
    private Long productUnitId;
    private Integer systemQuantity;
    private Integer actualQuantity;
    private Integer differenceQuantity;
    private String note;
    private String unitName;
    private String productName;
}



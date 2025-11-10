package com.smartretail.promotionservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDetailDto {
    private Long id;
    private Long promotionLineId;
    private Float discountPercent;
    private BigDecimal discountAmount;
    private Integer conditionQuantity;
    private Integer freeQuantity;
    private Long conditionProductUnitId;
    private Long giftProductUnitId;
    private BigDecimal minAmount;
    private BigDecimal maxDiscount;
    private Boolean active;
}

package com.smartretail.orderservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnDetailDto {

    private Long id;
    private Long returnOrderId;
    private Long orderDetailId;
    private Integer quantity;
    private BigDecimal refundAmount;

    // DTO cho tạo return detail mới
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateReturnDetailRequest {
        private Long orderDetailId;
        private Integer quantity;
    }

    // DTO cho response với thông tin sản phẩm
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnDetailResponse {
        private Long id;
        private Long returnOrderId;
        private Long orderDetailId;
        private Long productUnitId;
        private String productName;
        private String unitName;
        private String productImageUrl;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal refundAmount;
        private Integer originalQuantity;
        private Integer maxReturnQuantity;
    }
}

package com.smartretail.orderservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailDto {

    private Long id;
    private Long orderId;
    private Long productUnitId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    // DTO cho tạo order detail mới
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderDetailRequest {
        private Long productUnitId;
        private Integer quantity;
        private BigDecimal unitPrice;
    }

    // DTO cho cập nhật số lượng
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateQuantityRequest {
        private Integer quantity;
    }

    // DTO cho response với thông tin sản phẩm
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDetailResponse {
        private Long id;
        private Long orderId;
        private Long productUnitId;
        private String productName;
        private String unitName;
        private String productImageUrl;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private boolean canReturn;
        private Integer maxReturnQuantity;
    }

    // DTO cho thêm sản phẩm vào đơn hàng
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddProductRequest {
        private Long productUnitId;
        private Integer quantity;
    }
}

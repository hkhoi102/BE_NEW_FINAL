package com.smartretail.orderservice.dto;

import com.smartretail.orderservice.model.Order;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    private Long id;
    private Long customerId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private Order.OrderStatus status;
    private Order.PaymentMethod paymentMethod;
    private Order.PaymentStatus paymentStatus;
    private Long promotionAppliedId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderDetailDto> orderDetails;

    // DTO cho tạo đơn hàng mới (customerId sẽ được lấy từ JWT token)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderRequest {
        private List<OrderDetailDto.CreateOrderDetailRequest> orderDetails;
        private Long promotionAppliedId;
        private Order.PaymentMethod paymentMethod; // optional, default COD
        private String shippingAddress; // địa chỉ giao hàng từ giao diện
        private Order.DeliveryMethod deliveryMethod; // cách thức nhận hàng: PICKUP_AT_STORE hoặc HOME_DELIVERY
        private String phoneNumber; // số điện thoại khách hàng
    }

    // DTO cho preview giỏ hàng (không lưu DB)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreviewRequest {
        private List<OrderDetailDto.CreateOrderDetailRequest> orderDetails;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreviewResponse {
        private BigDecimal totalOriginalAmount; // tổng trước khuyến mãi
        private BigDecimal totalDiscountAmount; // tổng giảm giá
        private BigDecimal totalFinalAmount;    // tổng sau khuyến mãi
        private List<String> appliedPromotions; // tên các khuyến mãi áp dụng (nếu có)
        private List<GiftItem> giftItems;       // sản phẩm được tặng (mới)
    }

    // DTO cho sản phẩm được tặng
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GiftItem {
        private Long productUnitId;      // ID sản phẩm được tặng
        private String productName;      // Tên sản phẩm
        private String unitName;         // Đơn vị (cái, kg, lít...)
        private Integer quantity;        // Số lượng được tặng
        private BigDecimal unitPrice;    // Giá = 0 (vì là quà tặng)
        private BigDecimal subtotal;     // Thành tiền = 0
        private String promotionName;    // Tên khuyến mãi áp dụng
    }

    // DTO cho cập nhật trạng thái đơn hàng
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        private Order.OrderStatus status;
        private String note;
        private Long warehouseId;  // Optional: warehouse for inventory management
        private Long stockLocationId;  // Optional: stock location for inventory management
    }

    // DTO cho response với thông tin chi tiết
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderResponse {
        private String orderCode;
        private Long id;
        private Long customerId;
        private String customerName;
        private String customerPhone;
        private String customerEmail;
        private String shippingAddress;
        private Order.DeliveryMethod deliveryMethod;
        private String phoneNumber;
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private Order.OrderStatus status;
        private Order.PaymentMethod paymentMethod;
        private Order.PaymentStatus paymentStatus;
        private Long promotionAppliedId;
        private String promotionName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<OrderDetailDto.OrderDetailResponse> orderDetails;
        private boolean canCancel;
        private boolean canReturn;
        private PaymentInfo paymentInfo; // for BANK_TRANSFER
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfo {
        private String qrContent;
        private String accountNumber;
        private String accountName;
        private String bankCode;
        private String transferContent;
        private String referenceId;
    }

    // DTO cho danh sách đơn hàng (thông tin tóm tắt)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSummary {
        private String orderCode;
        private Long id;
        private Long customerId;
        private String customerName;
        private BigDecimal totalAmount;
        private Order.OrderStatus status;
        private Order.PaymentMethod paymentMethod;
        private Order.PaymentStatus paymentStatus;
        private String shippingAddress;
        private LocalDateTime createdAt;
        private int itemCount;
        private boolean canCancel;
    }
}

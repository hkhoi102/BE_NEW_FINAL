package com.smartretail.orderservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@Column(name = "order_code", unique = true, length = 50)
	private String orderCode;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "promotion_applied_id")
    private Long promotionAppliedId;

    // Warehouse and stock location for inventory management
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "stock_location_id")
    private Long stockLocationId;

    // Outbound document ID for inventory management
    @Column(name = "outbound_document_id")
    private Long outboundDocumentId;

    // Shipping address snapshot at order time
    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    // Delivery method: PICKUP_AT_STORE or HOME_DELIVERY
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false)
    private DeliveryMethod deliveryMethod = DeliveryMethod.PICKUP_AT_STORE;

    // Customer phone number for delivery
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderDetail> orderDetails;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReturnOrder> returnOrders;

    public enum OrderStatus {
        PENDING,        // Chờ xử lý
        CONFIRMED,      // Đã xác nhận
        DELIVERING,     // Đang giao hàng
        COMPLETED,      // Hoàn thành
        CANCELLED       // Đã hủy
    }

    // New: payment method selection and status
    public enum PaymentMethod {
        COD,
        BANK_TRANSFER
    }

    public enum PaymentStatus {
        UNPAID,
        PAID
    }

    public enum DeliveryMethod {
        PICKUP_AT_STORE,    // Nhận tại quầy
        HOME_DELIVERY        // Giao hàng tận nhà
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

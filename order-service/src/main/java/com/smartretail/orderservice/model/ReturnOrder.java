package com.smartretail.orderservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "return_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnStatus status = ReturnStatus.REQUESTED;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @OneToMany(mappedBy = "returnOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReturnDetail> returnDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "return_code", unique = true, nullable = false)
    private String returnCode;

    // Constructor để tạo ReturnOrder với Order object
    public ReturnOrder(Order order, Long customerId, String reason) {
        this.order = order;
        this.customerId = customerId;
        this.reason = reason;
        this.status = ReturnStatus.REQUESTED;
        this.createdAt = LocalDateTime.now();
    }

    // Getter để lấy orderId từ Order object
    public Long getOrderId() {
        return order != null ? order.getId() : null;
    }

    public enum ReturnStatus {
        REQUESTED,      // Khách yêu cầu trả hàng
        APPROVED,       // Đã duyệt
        REJECTED,       // Từ chối
        COMPLETED       // Hoàn thành trả hàng
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (status == ReturnStatus.APPROVED || status == ReturnStatus.REJECTED || status == ReturnStatus.COMPLETED) {
            processedAt = LocalDateTime.now();
        }
    }
}

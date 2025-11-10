package com.smartretail.orderservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "return_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_order_id", nullable = false)
    private Long returnOrderId;

    @Column(name = "order_detail_id", nullable = false)
    private Long orderDetailId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_order_id", insertable = false, updatable = false)
    private ReturnOrder returnOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_detail_id", insertable = false, updatable = false)
    private OrderDetail orderDetail;

    // Constructor để tính refund amount tự động
    public ReturnDetail(Long returnOrderId, Long orderDetailId, Integer quantity, BigDecimal unitPrice) {
        this.returnOrderId = returnOrderId;
        this.orderDetailId = orderDetailId;
        this.quantity = quantity;
        this.refundAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}

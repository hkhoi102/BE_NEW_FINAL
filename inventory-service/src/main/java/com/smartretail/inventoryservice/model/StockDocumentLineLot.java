package com.smartretail.inventoryservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_document_line_lots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDocumentLineLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private StockLot lot;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "note")
    private String note;

    public enum TransactionType {
        INBOUND,    // Nhập kho
        OUTBOUND,   // Xuất kho
        TRANSFER,   // Chuyển kho
        ADJUSTMENT, // Điều chỉnh
        RETURN      // Trả hàng
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}

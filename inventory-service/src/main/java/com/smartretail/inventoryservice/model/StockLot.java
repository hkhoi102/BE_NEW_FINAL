package com.smartretail.inventoryservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_lots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lot_number", nullable = false, unique = true)
    private String lotNumber;

    @Column(name = "product_unit_id", nullable = false)
    private Long productUnitId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "stock_location_id", nullable = false)
    private Long stockLocationId;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "supplier_batch_number")
    private String supplierBatchNumber;

    @Column(name = "initial_quantity", nullable = false)
    private Integer initialQuantity;

    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LotStatus status = LotStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_by_username")
    private String createdByUsername;

    @Column(name = "note")
    private String note;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.availableQuantity = this.currentQuantity - this.reservedQuantity;
    }

    @PrePersist
    public void prePersist() {
        this.availableQuantity = this.currentQuantity - this.reservedQuantity;
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public enum LotStatus {
        ACTIVE,        // Lô đang hoạt động
        EXPIRED,       // Lô đã hết hạn
        DEPLETED,      // Lô đã hết hàng
        QUARANTINE,    // Lô bị cách ly
        CANCELLED      // Lô bị hủy
    }

    // Helper methods
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public boolean isNearExpiry(int days) {
        return expiryDate != null &&
               expiryDate.isBefore(LocalDate.now().plusDays(days)) &&
               !isExpired();
    }

    public boolean hasAvailableQuantity() {
        return availableQuantity > 0;
    }

    public void reserveQuantity(int quantity) {
        if (availableQuantity < quantity) {
            throw new RuntimeException("Not enough available quantity in lot " + lotNumber);
        }
        this.reservedQuantity += quantity;
        this.availableQuantity = this.currentQuantity - this.reservedQuantity;
    }

    public void releaseReservation(int quantity) {
        if (this.reservedQuantity < quantity) {
            throw new RuntimeException("Cannot release more than reserved quantity");
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity = this.currentQuantity - this.reservedQuantity;
    }

    public void consumeQuantity(int quantity) {
        if (this.currentQuantity < quantity) {
            throw new RuntimeException("Not enough current quantity in lot " + lotNumber);
        }
        this.currentQuantity -= quantity;
        this.availableQuantity = this.currentQuantity - this.reservedQuantity;

        if (this.currentQuantity <= 0) {
            this.status = LotStatus.DEPLETED;
        }
    }
}

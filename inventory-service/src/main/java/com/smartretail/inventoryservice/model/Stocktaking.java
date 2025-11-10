package com.smartretail.inventoryservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "stocktaking")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stocktaking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stocktaking_number", nullable = false, unique = true)
    private String stocktakingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_location_id")
    private StockLocation stockLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StocktakingStatus status;

    @Column(name = "stocktaking_date", nullable = false)
    private LocalDateTime stocktakingDate;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Column(length = 500)
    private String note;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_by_username", length = 255)
    private String createdByUsername;

    // @OneToMany(mappedBy = "stocktaking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<StocktakingDetail> stocktakingDetails;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = StocktakingStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum StocktakingStatus {
        PENDING, IN_PROGRESS, COMPLETED, CONFIRMED, CANCELLED
    }
}

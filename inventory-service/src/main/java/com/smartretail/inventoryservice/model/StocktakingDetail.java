package com.smartretail.inventoryservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Entity
@Table(name = "stocktaking_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StocktakingDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stocktaking_id", nullable = false)
    private Stocktaking stocktaking;

    @Column(name = "product_unit_id", nullable = false)
    private Long productUnitId;

    @Column(name = "system_quantity", nullable = false)
    private Integer systemQuantity;

    @Column(name = "actual_quantity")
    private Integer actualQuantity;

    @Column(name = "difference_quantity")
    private Integer differenceQuantity;

    @Column(length = 200)
    private String note;

    @PrePersist
    @PreUpdate
    protected void calculateDifference() {
        if (actualQuantity != null && systemQuantity != null) {
            differenceQuantity = actualQuantity - systemQuantity;
        }
    }
}

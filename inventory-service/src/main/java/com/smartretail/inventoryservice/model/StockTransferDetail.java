package com.smartretail.inventoryservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Entity
@Table(name = "stock_transfer_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockTransferDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_transfer_id", nullable = false)
    private StockTransfer stockTransfer;

    @Column(name = "product_unit_id", nullable = false)
    private Long productUnitId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "transferred_quantity")
    private Integer transferredQuantity = 0;

    @Column(length = 200)
    private String note;
}

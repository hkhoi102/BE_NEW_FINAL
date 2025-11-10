package com.smartretail.inventoryservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import com.smartretail.inventoryservice.model.Inventory;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDto {

    private Long id;

    @NotNull(message = "Transaction type is required")
    private Inventory.TransactionType transactionType;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantity;

    @NotNull(message = "Transaction date is required")
    private LocalDateTime transactionDate;

    @NotBlank(message = "Note is required")
    private String note;

    @NotBlank(message = "Reference number is required")
    private String referenceNumber;

    @NotNull(message = "Product unit ID is required")
    private Long productUnitId;

    @NotNull(message = "Stock location ID is required")
    private Long stockLocationId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional fields for business operations
    private String productName;
    private String unitName;
    private String warehouseName;
    private String stockLocationName;

    // Stock lot tracking
    private Long stockLotId;  // ID of the stock lot used for this transaction

    // Optional lot fields for auto-create/merge lot on inbound
    // When provided on IMPORT transactions, system will create or update a stock lot accordingly
    private String lotNumber;            // optional: specific lot/batch number
    private LocalDate expiryDate;        // optional: expiry date for the lot
    private LocalDate manufacturingDate; // optional: mfg date
    private String supplierName;         // optional: supplier info
    private String supplierBatchNumber;  // optional: supplier batch

}

package com.smartretail.inventoryservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentDto {

    @NotNull(message = "Product unit ID is required")
    private Long productUnitId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Stock location ID is required")
    private Long stockLocationId;

    @NotNull(message = "New quantity is required")
    private Integer newQuantity;

    @NotBlank(message = "Adjustment reason is required")
    private String reason;

    @NotBlank(message = "Reference number is required")
    private String referenceNumber;

    private String note;

    @NotNull(message = "Adjustment date is required")
    private LocalDateTime adjustmentDate = LocalDateTime.now();

    // Additional fields for business operations
    private Integer oldQuantity;
    private Integer differenceQuantity;

    // Calculated field
    public Integer getDifferenceQuantity() {
        if (oldQuantity != null && newQuantity != null) {
            differenceQuantity = newQuantity - oldQuantity;
        }
        return differenceQuantity;
    }
}

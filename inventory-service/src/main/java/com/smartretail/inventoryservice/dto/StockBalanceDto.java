package com.smartretail.inventoryservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockBalanceDto {

    private Long id;

    @NotNull(message = "Product unit ID is required")
    private Long productUnitId;

    @NotNull(message = "Stock location ID is required")
    private Long stockLocationId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @Min(value = 0, message = "Reserved quantity cannot be negative")
    private Integer reservedQuantity = 0;

    @Min(value = 0, message = "Available quantity cannot be negative")
    private Integer availableQuantity;

    private LocalDateTime lastUpdatedAt;
    private LocalDateTime createdAt;

    // Additional fields for business operations
    private String productName;
    private String unitName;
    private String warehouseName;
    private String stockLocationName;

    // Calculated field
    public Integer getAvailableQuantity() {
        if (availableQuantity == null) {
            availableQuantity = quantity - reservedQuantity;
        }
        return availableQuantity;
    }
}

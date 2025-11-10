package com.smartretail.inventoryservice.dto;

import com.smartretail.inventoryservice.model.StockLot;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockLotDto {

    private Long id;

    @NotBlank(message = "Lot number is required")
    private String lotNumber;

    @NotNull(message = "Product unit ID is required")
    private Long productUnitId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Stock location ID is required")
    private Long stockLocationId;

    private LocalDate expiryDate;

    private LocalDate manufacturingDate;

    private String supplierName;

    private String supplierBatchNumber;

    @NotNull(message = "Initial quantity is required")
    @Min(value = 1, message = "Initial quantity must be greater than 0")
    private Integer initialQuantity;

    private Integer currentQuantity;

    private Integer reservedQuantity;

    private Integer availableQuantity;

    // Số lượng vừa được allocate cho request hiện tại
    private Integer allocatedQuantity;

    private StockLot.LotStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long createdBy;

    private String createdByUsername;

    private String note;

    // Additional fields for display
    private String productName;
    private String unitName;
    private String warehouseName;
    private String stockLocationName;

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
        return availableQuantity != null && availableQuantity > 0;
    }
}

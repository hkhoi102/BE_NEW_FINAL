package com.smartretail.inventoryservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryImportDto {

    private Long id;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Stock location ID is required")
    private Long stockLocationId;

    @Size(max = 100, message = "Reference number cannot exceed 100 characters")
    private String referenceNumber;

    @Size(max = 500, message = "Note cannot exceed 500 characters")
    private String note;

    @NotNull(message = "Transaction date is required")
    private LocalDateTime transactionDate;

    private List<InventoryImportDetailDto> importDetails;

    // Additional fields for response
    private String warehouseName;
    private String stockLocationName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryImportDetailDto {

        @NotNull(message = "Product unit ID is required")
        private Long productUnitId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be greater than 0")
        private Integer quantity;

        @Size(max = 200, message = "Note cannot exceed 200 characters")
        private String note;

        // Additional fields for response
        private String productName;
        private String unitName;
    }
}

package com.smartretail.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDto {

    @NotNull
    private Long productUnitId;

    @NotNull
    private Long sourceWarehouseId;

    @NotNull
    private Long sourceStockLocationId;

    @NotNull
    private Long destinationWarehouseId;

    @NotNull
    private Long destinationStockLocationId;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    private LocalDateTime transactionDate;

    @NotBlank
    private String referenceNumber;

    @NotBlank
    private String note;
}



package com.smartretail.inventoryservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StocktakingDto {
    private Long id;
    private String stocktakingNumber;
    private Long warehouseId;
    private Long stockLocationId;
    private String status;
    private LocalDateTime stocktakingDate;
    private LocalDateTime completedDate;
    private String note;
    private Long createdBy;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}



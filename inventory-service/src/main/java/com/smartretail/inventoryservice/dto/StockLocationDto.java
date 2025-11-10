package com.smartretail.inventoryservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockLocationDto {

    private Long id;

    @NotBlank(message = "Location name is required")
    @Size(max = 100, message = "Location name cannot exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @Size(max = 50, message = "Zone cannot exceed 50 characters")
    private String zone;

    @Size(max = 50, message = "Aisle cannot exceed 50 characters")
    private String aisle;

    @Size(max = 50, message = "Rack cannot exceed 50 characters")
    private String rack;

    @Size(max = 50, message = "Level cannot exceed 50 characters")
    private String level;

    @Size(max = 50, message = "Position cannot exceed 50 characters")
    private String position;

    private Boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional fields for response
    private String warehouseName;
}

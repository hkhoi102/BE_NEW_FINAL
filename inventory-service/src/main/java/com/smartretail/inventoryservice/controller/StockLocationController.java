package com.smartretail.inventoryservice.controller;

import com.smartretail.inventoryservice.dto.StockLocationDto;
import com.smartretail.inventoryservice.service.StockLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/stock-locations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StockLocationController {

    private final StockLocationService stockLocationService;

    @PostMapping
    public ResponseEntity<StockLocationDto> createStockLocation(@Valid @RequestBody StockLocationDto stockLocationDto) {
        StockLocationDto createdLocation = stockLocationService.createStockLocation(stockLocationDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLocation);
    }

    @GetMapping
    public ResponseEntity<List<StockLocationDto>> getAllStockLocations(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(defaultValue = "false") boolean includeInactive) {

        List<StockLocationDto> locations;
        if (warehouseId != null) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                locations = stockLocationService.searchStockLocationsInWarehouse(warehouseId, keyword.trim());
            } else if (includeInactive) {
                locations = stockLocationService.getAllStockLocationsByWarehouse(warehouseId);
            } else {
                locations = stockLocationService.getStockLocationsByWarehouse(warehouseId);
            }
        } else if (activeOnly) {
            locations = stockLocationService.getActiveStockLocations();
        } else {
            locations = stockLocationService.getAllStockLocations();
        }

        return ResponseEntity.ok(locations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockLocationDto> getStockLocationById(@PathVariable Long id) {
        return stockLocationService.getStockLocationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<StockLocationDto> updateStockLocation(
            @PathVariable Long id,
            @Valid @RequestBody StockLocationDto stockLocationDto) {
        try {
            StockLocationDto updatedLocation = stockLocationService.updateStockLocation(id, stockLocationDto);
            return ResponseEntity.ok(updatedLocation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStockLocation(@PathVariable Long id) {
        try {
            stockLocationService.deleteStockLocation(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<StockLocationDto> activateStockLocation(@PathVariable Long id) {
        try {
            StockLocationDto activatedLocation = stockLocationService.activateStockLocation(id);
            return ResponseEntity.ok(activatedLocation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<StockLocationDto> deactivateStockLocation(@PathVariable Long id) {
        try {
            StockLocationDto deactivatedLocation = stockLocationService.deactivateStockLocation(id);
            return ResponseEntity.ok(deactivatedLocation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

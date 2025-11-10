package com.smartretail.inventoryservice.controller;

import com.smartretail.inventoryservice.dto.WarehouseDto;
import com.smartretail.inventoryservice.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    public ResponseEntity<?> createWarehouse(@Valid @RequestBody WarehouseDto warehouseDto) {
        try {
            WarehouseDto createdWarehouse = warehouseService.createWarehouse(warehouseDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdWarehouse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<WarehouseDto>> getAllWarehouses(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean activeOnly) {

        List<WarehouseDto> warehouses;
        if (keyword != null && !keyword.trim().isEmpty()) {
            warehouses = warehouseService.searchWarehouses(keyword.trim());
        } else if (activeOnly) {
            warehouses = warehouseService.getActiveWarehouses();
        } else {
            warehouses = warehouseService.getAllWarehouses();
        }

        return ResponseEntity.ok(warehouses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseDto> getWarehouseById(@PathVariable Long id) {
        return warehouseService.getWarehouseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseDto warehouseDto) {
        try {
            WarehouseDto updatedWarehouse = warehouseService.updateWarehouse(id, warehouseDto);
            return ResponseEntity.ok(updatedWarehouse);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWarehouse(@PathVariable Long id) {
        try {
            warehouseService.deleteWarehouse(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

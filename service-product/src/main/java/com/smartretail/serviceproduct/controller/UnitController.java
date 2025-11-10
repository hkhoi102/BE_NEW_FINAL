package com.smartretail.serviceproduct.controller;

import com.smartretail.serviceproduct.dto.UnitDto;
import com.smartretail.serviceproduct.service.UnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/uoms")
@CrossOrigin(origins = "*")
public class UnitController {

    @Autowired
    private UnitService unitService;

    // POST /api/uoms - Create new unit
    @PostMapping
    public ResponseEntity<?> createUnit(@RequestBody UnitDto unitDto) {
        try {
            UnitDto createdUnit = unitService.createUnit(unitDto);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Unit created successfully");
            response.put("data", createdUnit);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error creating unit: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/uoms - Get all units
    @GetMapping
    public ResponseEntity<?> getAllUnits() {
        try {
            List<UnitDto> units = unitService.getAllUnits();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", units);
            response.put("total", units.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving units: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/uoms/{id} - Get unit by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getUnitById(@PathVariable Long id) {
        try {
            Optional<UnitDto> unit = unitService.getUnitById(id);
            if (unit.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", unit.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Unit not found with id: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving unit: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PUT /api/uoms/{id} - Update unit
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUnit(@PathVariable Long id, @RequestBody UnitDto unitDto) {
        try {
            Optional<UnitDto> updatedUnit = unitService.updateUnit(id, unitDto);
            if (updatedUnit.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Unit updated successfully");
                response.put("data", updatedUnit.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Unit not found with id: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating unit: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // DELETE /api/uoms/{id} - Delete unit (soft delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUnit(@PathVariable Long id) {
        try {
            boolean deleted = unitService.deleteUnit(id);
            if (deleted) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Unit deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Unit not found with id: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deleting unit: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

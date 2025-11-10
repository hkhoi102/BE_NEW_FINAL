package com.smartretail.serviceproduct.controller;

import com.smartretail.serviceproduct.dto.PriceListDto;
import com.smartretail.serviceproduct.service.PriceListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class PriceController {

    @Autowired
    private PriceListService priceListService;

    // POST /api/products/{id}/prices - Add new price
    @PostMapping("/{productId}/prices")
    public ResponseEntity<?> addPrice(@PathVariable Long productId, @RequestBody PriceListDto priceDto) {
        try {
            PriceListDto createdPrice = priceListService.createPrice(priceDto);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Price added successfully");
            response.put("data", createdPrice);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error adding price: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/products/{id}/prices - Get price history for product
    @GetMapping("/{productId}/prices")
    public ResponseEntity<?> getPriceHistory(@PathVariable Long productId) {
        try {
            List<PriceListDto> prices = priceListService.getPriceHistoryByProduct(productId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", prices);
            response.put("total", prices.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving price history: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/products/{id}/prices/current - Get current price for product
    @GetMapping("/{productId}/prices/current")
    public ResponseEntity<?> getCurrentPrice(@PathVariable Long productId, @RequestParam Long productUnitId) {
        try {
            Optional<BigDecimal> currentPrice = priceListService.getCurrentPrice(productUnitId);
            Map<String, Object> response = new HashMap<>();
            if (currentPrice.isPresent()) {
                response.put("success", true);
                response.put("data", currentPrice.get());
                response.put("message", "Current price retrieved successfully");
            } else {
                response.put("success", false);
                response.put("message", "No current price found for this product unit");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving current price: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PUT /api/products/{id}/prices/{priceId} - Update price
    @PutMapping("/{productId}/prices/{priceId}")
    public ResponseEntity<?> updatePrice(@PathVariable Long productId, @PathVariable Long priceId,
                                       @RequestBody PriceListDto priceDto) {
        try {
            Optional<PriceListDto> updatedPrice = priceListService.updatePrice(priceId, priceDto);
            if (updatedPrice.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Price updated successfully");
                response.put("data", updatedPrice.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Price not found with id: " + priceId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating price: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // DELETE /api/products/{id}/prices/{priceId} - Delete price
    @DeleteMapping("/{productId}/prices/{priceId}")
    public ResponseEntity<?> deletePrice(@PathVariable Long productId, @PathVariable Long priceId) {
        try {
            boolean deleted = priceListService.deletePrice(priceId);
            if (deleted) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Price deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Price not found with id: " + priceId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deleting price: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/products/{productId}/prices/units/{productUnitId} - Get prices by product unit
    @GetMapping("/{productId}/prices/units/{productUnitId}")
    public ResponseEntity<?> getPricesByProductUnit(@PathVariable Long productId, @PathVariable Long productUnitId) {
        try {
            List<PriceListDto> prices = priceListService.getPricesByProductUnit(productUnitId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", prices);
            response.put("total", prices.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving prices: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // POST /api/products/{productId}/prices/units/{productUnitId} - Create price for a product unit (with start/end)
    @PostMapping("/{productId}/prices/units/{productUnitId}")
    public ResponseEntity<?> addPriceForProductUnit(
            @PathVariable Long productId,
            @PathVariable Long productUnitId,
            @RequestBody PriceListDto priceDto) {
        try {
            // Override productUnitId from path to ensure consistency
            priceDto.setProductUnitId(productUnitId);
            PriceListDto createdPrice = priceListService.createPrice(priceDto);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Price added successfully");
            response.put("data", createdPrice);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error adding price: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/products/check-time-conflict - Kiểm tra xung đột thời gian giữa price headers
    @GetMapping("/check-time-conflict")
    public ResponseEntity<?> checkTimeConflict(
            @RequestParam Long productUnitId,
            @RequestParam Long priceHeaderId) {
        try {
            Map<String, Object> result = priceListService.checkTimeConflict(productUnitId, priceHeaderId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("hasConflict", true);
            response.put("message", "Lỗi khi kiểm tra: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

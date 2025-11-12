package com.smartretail.serviceproduct.controller;

import com.smartretail.serviceproduct.dto.PriceHeaderDto;
import com.smartretail.serviceproduct.dto.PriceListDto;
import com.smartretail.serviceproduct.service.PriceHeaderService;
import com.smartretail.serviceproduct.service.PriceListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class PriceHeaderController {

    @Autowired
    private PriceHeaderService priceHeaderService;

    @Autowired
    private PriceListService priceListService;
    // POST /api/products/price-headers - create a global price header
    @PostMapping("/price-headers")
    public ResponseEntity<?> createGlobalHeader(@RequestBody PriceHeaderDto dto) {
        try {
            PriceHeaderDto created = priceHeaderService.createGlobal(dto);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Price header created successfully");
            response.put("data", created);
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error creating header: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PUT /api/products/price-headers/{headerId} - update a price header
    @PutMapping("/price-headers/{headerId}")
    public ResponseEntity<?> updateHeader(@PathVariable Long headerId, @RequestBody PriceHeaderDto dto) {
        try {
            Optional<PriceHeaderDto> updated = priceHeaderService.updateHeader(headerId, dto);
            if (updated.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Price header updated successfully");
                response.put("data", updated.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Price header not found with id: " + headerId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating header: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/products/price-headers - list all headers (both active and inactive)
    @GetMapping("/price-headers")
    public ResponseEntity<?> listAllHeaders() {
        try {
            List<PriceHeaderDto> list = priceHeaderService.listAllHeaders();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", list);
            response.put("total", list.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving headers: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // POST /api/products/price-headers/{headerId}/prices/bulk - create prices in bulk for a header
    @PostMapping("/price-headers/{headerId}/prices/bulk")
    public ResponseEntity<?> createPricesBulk(@PathVariable Long headerId, @RequestBody java.util.List<PriceListDto> items) {
        try {
            java.util.List<PriceListDto> created = priceListService.createPricesBulk(headerId, items);
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "Prices created successfully");
            response.put("data", created);
            response.put("total", created.size());
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "Error creating prices: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/products/price-headers/{headerId} - get header details with its items
    @GetMapping("/price-headers/{headerId}")
    public ResponseEntity<?> getHeaderDetails(@PathVariable Long headerId) {
        try {
            var result = priceHeaderService.getHeaderWithItems(headerId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/products/price-headers/{headerId}/check-products - check which products already have prices in this header
    @GetMapping("/price-headers/{headerId}/check-products")
    public ResponseEntity<?> checkProductsInHeader(@PathVariable Long headerId) {
        try {
            var result = priceHeaderService.checkProductsInHeader(headerId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PUT /api/products/price-headers/{headerId}/deactivate - deactivate price header
    @PutMapping("/price-headers/{headerId}/deactivate")
    public ResponseEntity<?> deactivateHeader(@PathVariable Long headerId) {
        try {
            boolean success = priceHeaderService.deactivateHeader(headerId);
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Price header deactivated successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Price header not found with id: " + headerId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deactivating header: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PUT /api/products/price-headers/{headerId}/activate - activate price header
    @PutMapping("/price-headers/{headerId}/activate")
    public ResponseEntity<?> activateHeader(@PathVariable Long headerId) {
        try {
            boolean success = priceHeaderService.activateHeader(headerId);
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Price header activated successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Price header not found with id: " + headerId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error activating header: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}



package com.smartretail.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@FeignClient(name = "inventory-service", url = "${inventory.service.url:http://api-gateway:8085}")
public interface InventoryServiceClient {

    @GetMapping("/api/inventory/stock/product-unit/{productUnitId}")
    Map<String, Object> getStockByProductUnit(@PathVariable("productUnitId") Long productUnitId, @RequestHeader("Authorization") String token);

    @PostMapping("/api/inventory/outbound")
    Map<String, Object> createOutboundInventory(@RequestBody Map<String, Object> request, @RequestHeader("Authorization") String token);

    @PostMapping("/api/inventory/outbound/bulk")
    Map<String, Object> createBulkOutboundInventory(@RequestBody List<Map<String, Object>> requests, @RequestHeader("Authorization") String token);

    // FEFO outbound endpoints
    @PostMapping("/api/inventory/outbound/bulk/fefo")
    Map<String, Object> createBulkOutboundInventoryFefo(@RequestBody java.util.List<java.util.Map<String, Object>> requests,
                                                        @RequestHeader("Authorization") String token);

    @PostMapping("/api/inventory/inbound/process")
    Map<String, Object> createInboundInventory(@RequestBody Map<String, Object> request, @RequestHeader("Authorization") String token);

    @PostMapping("/api/inventory/inbound/bulk")
    Map<String, Object> createBulkInboundInventory(@RequestBody java.util.List<java.util.Map<String, Object>> requests, @RequestHeader("Authorization") String token);

    @GetMapping("/api/inventory/stock")
    Map<String, Object> getStockBalance(@RequestHeader("Authorization") String token);

    // Get stock balances for a specific product unit (list by locations/warehouses)
    @GetMapping("/api/inventory/stock/{productUnitId}")
    List<Map<String, Object>> getStockBalanceByProduct(@PathVariable("productUnitId") Long productUnitId,
                                                       @RequestHeader("Authorization") String token);

    // Stock document APIs
    @PostMapping("/api/inventory/documents")
    Map<String, Object> createStockDocument(@RequestBody Map<String, Object> request, @RequestHeader("Authorization") String token);

    @PostMapping("/api/inventory/documents/{id}/lines/bulk")
    Map<String, Object> addDocumentLinesBulk(@PathVariable("id") Long id, @RequestBody Map<String, Object> request, @RequestHeader("Authorization") String token);

    @PostMapping("/api/inventory/documents/{id}/approve")
    Map<String, Object> approveStockDocument(@PathVariable("id") Long id, @RequestHeader("Authorization") String token);

    @PostMapping("/api/inventory/documents/{id}/cancel")
    Map<String, Object> cancelStockDocument(@PathVariable("id") Long id, @RequestHeader("Authorization") String token);
}

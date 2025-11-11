package com.smartretail.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "service-product", url = "${product.service.url:http://localhost:8085}")
public interface ProductServiceClient {

    @GetMapping("/api/products/{id}")
    Map<String, Object> getProductById(@PathVariable("id") Long id, @RequestHeader("Authorization") String token);

    @GetMapping("/api/products/by-code/{code}")
    Map<String, Object> getProductByCode(@PathVariable("code") String code, @RequestHeader("Authorization") String token);

    @GetMapping("/api/products/{productId}/units/{unitId}")
    Map<String, Object> getProductUnitById(@PathVariable("productId") Long productId, @PathVariable("unitId") Long unitId, @RequestHeader("Authorization") String token);

    // Public product unit endpoint that does not require productId
    @GetMapping("/api/products/units/{id}")
    Map<String, Object> getProductUnitPublic(@PathVariable("id") Long id, @RequestHeader("Authorization") String token);

    @GetMapping("/api/products/{productId}/prices/current")
    Map<String, Object> getCurrentPrice(@PathVariable("productId") Long productId, @RequestParam("productUnitId") Long productUnitId, @RequestHeader("Authorization") String token);
}

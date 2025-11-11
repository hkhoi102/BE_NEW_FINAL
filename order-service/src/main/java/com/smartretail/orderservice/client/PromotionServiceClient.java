package com.smartretail.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "promotion-service", url = "${promotion.service.url:http://localhost:8085}")
public interface PromotionServiceClient {

    @GetMapping("/api/promotions/headers/{id}")
    Map<String, Object> getPromotionById(@PathVariable("id") Long id, @RequestHeader("Authorization") String token);

    @PostMapping("/api/promotions/calculate")
    Map<String, Object> calculatePromotion(@RequestBody Map<String, Object> request, @RequestHeader("Authorization") String token);

    @GetMapping("/api/promotions/headers/active")
    Map<String, Object> getActivePromotions(@RequestHeader("Authorization") String token);

    // New calculation endpoint aligning with promotion-service
    @PostMapping("/api/promotions/calculation/order")
    Map<String, Object> calculateOrderPromotions(@RequestBody Map<String, Object> request,
                                                 @RequestHeader("Authorization") String token);

    // Bill-level calculation endpoint
    @PostMapping("/api/promotions/calculation/bill")
    Map<String, Object> calculateBill(@RequestBody Map<String, Object> request,
                                      @RequestHeader("Authorization") String token);
}

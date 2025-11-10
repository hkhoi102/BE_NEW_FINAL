package com.smartretail.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "payment-service")
public interface PaymentServiceClient {

	@PostMapping("/api/payments/sepay/intent")
	Map<String, Object> createSepayIntent(@RequestBody Map<String, Object> request);
}



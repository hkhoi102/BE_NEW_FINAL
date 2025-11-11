package com.smartretail.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "service-customer", url = "${customer.service.url:http://api-gateway:8085}")
public interface CustomerServiceClient {

    @GetMapping("/api/customers/me")
    Map<String, Object> getCurrentCustomer(@RequestHeader("Authorization") String token);

    @GetMapping("/api/customers/by-user/{userId}")
    Map<String, Object> getCustomerByUserId(@PathVariable("userId") Long userId, @RequestHeader("Authorization") String token);

    @GetMapping("/api/customers/{id}")
    Map<String, Object> getCustomerById(@PathVariable("id") Long id, @RequestHeader("Authorization") String token);
}

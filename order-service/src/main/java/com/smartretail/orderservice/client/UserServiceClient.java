package com.smartretail.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "user-service", url = "${user.service.url:http://api-gateway:8085}")
public interface UserServiceClient {

    @GetMapping("/api/users/me")
    Map<String, Object> getCurrentUser(@RequestHeader("Authorization") String token);
}

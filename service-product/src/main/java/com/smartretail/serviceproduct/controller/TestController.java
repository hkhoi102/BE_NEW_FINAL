package com.smartretail.serviceproduct.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class TestController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "PONG from service-product!");
        response.put("service", "service-product");
        response.put("port", "8084");
        response.put("status", "connected");
        response.put("gateway", "ready");
        return response;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "service-product");
        response.put("port", "8084");
        response.put("version", "1.0.0");
        response.put("apis", new String[]{
            "/api/products/** - Product Management",
            "/api/categories/** - Category Management",
            "/api/uoms/** - Unit Management",
            "/api/product/** - Health & Test"
        });
        return response;
    }
}

package com.smartretail.apigateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin(origins = "*")
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "API Gateway is running on port 8085!";
    }

    @GetMapping("/")
    public String home() {
        return "Welcome to Smart Retail API Gateway!";
    }
}

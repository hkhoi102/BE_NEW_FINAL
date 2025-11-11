package com.smartretail.orderservice.service;

import com.smartretail.orderservice.client.CustomerServiceClient;
import com.smartretail.orderservice.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomerInfoService {

    @Autowired
    private CustomerServiceClient customerServiceClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public Long getCustomerIdFromToken(String authHeader) {
        String token = jwtTokenProvider.extractTokenFromHeader(authHeader);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }

        try {
            // Ưu tiên gọi /me qua Gateway để tránh yêu cầu quyền admin ở /by-user/{id}
            Map<String, Object> response = customerServiceClient.getCurrentCustomer(authHeader);
            if (response != null) {
                // Customer Service trả về CustomerInfo object trực tiếp
                Long customerId = ((Number) response.get("id")).longValue();
                return customerId;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get customer information: " + e.getMessage());
        }

        throw new RuntimeException("Customer not found");
    }

    /**
     * Try to get customerId; if customer-service is not accessible (e.g., 403),
     * return null so callers can still proceed for preview pricing.
     */
    public Long tryGetCustomerIdOrNull(String authHeader) {
        String token = jwtTokenProvider.extractTokenFromHeader(authHeader);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return null;
        }

        try {
            Map<String, Object> response = customerServiceClient.getCurrentCustomer(authHeader);
            if (response != null) {
                return ((Number) response.get("id")).longValue();
            }
        } catch (Exception ignore) {
            return null;
        }
        return null;
    }

    public Map<String, Object> getCustomerInfoFromToken(String authHeader) {
        String token = jwtTokenProvider.extractTokenFromHeader(authHeader);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }

        try {
            Map<String, Object> response = customerServiceClient.getCurrentCustomer(authHeader);
            if (response != null) {
                // Customer Service trả về CustomerInfo object trực tiếp
                return response;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get customer information: " + e.getMessage());
        }

        throw new RuntimeException("Customer not found");
    }

    public Map<String, Object> getCustomerProfile(String authHeader) {
        try {
            Map<String, Object> me = customerServiceClient.getCurrentCustomer(authHeader);
            if (me != null) {
                return me;
            }
        } catch (Exception ignore) {}
        try {
            return getCustomerInfoFromToken(authHeader);
        } catch (Exception e) {
            return java.util.Collections.emptyMap();
        }
    }

    /**
     * Ensure a customer exists for the current user. If not found, attempt to auto-provision.
     * fallbackPhone will be used when creating the customer if the token doesn't contain a phone claim.
     */
    public Long ensureCustomer(String authHeader, String fallbackPhone) {
        try {
            Long id = getCustomerIdFromToken(authHeader);
            if (id != null) return id;
        } catch (Exception ignored) {}

        // Auto-provision using claims
        String token = jwtTokenProvider.extractTokenFromHeader(authHeader);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        if (userId == null) {
            throw new RuntimeException("User ID not found in token");
        }
        String subject = jwtTokenProvider.getUsernameFromToken(token);
        String email = (subject != null && subject.contains("@"))
                ? subject
                : "user" + userId + "@local";
        String name = (subject != null && !subject.isBlank()) ? subject : ("User " + userId);
        String phone = (fallbackPhone != null && !fallbackPhone.isBlank()) ? fallbackPhone : "0000000000";

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("userId", userId);
        body.put("name", name);
        body.put("email", email);
        body.put("phone", phone);

        Map<String, Object> created = customerServiceClient.provision(body, authHeader);
        if (created != null && created.get("id") != null) {
            return ((Number) created.get("id")).longValue();
        }
        throw new RuntimeException("Failed to auto-provision customer profile");
    }
}

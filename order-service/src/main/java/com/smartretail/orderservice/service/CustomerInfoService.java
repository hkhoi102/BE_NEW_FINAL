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
}

package com.smartretail.servicecustomer.controller;

import com.smartretail.servicecustomer.dto.CustomerDtos;
import com.smartretail.servicecustomer.service.CustomerService;
import com.smartretail.servicecustomer.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin("*")
public class CustomerController {
    private final CustomerService customerService;
    private final JwtTokenProvider jwtTokenProvider;

    public CustomerController(CustomerService customerService, JwtTokenProvider jwtTokenProvider) { this.customerService = customerService; this.jwtTokenProvider = jwtTokenProvider; }

    @GetMapping("/health")
    public String health() { return "Customer Service is running!"; }

    @PostMapping
    public ResponseEntity<CustomerDtos.CustomerInfo> create(@Valid @RequestBody CustomerDtos.CreateRequest req) {
        return ResponseEntity.ok(customerService.create(req));
    }

    @PostMapping("/provision")
    public ResponseEntity<CustomerDtos.CustomerInfo> provision(@Valid @RequestBody CustomerDtos.ProvisionRequest req) {
        return ResponseEntity.ok(customerService.provision(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDtos.CustomerInfo> get(@PathVariable Long id) { return ResponseEntity.ok(customerService.get(id)); }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<CustomerDtos.CustomerInfo> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(customerService.getByUserId(userId));
    }

    @GetMapping("/me")
    public ResponseEntity<java.util.Map<String, Object>> me(@RequestHeader(org.springframework.http.HttpHeaders.AUTHORIZATION) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).build();
            }
            String token = authHeader.substring(7);
            var claims = jwtTokenProvider.validateAndParse(token).getBody();

            Number uidNum = (Number) (claims.get("uid") != null ? claims.get("uid")
                    : (claims.get("userId") != null ? claims.get("userId")
                    : (claims.get("user_id") != null ? claims.get("user_id")
                    : claims.get("id"))));
            if (uidNum == null) {
                return ResponseEntity.badRequest().build();
            }
            Long userId = uidNum.longValue();
            var info = customerService.getByUserId(userId);
            java.util.Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("id", info.id);
            resp.put("customerId", info.id); // alias cho Order Service
            resp.put("userId", info.userId);
            resp.put("name", info.name);
            resp.put("phone", info.phone);
            resp.put("email", info.email);
            resp.put("address", info.address);
            resp.put("points", info.points);
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<CustomerDtos.CustomerInfo>> list(@RequestParam(required = false) String q, Pageable pageable) {
        return ResponseEntity.ok(customerService.list(q, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDtos.CustomerInfo> update(@PathVariable Long id, @Valid @RequestBody CustomerDtos.UpdateRequest req) {
        return ResponseEntity.ok(customerService.update(id, req));
    }

    @PatchMapping("/{id}/address")
    public ResponseEntity<CustomerDtos.CustomerInfo> updateAddress(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(customerService.updateAddress(id, body.getOrDefault("address", "")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { customerService.delete(id); return ResponseEntity.ok().build(); }

    @GetMapping("/{id}/points")
    public ResponseEntity<Integer> points(@PathVariable Long id) { return ResponseEntity.ok(customerService.getPoints(id)); }

    @PutMapping("/{id}/points/increase")
    public ResponseEntity<Integer> inc(@PathVariable Long id, @RequestParam int points) { return ResponseEntity.ok(customerService.increasePoints(id, points)); }

    @PutMapping("/{id}/points/decrease")
    public ResponseEntity<Integer> dec(@PathVariable Long id, @RequestParam int points) { return ResponseEntity.ok(customerService.decreasePoints(id, points)); }
}



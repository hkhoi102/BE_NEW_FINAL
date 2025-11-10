package com.smartretail.servicecustomer.controller;

import com.smartretail.servicecustomer.dto.CustomerDtos;
import com.smartretail.servicecustomer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin("*")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) { this.customerService = customerService; }

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
    public ResponseEntity<CustomerDtos.CustomerInfo> me(Authentication auth) {
        // email is subject in JWT; in your system, you also include userId claim. For now, resolve by email -> not implemented here.
        // Recommend client call /by-user/{userId} using claim userId.
        return ResponseEntity.badRequest().build();
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



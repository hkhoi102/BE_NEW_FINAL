package com.smartretail.servicecustomer.service;

import com.smartretail.servicecustomer.dto.CustomerDtos;
import com.smartretail.servicecustomer.model.Customer;
import com.smartretail.servicecustomer.model.CustomerPoint;
import com.smartretail.servicecustomer.repository.CustomerPointRepository;
import com.smartretail.servicecustomer.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerPointRepository pointRepository;

    public CustomerService(CustomerRepository customerRepository, CustomerPointRepository pointRepository) {
        this.customerRepository = customerRepository;
        this.pointRepository = pointRepository;
    }

    public CustomerDtos.CustomerInfo create(CustomerDtos.CreateRequest req) {
        if (customerRepository.existsByEmail(req.email)) throw new IllegalArgumentException("Email exists");
        if (customerRepository.existsByPhone(req.phone)) throw new IllegalArgumentException("Phone exists");
        Customer c = new Customer();
        c.setUserId(req.userId);
        c.setName(req.name);
        c.setPhone(req.phone);
        c.setEmail(req.email);
        c = customerRepository.save(c);
        ensurePoint(c.getId());
        return toInfo(c);
    }

    public CustomerDtos.CustomerInfo provision(CustomerDtos.ProvisionRequest req) {
        return customerRepository.findByUserId(req.userId)
                .map(this::toInfo)
                .orElseGet(() -> {
                    CustomerDtos.CreateRequest c = new CustomerDtos.CreateRequest();
                    c.userId = req.userId; c.name = req.name; c.email = req.email; c.phone = req.phone;
                    return create(c);
                });
    }

    @Transactional(readOnly = true)
    public CustomerDtos.CustomerInfo get(Long id) { return toInfo(customerRepository.findById(id).orElseThrow()); }

    @Transactional(readOnly = true)
    public Page<CustomerDtos.CustomerInfo> list(String q, Pageable pageable) {
        return customerRepository
                .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCase(q==null?"":q, q==null?"":q, q==null?"":q, pageable)
                .map(this::toInfo);
    }

    public CustomerDtos.CustomerInfo update(Long id, CustomerDtos.UpdateRequest req) {
        Customer c = customerRepository.findById(id).orElseThrow();
        c.setName(req.name);
        c.setPhone(req.phone);
        c.setEmail(req.email);
        c.setAddress(req.address);
        return toInfo(customerRepository.save(c));
    }

    public void delete(Long id) { customerRepository.deleteById(id); }

    @Transactional(readOnly = true)
    public CustomerDtos.CustomerInfo getByUserId(Long userId) {
        return customerRepository.findByUserId(userId).map(this::toInfo).orElseThrow();
    }

    public CustomerDtos.CustomerInfo updateAddress(Long id, String address) {
        Customer c = customerRepository.findById(id).orElseThrow();
        c.setAddress(address);
        return toInfo(customerRepository.save(c));
    }

    @Transactional(readOnly = true)
    public Integer getPoints(Long id) {
        return pointRepository.findByCustomerId(id).map(CustomerPoint::getPoints).orElse(0);
    }

    public Integer increasePoints(Long id, int delta) {
        CustomerPoint p = ensurePoint(id);
        p.setPoints(p.getPoints() + Math.max(delta, 0));
        return pointRepository.save(p).getPoints();
    }

    public Integer decreasePoints(Long id, int delta) {
        CustomerPoint p = ensurePoint(id);
        int v = Math.max(p.getPoints() - Math.max(delta, 0), 0);
        p.setPoints(v);
        return pointRepository.save(p).getPoints();
    }

    private CustomerPoint ensurePoint(Long customerId) {
        return pointRepository.findByCustomerId(customerId).orElseGet(() -> {
            CustomerPoint p = new CustomerPoint();
            p.setCustomerId(customerId);
            p.setPoints(0);
            return pointRepository.save(p);
        });
    }

    private CustomerDtos.CustomerInfo toInfo(Customer c) {
        CustomerDtos.CustomerInfo i = new CustomerDtos.CustomerInfo();
        i.id = c.getId(); i.userId = c.getUserId(); i.name = c.getName(); i.phone = c.getPhone(); i.email = c.getEmail(); i.address = c.getAddress();
        i.points = getPoints(c.getId());
        return i;
    }
}



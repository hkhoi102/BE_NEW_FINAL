package com.smartretail.serviceproduct.controller;

import com.smartretail.serviceproduct.dto.ProductUnitDto;
import com.smartretail.serviceproduct.service.ProductUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/units")
@CrossOrigin(origins = "*")
public class ProductUnitPublicController {

    @Autowired
    private ProductUnitService productUnitService;

    // GET /api/products/units/{id} - Lấy ProductUnit theo ID (không cần productId)
    @GetMapping("/{id}")
    public ResponseEntity<ProductUnitDto> getById(@PathVariable("id") Long id) {
        try {
            ProductUnitDto dto = productUnitService.getProductUnitById(id);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // GET /api/products/units?productId=&unitId= - Lấy ProductUnit theo productId+unitId
    @GetMapping
    public ResponseEntity<ProductUnitDto> getByProductAndUnit(
            @RequestParam("productId") Long productId,
            @RequestParam("unitId") Long unitId) {
        try {
            List<ProductUnitDto> list = productUnitService.getProductUnitsByProductId(productId);
            ProductUnitDto found = list.stream()
                    .filter(pu -> pu.getUnitId().equals(unitId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("ProductUnit not found for productId=" + productId + ", unitId=" + unitId));

            return ResponseEntity.ok(found);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // GET /api/products/units/list?productId= - Lấy toàn bộ đơn vị của sản phẩm (trả về list thô)
    @GetMapping("/list")
    public ResponseEntity<List<ProductUnitDto>> getUnitsByProduct(@RequestParam("productId") Long productId) {
        try {
            List<ProductUnitDto> list = productUnitService.getProductUnitsByProductId(productId);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}



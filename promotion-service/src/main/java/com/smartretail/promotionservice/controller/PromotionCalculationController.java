package com.smartretail.promotionservice.controller;

import com.smartretail.promotionservice.service.PromotionCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/promotions/calculation")
@CrossOrigin(origins = "*")
public class PromotionCalculationController {

    @Autowired
    private PromotionCalculationService calculationService;

    /**
     * DTO để nhận thông tin sản phẩm cần tính khuyến mãi
     */
    public static class ProductCalculationRequest {
        private Long productId;
        private Long productUnitId;
        private Long categoryId;
        private Integer quantity;
        private BigDecimal unitPrice;

        // Getters and Setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Long getProductUnitId() { return productUnitId; }
        public void setProductUnitId(Long productUnitId) { this.productUnitId = productUnitId; }

        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    }

    /**
     * DTO nhận tổng tiền bill để tính giảm
     */
    public static class BillCalculationRequest {
        private BigDecimal totalAmount;
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    }

    /**
     * DTO để nhận yêu cầu tính toán khuyến mãi cho đơn hàng
     */
    public static class OrderCalculationRequest {
        private List<ProductCalculationRequest> products;
        private Long customerId;

        // Getters and Setters
        public List<ProductCalculationRequest> getProducts() { return products; }
        public void setProducts(List<ProductCalculationRequest> products) { this.products = products; }

        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
    }

    /**
     * Tính giảm giá theo bill (chỉ cần tổng tiền)
     */
    @PostMapping("/bill")
    public ResponseEntity<PromotionCalculationService.BillDiscountResult> calculateBill(@RequestBody BillCalculationRequest request) {
        try {
            PromotionCalculationService.BillDiscountResult result =
                calculationService.calculateBillDiscount(request.getTotalAmount());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Tính toán khuyến mãi cho một đơn hàng
     */
    @PostMapping("/order")
    public ResponseEntity<PromotionCalculationService.OrderPromotionResult> calculateOrderPromotions(
            @RequestBody OrderCalculationRequest request) {
        try {
            // Chuyển đổi request thành ProductPromotionInfo
            List<PromotionCalculationService.ProductPromotionInfo> products = request.getProducts().stream()
                .map(p -> new PromotionCalculationService.ProductPromotionInfo(
                    p.getProductId(), p.getProductUnitId(), p.getCategoryId(), p.getQuantity(), p.getUnitPrice()))
                .toList();

            PromotionCalculationService.OrderPromotionResult result =
                calculationService.calculateOrderPromotions(products, request.getCustomerId());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Tính toán khuyến mãi cho một sản phẩm đơn lẻ
     */
    @PostMapping("/product")
    public ResponseEntity<PromotionCalculationService.ProductPromotionInfo> calculateProductPromotions(
            @RequestBody ProductCalculationRequest request) {
        try {
            PromotionCalculationService.ProductPromotionInfo product =
                new PromotionCalculationService.ProductPromotionInfo(
                    request.getProductId(), request.getProductUnitId(), request.getCategoryId(),
                    request.getQuantity(), request.getUnitPrice());

            // Tạo danh sách chỉ có 1 sản phẩm để sử dụng service
            List<PromotionCalculationService.ProductPromotionInfo> products = List.of(product);

            PromotionCalculationService.OrderPromotionResult result =
                calculationService.calculateOrderPromotions(products, null);

            // Trả về thông tin sản phẩm đầu tiên
            return ResponseEntity.ok(result.getProductPromotions().get(0));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Kiểm tra khuyến mãi có sẵn cho một sản phẩm
     */
    @GetMapping("/check/{productId}")
    public ResponseEntity<String> checkProductPromotions(@PathVariable Long productId) {
        try {
            // Tạo một sản phẩm giả để kiểm tra
            PromotionCalculationService.ProductPromotionInfo product =
                new PromotionCalculationService.ProductPromotionInfo(
                    productId, /* productId */
                    1L,        /* productUnitId (mock) */
                    1L,        /* categoryId (mock) */
                    1,         /* quantity */
                    BigDecimal.ONE /* unitPrice */
                );

            List<PromotionCalculationService.ProductPromotionInfo> products = List.of(product);
            PromotionCalculationService.OrderPromotionResult result =
                calculationService.calculateOrderPromotions(products, null);

            if (result.getProductPromotions().get(0).getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                return ResponseEntity.ok("Sản phẩm có khuyến mãi: " +
                    String.join(", ", result.getProductPromotions().get(0).getAppliedPromotions()));
            } else {
                return ResponseEntity.ok("Sản phẩm không có khuyến mãi");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi kiểm tra khuyến mãi: " + e.getMessage());
        }
    }
}

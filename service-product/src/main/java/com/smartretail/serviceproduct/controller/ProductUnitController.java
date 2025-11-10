package com.smartretail.serviceproduct.controller;

import com.smartretail.serviceproduct.dto.ProductUnitDto;
import com.smartretail.serviceproduct.service.ProductUnitService;
import com.smartretail.serviceproduct.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/products/{productId}/units")
@CrossOrigin(origins = "*")
public class ProductUnitController {

    @Autowired
    private ProductUnitService productUnitService;

    @Autowired
    private ImageService imageService;

    // Thêm đơn vị tính mới cho sản phẩm
    @PostMapping
    public ResponseEntity<?> addProductUnit(@PathVariable Long productId, @RequestBody ProductUnitDto productUnitDto) {
        try {
            productUnitDto.setProductId(productId);
            ProductUnitDto savedProductUnit = productUnitService.addProductUnit(productUnitDto);
            return ResponseEntity.ok(new ApiResponse(true, "Product unit added successfully", savedProductUnit));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // Lấy danh sách đơn vị tính của sản phẩm
    @GetMapping
    public ResponseEntity<?> getProductUnits(@PathVariable Long productId) {
        try {
            List<ProductUnitDto> productUnits = productUnitService.getProductUnitsByProductId(productId);
            return ResponseEntity.ok(new ApiResponse(true, "Product units retrieved successfully", productUnits));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // Cập nhật đơn vị tính
    @PutMapping("/{unitId}")
    public ResponseEntity<?> updateProductUnit(@PathVariable Long productId, @PathVariable Long unitId, @RequestBody ProductUnitDto productUnitDto) {
        try {
            productUnitDto.setId(unitId);
            productUnitDto.setProductId(productId);
            ProductUnitDto updatedProductUnit = productUnitService.updateProductUnit(unitId, productUnitDto);
            return ResponseEntity.ok(new ApiResponse(true, "Product unit updated successfully", updatedProductUnit));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // Xóa đơn vị tính
    @DeleteMapping("/{unitId}")
    public ResponseEntity<?> deleteProductUnit(@PathVariable Long productId, @PathVariable Long unitId) {
        try {
            boolean deleted = productUnitService.deleteProductUnit(unitId);
            if (deleted) {
                return ResponseEntity.ok(new ApiResponse(true, "Product unit deleted successfully", null));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Failed to delete product unit", null));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // Lấy đơn vị tính theo ID (cho Order Service)
    @GetMapping("/{unitId}")
    public ResponseEntity<?> getProductUnitById(@PathVariable Long productId, @PathVariable Long unitId) {
        try {
            ProductUnitDto productUnit = productUnitService.getProductUnitById(unitId);
            if (productUnit != null) {
                return ResponseEntity.ok(new ApiResponse(true, "Product unit retrieved successfully", productUnit));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // Đặt đơn vị tính làm mặc định cho sản phẩm
    @PutMapping("/{unitId}/make-default")
    public ResponseEntity<?> makeDefault(@PathVariable Long productId, @PathVariable Long unitId) {
        try {
            ProductUnitDto dto = productUnitService.makeDefaultUnit(productId, unitId);
            return ResponseEntity.ok(new ApiResponse(true, "Default unit set successfully", dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // Kích hoạt đơn vị tính
    @PatchMapping("/{unitId}/activate")
    public ResponseEntity<?> activateProductUnit(@PathVariable Long productId, @PathVariable Long unitId) {
        try {
            ProductUnitDto dto = productUnitService.activateProductUnit(unitId);
            return ResponseEntity.ok(new ApiResponse(true, "Product unit activated successfully", dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // Tạm dừng đơn vị tính
    @PatchMapping("/{unitId}/deactivate")
    public ResponseEntity<?> deactivateProductUnit(@PathVariable Long productId, @PathVariable Long unitId) {
        try {
            ProductUnitDto dto = productUnitService.deactivateProductUnit(unitId);
            return ResponseEntity.ok(new ApiResponse(true, "Product unit deactivated successfully", dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // PUT /api/products/{productId}/units/{unitId}/image - Update product unit image
    @PutMapping("/{unitId}/image")
    public ResponseEntity<?> updateProductUnitImage(
            @PathVariable Long productId,
            @PathVariable Long unitId,
            @RequestParam("image") MultipartFile image) {
        try {
            // Kiểm tra ProductUnit có tồn tại không
            ProductUnitDto existingUnit = productUnitService.getProductUnitById(unitId);

            if (existingUnit == null || !existingUnit.getProductId().equals(productId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy đơn vị tính với ID: " + unitId);
                return ResponseEntity.notFound().build();
            }

            // Xóa ảnh cũ nếu có
            if (existingUnit.getImageUrl() != null && !existingUnit.getImageUrl().trim().isEmpty()) {
                try {
                    imageService.deleteProductImage(existingUnit.getImageUrl());
                } catch (Exception e) {
                    System.err.println("Không thể xóa ảnh cũ: " + e.getMessage());
                }
            }

            // Upload ảnh mới
            String newImagePath = imageService.uploadProductImage(image);

            // Cập nhật imageUrl cho ProductUnit
            ProductUnitDto updateUnitDto = new ProductUnitDto();
            updateUnitDto.setImageUrl(newImagePath);
            ProductUnitDto updatedUnit = productUnitService.updateProductUnit(unitId, updateUnitDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ảnh đơn vị tính đã được cập nhật thành công");
            response.put("data", updatedUnit);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật ảnh đơn vị tính: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Inner class cho API response
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;

        public ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
}

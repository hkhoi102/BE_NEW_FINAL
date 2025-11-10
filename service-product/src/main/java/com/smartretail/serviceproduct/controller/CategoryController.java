package com.smartretail.serviceproduct.controller;

import com.smartretail.serviceproduct.dto.ProductCategoryDto;
import com.smartretail.serviceproduct.service.ProductCategoryService;
import com.smartretail.serviceproduct.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    @Autowired
    private ProductCategoryService categoryService;

    @Autowired
    private ImageService imageService;

    // POST /api/categories - Create new category
    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody ProductCategoryDto categoryDto) {
        try {
            ProductCategoryDto createdCategory = categoryService.createCategory(categoryDto);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Category created successfully");
            response.put("data", createdCategory);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error creating category: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/categories - Get all categories
    @GetMapping
    public ResponseEntity<?> getAllCategories() {
        try {
            List<ProductCategoryDto> categories = categoryService.getAllCategories();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categories);
            response.put("total", categories.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving categories: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/categories/{id} - Get category by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        try {
            Optional<ProductCategoryDto> category = categoryService.getCategoryById(id);
            if (category.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", category.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Category not found with id: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving category: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PUT /api/categories/{id} - Update category
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody ProductCategoryDto categoryDto) {
        try {
            Optional<ProductCategoryDto> updatedCategory = categoryService.updateCategory(id, categoryDto);
            if (updatedCategory.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Category updated successfully");
                response.put("data", updatedCategory.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Category not found with id: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating category: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // DELETE /api/categories/{id} - Delete category (soft delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            boolean deleted = categoryService.deleteCategory(id);
            if (deleted) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Category deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Category not found with id: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deleting category: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // POST /api/categories/with-image - Create new category with image
    @PostMapping("/with-image")
    public ResponseEntity<?> createCategoryWithImage(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            // Tạo ProductCategoryDto
            ProductCategoryDto categoryDto = new ProductCategoryDto();
            categoryDto.setName(name);
            categoryDto.setDescription(description);

            // Xử lý ảnh nếu có
            if (image != null && !image.isEmpty()) {
                try {
                    String imagePath = imageService.uploadProductImage(image);
                    categoryDto.setImageUrl(imagePath);
                } catch (Exception e) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Lỗi khi upload ảnh: " + e.getMessage());
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Tạo category
            ProductCategoryDto createdCategory = categoryService.createCategory(categoryDto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Category đã được tạo thành công với ảnh");
            response.put("data", createdCategory);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi tạo category: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PUT /api/categories/{id}/image - Update category image
    @PutMapping("/{id}/image")
    public ResponseEntity<?> updateCategoryImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {
        try {
            // Kiểm tra category có tồn tại không
            Optional<ProductCategoryDto> existingCategory = categoryService.getCategoryById(id);
            if (!existingCategory.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy category với ID: " + id);
                return ResponseEntity.notFound().build();
            }

            // Xóa ảnh cũ nếu có
            ProductCategoryDto currentCategory = existingCategory.get();
            if (currentCategory.getImageUrl() != null && !currentCategory.getImageUrl().trim().isEmpty()) {
                try {
                    imageService.deleteProductImage(currentCategory.getImageUrl());
                } catch (Exception e) {
                    System.err.println("Không thể xóa ảnh cũ: " + e.getMessage());
                }
            }

            // Upload ảnh mới
            String newImagePath = imageService.uploadProductImage(image);

            // Cập nhật category với ảnh mới
            ProductCategoryDto updateDto = new ProductCategoryDto();
            updateDto.setName(currentCategory.getName());
            updateDto.setDescription(currentCategory.getDescription());
            updateDto.setImageUrl(newImagePath);

            Optional<ProductCategoryDto> updatedCategory = categoryService.updateCategory(id, updateDto);

            if (updatedCategory.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Ảnh category đã được cập nhật thành công");
                response.put("data", updatedCategory.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không thể cập nhật category");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật ảnh category: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

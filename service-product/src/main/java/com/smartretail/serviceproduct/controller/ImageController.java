package com.smartretail.serviceproduct.controller;

import com.smartretail.serviceproduct.dto.ProductImageDto;
import com.smartretail.serviceproduct.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private com.smartretail.serviceproduct.service.S3Service s3Service;

    /**
     * Upload ảnh sản phẩm
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "File ảnh không được để trống");
                return ResponseEntity.badRequest().body(response);
            }

            // Upload và lưu ảnh
            String imagePath = imageService.uploadProductImage(file);

            // Tạo response DTO
            ProductImageDto imageDto = new ProductImageDto(
                imagePath,
                file.getOriginalFilename(),
                imageService.getReadableFileSize(file.getSize()),
                file.getContentType()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ảnh đã được upload thành công");
            response.put("data", imageDto);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi upload ảnh: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Upload ảnh cho sản phẩm cụ thể
     */
    @PostMapping("/products/{productId}/upload")
    public ResponseEntity<?> uploadProductImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "File ảnh không được để trống");
                return ResponseEntity.badRequest().body(response);
            }

            // Upload và lưu ảnh
            String imagePath = imageService.uploadProductImage(file);

            // Tạo response DTO
            ProductImageDto imageDto = new ProductImageDto(
                imagePath,
                file.getOriginalFilename(),
                imageService.getReadableFileSize(file.getSize()),
                file.getContentType()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ảnh đã được upload thành công cho sản phẩm ID: " + productId);
            response.put("data", imageDto);
            response.put("productId", productId);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi upload ảnh: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Xóa ảnh sản phẩm
     */
    @DeleteMapping("/{imagePath}")
    public ResponseEntity<?> deleteImage(@PathVariable String imagePath) {
        try {
            boolean deleted = imageService.deleteProductImage(imagePath);

            Map<String, Object> response = new HashMap<>();
            if (deleted) {
                response.put("success", true);
                response.put("message", "Ảnh đã được xóa thành công");
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy ảnh để xóa");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi xóa ảnh: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Lấy thông tin ảnh
     */
    @GetMapping("/{imagePath}")
    public ResponseEntity<?> getImageInfo(@PathVariable String imagePath) {
        try {
            String fullPath = imageService.getFullImagePath(imagePath);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "imagePath", imagePath,
                "fullPath", fullPath,
                "exists", fullPath != null
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy thông tin ảnh: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Upload nhiều ảnh cùng lúc
     */
    @PostMapping("/upload/multiple")
    public ResponseEntity<?> uploadMultipleImages(@RequestParam("files") MultipartFile[] files) {
        try {
            if (files == null || files.length == 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không có file nào được chọn");
                return ResponseEntity.badRequest().body(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Upload " + files.length + " ảnh thành công");
            response.put("totalFiles", files.length);

            java.util.List<ProductImageDto> uploadedImages = new java.util.ArrayList<>();
            int successCount = 0;
            int errorCount = 0;

            for (MultipartFile file : files) {
                try {
                    String imagePath = imageService.uploadProductImage(file);
                    ProductImageDto imageDto = new ProductImageDto(
                        imagePath,
                        file.getOriginalFilename(),
                        imageService.getReadableFileSize(file.getSize()),
                        file.getContentType()
                    );
                    uploadedImages.add(imageDto);
                    successCount++;
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("Lỗi upload file " + file.getOriginalFilename() + ": " + e.getMessage());
                }
            }

            response.put("successCount", successCount);
            response.put("errorCount", errorCount);
            response.put("data", uploadedImages);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi upload nhiều ảnh: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Test kết nối S3
     */
    @GetMapping("/test-s3")
    public ResponseEntity<?> testS3Connection() {
        try {
            boolean isConnected = s3Service.testConnection();

            Map<String, Object> response = new HashMap<>();
            response.put("success", isConnected);
            response.put("message", isConnected ? "Kết nối S3 thành công" : "Kết nối S3 thất bại");
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi test kết nối S3: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

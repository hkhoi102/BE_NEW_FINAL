package com.smartretail.serviceproduct.controller;

import com.smartretail.serviceproduct.dto.ProductDto;
import com.smartretail.serviceproduct.dto.ImportResultDto;
import com.smartretail.serviceproduct.service.ProductService;
import com.smartretail.serviceproduct.service.ImageService;
import com.smartretail.serviceproduct.service.ProductImportService;
import com.smartretail.serviceproduct.service.ProductUnitService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private ProductImportService productImportService;

    @Autowired
    private ProductUnitService productUnitService;

    // POST /api/products - Create new product
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductDto productDto) {
        try {
            ProductDto createdProduct = productService.createProduct(productDto);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product created successfully");
            response.put("data", createdProduct);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error creating product: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // POST /api/products/with-image - Create new product with image
    @PostMapping("/with-image")
    public ResponseEntity<?> createProductWithImage(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "expirationDate", required = false) String expirationDate,
            @RequestParam(value = "defaultUnitId", required = false) Long defaultUnitId,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            // Tạo ProductDto
            ProductDto productDto = new ProductDto();
            productDto.setName(name);
            productDto.setDescription(description);
            productDto.setCategoryId(categoryId);
            if (code != null && !code.trim().isEmpty()) {
                productDto.setCode(code.trim());
            }

            if (expirationDate != null && !expirationDate.trim().isEmpty()) {
                productDto.setExpirationDate(java.time.LocalDate.parse(expirationDate));
            }


            if (defaultUnitId != null) {
                productDto.setDefaultUnitId(defaultUnitId);
            }

            // Tạo sản phẩm
            ProductDto createdProduct = productService.createProduct(productDto);

            // Xử lý ảnh nếu có - gán vào ProductUnit mặc định
            if (image != null && !image.isEmpty() && createdProduct.getProductUnits() != null && !createdProduct.getProductUnits().isEmpty()) {
                try {
                    String imagePath = imageService.uploadProductImage(image);
                    // Lấy ProductUnit mặc định (isDefault = true) hoặc ProductUnit đầu tiên
                    com.smartretail.serviceproduct.dto.ProductUnitInfo defaultUnit = createdProduct.getProductUnits().stream()
                            .filter(unit -> Boolean.TRUE.equals(unit.getIsDefault()))
                            .findFirst()
                            .orElse(createdProduct.getProductUnits().get(0));

                    // Cập nhật imageUrl cho ProductUnit này
                    if (defaultUnit != null) {
                        com.smartretail.serviceproduct.dto.ProductUnitDto updateUnitDto = new com.smartretail.serviceproduct.dto.ProductUnitDto();
                        updateUnitDto.setImageUrl(imagePath);
                        productUnitService.updateProductUnit(defaultUnit.getId(), updateUnitDto);
                    }
                } catch (Exception e) {
                    // Log lỗi nhưng không fail toàn bộ request
                    System.err.println("Lỗi khi upload ảnh cho ProductUnit: " + e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sản phẩm đã được tạo thành công với ảnh");
            response.put("data", createdProduct);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi tạo sản phẩm: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/products - Get all products with pagination and filters
    @GetMapping
    public ResponseEntity<?> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        try {
            Page<ProductDto> products;
            if (includeInactive) {
                products = productService.getAllProductsIncludingInactive(name, categoryId, page, size);
            } else {
                products = productService.getAllProducts(name, categoryId, page, size);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", products.getContent());
            response.put("totalElements", products.getTotalElements());
            response.put("totalPages", products.getTotalPages());
            response.put("currentPage", products.getNumber());
            response.put("size", products.getSize());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving products: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/products/{id} - Get product by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        try {
            Optional<ProductDto> product = productService.getProductById(id);
            if (product.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", product.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Product not found with id: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving product: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/products/by-code/{code} - Lookup product by barcode/QR code
    @GetMapping("/by-code/{code}")
    public ResponseEntity<?> getProductByCode(@PathVariable String code) {
        try {
            ProductDto dto = productService.getProductByBarcode(code);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/products/by-product-code/{code} - Lookup product by Product.code
    @GetMapping("/by-product-code/{code}")
    public ResponseEntity<?> getProductByProductCode(@PathVariable String code) {
        try {
            ProductDto dto = productService.getProductByCode(code);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Removed legacy by-product-code endpoint (Product.code removed)

    // ===== Barcode admin APIs =====
    @PostMapping("/units/{productUnitId}/barcodes")
    public ResponseEntity<?> addBarcode(@PathVariable Long productUnitId, @RequestBody Map<String, String> body) {
        try {
            String code = body.getOrDefault("code", "");
            String type = body.getOrDefault("type", "");
            var saved = productService.addBarcode(productUnitId, code, type);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/barcodes/{id}")
    public ResponseEntity<?> deleteBarcode(@PathVariable Long id) {
        try {
            productService.deleteBarcode(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Barcode deleted");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PUT /api/products/barcodes/{id} - update barcode code/type
    @PutMapping("/barcodes/{id}")
    public ResponseEntity<?> updateBarcode(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String code = body.get("code");
            String type = body.get("type");
            var updated = productService.updateBarcode(id, code, type);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }


    @GetMapping("/{productId}/barcodes")
    public ResponseEntity<?> getBarcodesByProductId(@PathVariable Long productId) {
        try {
            List<com.smartretail.serviceproduct.dto.BarcodeDto> barcodes = productService.getBarcodesByProductId(productId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", barcodes);
            response.put("total", barcodes.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/products/units/{productUnitId}/barcodes - get barcodes by product unit
    @GetMapping("/units/{productUnitId}/barcodes")
    public ResponseEntity<?> getBarcodesByProductUnit(@PathVariable Long productUnitId) {
        try {
            List<com.smartretail.serviceproduct.dto.BarcodeDto> barcodes = productService.getBarcodesByProductUnitId(productUnitId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", barcodes);
            response.put("total", barcodes.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PUT /api/products/{id} - Update product
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody ProductDto productDto) {
        try {
            Optional<ProductDto> updatedProduct = productService.updateProduct(id, productDto);
            if (updatedProduct.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Product updated successfully");
                response.put("data", updatedProduct.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Product not found with id: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating product: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PUT /api/products/{id}/with-image - Update product with image (multipart)
    @PutMapping(path = "/{id}/with-image")
    public ResponseEntity<?> updateProductWithImage(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "expirationDate", required = false) String expirationDate,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        try {
            // Lấy sản phẩm hiện tại
            Optional<ProductDto> existingOpt = productService.getProductById(id);
            if (!existingOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Product not found with id: " + id);
                return ResponseEntity.notFound().build();
            }

            ProductDto existing = existingOpt.get();

            // Chuẩn bị DTO cập nhật
            ProductDto updateDto = new ProductDto();
            updateDto.setName(name != null ? name : existing.getName());
            updateDto.setDescription(description != null ? description : existing.getDescription());
            updateDto.setCategoryId(categoryId != null ? categoryId : existing.getCategoryId());

            if (expirationDate != null && !expirationDate.trim().isEmpty()) {
                updateDto.setExpirationDate(java.time.LocalDate.parse(expirationDate));
            } else {
                updateDto.setExpirationDate(existing.getExpirationDate());
            }

            // Ảnh: nếu có ảnh mới, gán vào ProductUnit mặc định
            if (image != null && !image.isEmpty() && existing.getProductUnits() != null && !existing.getProductUnits().isEmpty()) {
                try {
                    String imagePath = imageService.uploadProductImage(image);
                    // Lấy ProductUnit mặc định (isDefault = true) hoặc ProductUnit đầu tiên
                    com.smartretail.serviceproduct.dto.ProductUnitInfo defaultUnit = existing.getProductUnits().stream()
                            .filter(unit -> Boolean.TRUE.equals(unit.getIsDefault()))
                            .findFirst()
                            .orElse(existing.getProductUnits().get(0));

                    // Xóa ảnh cũ nếu có
                    if (defaultUnit != null && defaultUnit.getImageUrl() != null && !defaultUnit.getImageUrl().trim().isEmpty()) {
                        try { imageService.deleteProductImage(defaultUnit.getImageUrl()); } catch (Exception ignored) {}
                    }

                    // Cập nhật imageUrl cho ProductUnit này
                    if (defaultUnit != null) {
                        com.smartretail.serviceproduct.dto.ProductUnitDto updateUnitDto = new com.smartretail.serviceproduct.dto.ProductUnitDto();
                        updateUnitDto.setImageUrl(imagePath);
                        productUnitService.updateProductUnit(defaultUnit.getId(), updateUnitDto);
                    }
                } catch (Exception e) {
                    // Log lỗi nhưng không fail toàn bộ request
                    System.err.println("Lỗi khi upload ảnh cho ProductUnit: " + e.getMessage());
                }
            }

            Optional<ProductDto> updated = productService.updateProduct(id, updateDto);
            if (updated.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Product updated successfully (with image)");
                response.put("data", updated.get());
                return ResponseEntity.ok(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Cannot update product");
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating product with image: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PUT /api/products/{id}/image - Update product image (deprecated - use ProductUnit image endpoint instead)
    // This endpoint is kept for backward compatibility but will update the default ProductUnit's image
    @PutMapping("/{id}/image")
    public ResponseEntity<?> updateProductImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {
        try {
            // Kiểm tra sản phẩm có tồn tại không
            Optional<ProductDto> existingProduct = productService.getProductById(id);
            if (!existingProduct.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy sản phẩm với ID: " + id);
                return ResponseEntity.notFound().build();
            }

            ProductDto currentProduct = existingProduct.get();

            // Tìm ProductUnit mặc định hoặc ProductUnit đầu tiên
            if (currentProduct.getProductUnits() == null || currentProduct.getProductUnits().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Sản phẩm chưa có đơn vị tính. Vui lòng thêm đơn vị tính trước.");
                return ResponseEntity.badRequest().body(response);
            }

            com.smartretail.serviceproduct.dto.ProductUnitInfo defaultUnit = currentProduct.getProductUnits().stream()
                    .filter(unit -> Boolean.TRUE.equals(unit.getIsDefault()))
                    .findFirst()
                    .orElse(currentProduct.getProductUnits().get(0));

            // Xóa ảnh cũ nếu có
            if (defaultUnit.getImageUrl() != null && !defaultUnit.getImageUrl().trim().isEmpty()) {
                try {
                    imageService.deleteProductImage(defaultUnit.getImageUrl());
                } catch (Exception e) {
                    System.err.println("Không thể xóa ảnh cũ: " + e.getMessage());
                }
            }

            // Upload ảnh mới
            String newImagePath = imageService.uploadProductImage(image);

            // Cập nhật imageUrl cho ProductUnit này
            com.smartretail.serviceproduct.dto.ProductUnitDto updateUnitDto = new com.smartretail.serviceproduct.dto.ProductUnitDto();
            updateUnitDto.setImageUrl(newImagePath);
            productUnitService.updateProductUnit(defaultUnit.getId(), updateUnitDto);

            // Lấy lại sản phẩm đã cập nhật
            Optional<ProductDto> updatedProduct = productService.getProductById(id);

            if (updatedProduct.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Ảnh sản phẩm đã được cập nhật thành công");
                response.put("data", updatedProduct.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không thể cập nhật sản phẩm");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật ảnh sản phẩm: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // DELETE /api/products/{id} - Delete product (soft delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            boolean deleted = productService.deleteProduct(id);
            if (deleted) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Product deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Product not found with id: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deleting product: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/products/search - Search products by name
    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestParam String q) {
        try {
            List<ProductDto> products = productService.searchProductsByName(q);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", products);
            response.put("total", products.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error searching products: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/products/category/{categoryId} - Get products by category
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getProductsByCategory(@PathVariable Long categoryId) {
        try {
            List<ProductDto> products = productService.getProductsByCategory(categoryId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", products);
            response.put("total", products.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving products by category: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/products/warehouse/{warehouseId} - Get products available in a specific warehouse
    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<?> getProductsByWarehouse(@PathVariable Long warehouseId) {
        try {
            List<ProductDto> products = productService.getProductsByWarehouse(warehouseId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", products);
            response.put("total", products.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving products by warehouse: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // POST /api/products/import/excel - Import products from Excel file
    @PostMapping("/import/excel")
    public ResponseEntity<?> importProductsFromExcel(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Warehouse-Id", required = false) Long warehouseIdHeader,
            @RequestHeader(value = "X-Stock-Location-Id", required = false) Long stockLocationIdHeader,
            @RequestParam(value = "warehouseId", required = false) Long warehouseIdParam,
            @RequestParam(value = "stockLocationId", required = false) Long stockLocationIdParam) {
        try {
            if (file == null || file.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "File không được để trống");
                return ResponseEntity.badRequest().body(response);
            }

            if (!file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "File phải có định dạng .xlsx");
                return ResponseEntity.badRequest().body(response);
            }

            Long finalWarehouseId = warehouseIdParam != null ? warehouseIdParam : warehouseIdHeader;
            Long finalStockLocationId = stockLocationIdParam != null ? stockLocationIdParam : stockLocationIdHeader;

            ImportResultDto result = productImportService.importFromExcel(file, finalWarehouseId, finalStockLocationId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Import hoàn thành: " + result.getSuccessCount() + " thành công, " + result.getErrorCount() + " lỗi");
            response.put("data", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi import file Excel: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // POST /api/products/import/csv - Import products from CSV file
    @PostMapping("/import/csv")
    public ResponseEntity<?> importProductsFromCSV(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Warehouse-Id", required = false) Long warehouseIdHeader,
            @RequestHeader(value = "X-Stock-Location-Id", required = false) Long stockLocationIdHeader,
            @RequestParam(value = "warehouseId", required = false) Long warehouseIdParam,
            @RequestParam(value = "stockLocationId", required = false) Long stockLocationIdParam) {
        try {
            if (file == null || file.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "File không được để trống");
                return ResponseEntity.badRequest().body(response);
            }

            if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "File phải có định dạng .csv");
                return ResponseEntity.badRequest().body(response);
            }

            Long finalWarehouseId = warehouseIdParam != null ? warehouseIdParam : warehouseIdHeader;
            Long finalStockLocationId = stockLocationIdParam != null ? stockLocationIdParam : stockLocationIdHeader;

            ImportResultDto result = productImportService.importFromCSV(file, finalWarehouseId, finalStockLocationId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Import hoàn thành: " + result.getSuccessCount() + " thành công, " + result.getErrorCount() + " lỗi");
            response.put("data", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi import file CSV: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/products/import/template - Download import template
    @GetMapping("/import/template")
    public ResponseEntity<?> downloadImportTemplate() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Template import sản phẩm");
            response.put("template", Map.of(
                "excelColumns", List.of("Name", "Description", "Category Name", "Unit Name", "Expiration Date", "Image URL", "Barcode"),
                "csvColumns", List.of("Name", "Description", "Category Name", "Unit Name", "Expiration Date", "Image URL", "Barcode"),
                "requiredFields", List.of("Name"),
                "optionalFields", List.of("Description", "Category Name", "Unit Name", "Expiration Date", "Image URL", "Barcode"),
                "dateFormat", "YYYY-MM-DD",
                "example", Map.of(
                    "Name", "Coca Cola 330ml",
                    "Description", "Nước ngọt Coca Cola lon 330ml",
                    "Category Name", "Đồ uống",
                    "Unit Name", "Lon",
                    "Expiration Date", "2025-12-31",
                    "Image URL", "https://example.com/image.jpg",
                    "Barcode", "1234567890123"
                )
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy template: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Lấy tất cả sản phẩm với thông tin tồn kho (kể cả sản phẩm chưa có tồn)
    @GetMapping("/with-stock")
    public ResponseEntity<?> getAllProductsWithStock(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long stockLocationId) {
        try {
            List<ProductDto> products = productService.getAllProductsWithStock(warehouseId, stockLocationId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Products with stock information retrieved successfully");
            response.put("data", products);
            response.put("total", products.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving products with stock: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Lấy danh sách sản phẩm sắp hết hàng và sản phẩm mới chưa nhập kho
    @GetMapping("/inventory-status")
    public ResponseEntity<?> getProductsInventoryStatus(
            @RequestParam(defaultValue = "10") int lowStockThreshold,
            @RequestParam(required = false) Long warehouseId) {
        try {
            Map<String, Object> result = productService.getProductsInventoryStatus(lowStockThreshold, warehouseId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Products inventory status retrieved successfully");
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving products inventory status: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Lấy danh sách sản phẩm sắp hết hàng
    @GetMapping("/low-stock")
    public ResponseEntity<?> getLowStockProducts(
            @RequestParam(defaultValue = "10") int threshold,
            @RequestParam(required = false) Long warehouseId) {
        try {
            List<ProductDto> lowStockProducts = productService.getLowStockProducts(threshold, warehouseId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Low stock products retrieved successfully");
            response.put("data", lowStockProducts);
            response.put("total", lowStockProducts.size());
            response.put("threshold", threshold);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving low stock products: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

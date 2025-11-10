package com.smartretail.serviceproduct.service;

import com.smartretail.serviceproduct.dto.ProductDto;
import com.smartretail.serviceproduct.dto.ProductUnitInfo;
import com.smartretail.serviceproduct.model.Product;
import com.smartretail.serviceproduct.model.ProductCategory;
import com.smartretail.serviceproduct.model.BarcodeMapping;
import com.smartretail.serviceproduct.model.ProductUnit;
import com.smartretail.serviceproduct.model.PriceList;
import com.smartretail.serviceproduct.model.Unit;
import com.smartretail.serviceproduct.repository.ProductRepository;
import com.smartretail.serviceproduct.repository.ProductCategoryRepository;
import com.smartretail.serviceproduct.repository.ProductUnitRepository;
import com.smartretail.serviceproduct.repository.PriceListRepository;
import com.smartretail.serviceproduct.repository.UnitRepository;
import com.smartretail.serviceproduct.repository.BarcodeMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private ProductUnitRepository productUnitRepository;

    @Autowired
    private PriceListRepository priceListRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private BarcodeMappingRepository barcodeMappingRepository;

    @Value("${inventory.service.name:inventory-service}")
    private String inventoryServiceName;

    public ProductDto createOrUpsertAndInbound(ProductDto incoming, Integer quantity, Long warehouseId, Long stockLocationId) {
        if (incoming == null) throw new IllegalArgumentException("Product payload is required");
        if (warehouseId == null || stockLocationId == null) {
            throw new IllegalArgumentException("warehouseId and stockLocationId are required");
        }

        // find or create product by name + category
        Product product = productRepository.findByNameAndCategoryId(incoming.getName(), incoming.getCategoryId())
                .orElseGet(() -> {
                    ProductDto created = createProduct(incoming);
                    return productRepository.findById(created.getId()).orElseThrow();
                });

        // ensure unit exists on product
        Long unitId = incoming.getDefaultUnitId();
        if (unitId == null) {
            throw new RuntimeException("Unit Name/Id is required in import");
        }
        Unit unit = unitRepository.findById(unitId).orElseThrow(() -> new RuntimeException("Unit not found: " + unitId));

        ProductUnit productUnit = productUnitRepository.findByProductAndUnit(product.getId(), unit.getId())
                .orElseGet(() -> {
                    ProductUnit pu = new ProductUnit();
                    pu.setProduct(product);
                    pu.setUnit(unit);
                    pu.setConversionRate(calculateConversionRate(unit));
                    pu.setIsDefault(false);
                    pu.setActive(true);
                    return productUnitRepository.save(pu);
                });

        // call inventory inbound
        if (quantity != null && quantity > 0) {
            String url = "http://" + inventoryServiceName + "/api/inventory/inbound/process";
            Map<String, Object> payload = new HashMap<>();
            payload.put("warehouseId", warehouseId);
            payload.put("stockLocationId", stockLocationId);
            payload.put("productUnitId", productUnit.getId());
            payload.put("quantity", quantity);
            payload.put("transactionDate", java.time.LocalDateTime.now().toString());
            payload.put("referenceNumber", "IMPORT-PRODUCT-EXCEL");
            payload.put("note", "Import from product-service");
            restTemplate.postForEntity(url, payload, Map.class);
        }

        return convertToDto(product);
    }

    // Create new product
    public ProductDto createProduct(ProductDto productDto) {
        ProductCategory category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + productDto.getCategoryId()));

        if (productRepository.existsByNameAndCategoryId(productDto.getName(), productDto.getCategoryId())) {
            throw new RuntimeException("Product with name '" + productDto.getName() + "' already exists in this category");
        }

        // Kiểm tra mã sản phẩm trùng trước khi tạo sản phẩm
        String requestedCode = productDto.getCode();
        if (requestedCode != null && !requestedCode.trim().isEmpty()) {
            String productCode = requestedCode.trim();
            if (productRepository.existsByCode(productCode)) {
                throw new RuntimeException("Mã sản phẩm đã tồn tại: " + productCode);
            }
        }

        Product product = new Product();
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setExpirationDate(productDto.getExpirationDate());
        product.setCategory(category);

        // Set code nếu có
        if (requestedCode != null && !requestedCode.trim().isEmpty()) {
            product.setCode(requestedCode.trim());
        }

        Product savedProduct = productRepository.save(product);

        // Nếu có defaultUnitId hoặc cần tạo đơn vị mặc định
        if (productDto.getDefaultUnitId() != null || productDto.getBarcodes() != null) {
            Long defaultUnitId = productDto.getDefaultUnitId();
            if (defaultUnitId != null) {
                createDefaultProductUnit(savedProduct, defaultUnitId);
            }
        }

        return convertToDto(savedProduct);
    }

        // Tự động tạo đơn vị cơ bản cho sản phẩm mới (không tạo giá ban đầu)
    private ProductUnit createDefaultProductUnit(Product product, Long defaultUnitId) {
        try {
            // Lấy đơn vị được chọn hoặc đơn vị mặc định
            Unit selectedUnit;
            if (defaultUnitId != null) {
                selectedUnit = unitRepository.findById(defaultUnitId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn vị với ID: " + defaultUnitId));

                // Kiểm tra đơn vị có active không
                if (!selectedUnit.getActive()) {
                    throw new RuntimeException("Đơn vị với ID " + defaultUnitId + " không hoạt động");
                }
            } else {
                // Nếu không chọn đơn vị, dùng đơn vị mặc định (Lon)
                selectedUnit = unitRepository.findByActiveTrue()
                        .stream()
                        .filter(unit -> unit.getIsDefault() != null && unit.getIsDefault())
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn vị mặc định"));
            }

            // Tự động tính conversionRate dựa trên đơn vị
            int conversionRate = calculateConversionRate(selectedUnit);

            // Tạo ProductUnit với đơn vị được chọn
            ProductUnit productUnit = new ProductUnit(product, selectedUnit, conversionRate, true);
            ProductUnit savedProductUnit = productUnitRepository.save(productUnit);

            // Không tạo giá ban đầu - giá sẽ được set riêng qua PriceController
            System.out.println("✅ Đã tạo đơn vị " + selectedUnit.getName() + " cho sản phẩm " + product.getName() +
                             " (chưa có giá - cần set giá riêng)");

            return savedProductUnit;
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi tạo đơn vị và giá cơ bản: " + e.getMessage());
            throw new RuntimeException("Không thể tạo đơn vị và giá cơ bản: " + e.getMessage());
        }
    }

        // Tự động tính conversionRate dựa trên đơn vị
    private int calculateConversionRate(Unit unit) {
        String unitName = unit.getName().toLowerCase();
        String description = unit.getDescription() != null ? unit.getDescription().toLowerCase() : "";

        // Dựa vào tên và mô tả đơn vị để tính conversionRate
        if (unitName.contains("thùng") || description.contains("24 lon")) {
            return 24; // 1 thùng = 24 lon (số lượng)
        } else if (unitName.contains("chai") || description.contains("500ml")) {
            return 1; // 1 chai = 1 chai (không đổi)
        } else if (unitName.contains("gói") || unitName.contains("hộp")) {
            return 1; // 1 gói/hộp = 1 gói/hộp
        } else {
            return 1; // Mặc định
        }
    }

    // Get all products with pagination and filters
    public Page<ProductDto> getAllProducts(String name, Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findActiveProductsWithFilters(name, categoryId, pageable);
        return products.map(this::convertToDto);
    }

    // Get all products (including those with inactive productUnits)
    public Page<ProductDto> getAllProductsIncludingInactive(String name, Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findAllProductsWithFilters(name, categoryId, pageable);
        return products.map(product -> convertToDtoIncludingInactive(product));
    }

    // Get product by ID
    public Optional<ProductDto> getProductById(Long id) {
        Optional<Product> product = productRepository.findById(id);
        return product.map(this::convertToDto);
    }

    // Get product by barcode/QR code (mapped to ProductUnit)
    public ProductDto getProductByBarcode(String code) {
        String normalized = code == null ? null : code.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("Code is required");
        }
        BarcodeMapping mapping = barcodeMappingRepository.findByCode(normalized)
                .orElseThrow(() -> new RuntimeException("Code not found: " + normalized));
        ProductUnit selectedPu = mapping.getProductUnit();
        Product product = selectedPu.getProduct();
        ProductDto dto = convertToDto(product);
        // Ưu tiên đơn vị trùng với mã quét: đưa lên đầu danh sách
        java.util.List<ProductUnitInfo> infos = dto.getProductUnits();
        if (infos != null) {
            infos.sort((a, b) -> a.getId().equals(selectedPu.getId()) ? -1 : (b.getId().equals(selectedPu.getId()) ? 1 : 0));
        }
        return dto;
    }

    // Get product by product code (Product.code)
    public ProductDto getProductByCode(String productCode) {
        String normalized = productCode == null ? null : productCode.trim();
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("Product code is required");
        }
        Product product = productRepository.findByCode(normalized)
                .orElseThrow(() -> new RuntimeException("Product code not found: " + normalized));
        return convertToDto(product);
    }

    // Removed direct product code lookup (Product.code removed)

    // ===== Barcode admin APIs =====
    public com.smartretail.serviceproduct.dto.BarcodeDto addBarcode(Long productUnitId, String code, String type) {
        if (code == null || code.trim().isEmpty()) {
            throw new RuntimeException("Code is required");
        }
        String normalized = code.trim();
        if (barcodeMappingRepository.findByCode(normalized).isPresent()) {
            throw new RuntimeException("Barcode already exists: " + normalized);
        }
        ProductUnit pu = productUnitRepository.findById(productUnitId)
                .orElseThrow(() -> new RuntimeException("ProductUnit not found: " + productUnitId));
        BarcodeMapping bm = new BarcodeMapping();
        bm.setProductUnit(pu);
        bm.setCode(normalized);
        bm.setType(type);
        BarcodeMapping saved = barcodeMappingRepository.save(bm);

        // Convert to DTO
        return new com.smartretail.serviceproduct.dto.BarcodeDto(
            saved.getId(),
            saved.getProductUnit().getId(),
            saved.getCode(),
            saved.getType(),
            saved.getCreatedAt()
        );
    }

    public void deleteBarcode(Long id) {
        if (!barcodeMappingRepository.existsById(id)) {
            throw new RuntimeException("Barcode not found: " + id);
        }
        barcodeMappingRepository.deleteById(id);
    }

    public List<com.smartretail.serviceproduct.dto.BarcodeDto> getBarcodesByProductId(Long productId) {
        List<BarcodeMapping> barcodes = barcodeMappingRepository.findByProductUnit_Product_Id(productId);
        return barcodes.stream()
                .map(bm -> new com.smartretail.serviceproduct.dto.BarcodeDto(
                    bm.getId(),
                    bm.getProductUnit().getId(),
                    bm.getCode(),
                    bm.getType(),
                    bm.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public List<com.smartretail.serviceproduct.dto.BarcodeDto> getBarcodesByProductUnitId(Long productUnitId) {
        List<BarcodeMapping> barcodes = barcodeMappingRepository.findByProductUnit_Id(productUnitId);
        return barcodes.stream()
                .map(bm -> new com.smartretail.serviceproduct.dto.BarcodeDto(
                        bm.getId(),
                        bm.getProductUnit().getId(),
                        bm.getCode(),
                        bm.getType(),
                        bm.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public com.smartretail.serviceproduct.dto.BarcodeDto updateBarcode(Long barcodeId, String newCode, String newType) {
        BarcodeMapping bm = barcodeMappingRepository.findById(barcodeId)
                .orElseThrow(() -> new RuntimeException("Barcode not found: " + barcodeId));
        if (newCode != null && !newCode.trim().isEmpty()) {
            if (!bm.getCode().equals(newCode) && barcodeMappingRepository.existsByCode(newCode)) {
                throw new RuntimeException("Barcode already exists: " + newCode);
            }
            bm.setCode(newCode.trim());
        }
        if (newType != null) {
            bm.setType(newType);
        }
        BarcodeMapping saved = barcodeMappingRepository.save(bm);
        return new com.smartretail.serviceproduct.dto.BarcodeDto(
                saved.getId(), saved.getProductUnit().getId(), saved.getCode(), saved.getType(), saved.getCreatedAt()
        );
    }


    // Update product
    public Optional<ProductDto> updateProduct(Long id, ProductDto productDto) {
        Optional<Product> existingProduct = productRepository.findById(id);
        if (existingProduct.isPresent()) {
            Product product = existingProduct.get();

            // Check if category is being changed
            if (!product.getCategory().getId().equals(productDto.getCategoryId())) {
                ProductCategory newCategory = categoryRepository.findById(productDto.getCategoryId())
                        .orElseThrow(() -> new RuntimeException("Category not found with id: " + productDto.getCategoryId()));
                product.setCategory(newCategory);
            }

            // Check if name conflicts in the same category
            if (!product.getName().equals(productDto.getName()) &&
                productRepository.existsByNameAndCategoryId(productDto.getName(), productDto.getCategoryId())) {
                throw new RuntimeException("Product with name '" + productDto.getName() + "' already exists in this category");
            }

            // Check if code conflicts (if code is being changed)
            String newCode = productDto.getCode();
            if (newCode != null && !newCode.trim().isEmpty()) {
                String trimmedCode = newCode.trim();
                String currentCode = product.getCode();

                // Only check for conflicts if the code is actually changing
                if (!trimmedCode.equals(currentCode)) {
                    if (productRepository.existsByCode(trimmedCode)) {
                        throw new RuntimeException("Mã sản phẩm đã tồn tại: " + trimmedCode);
                    }
                }
            }

            product.setName(productDto.getName());
            product.setDescription(productDto.getDescription());
            product.setExpirationDate(productDto.getExpirationDate());

            // Update code if provided
            if (newCode != null && !newCode.trim().isEmpty()) {
                product.setCode(newCode.trim());
            } else if (newCode != null && newCode.trim().isEmpty()) {
                // If empty string is provided, set code to null
                product.setCode(null);
            }

            Product updatedProduct = productRepository.save(product);
            return Optional.of(convertToDto(updatedProduct));
        }
        return Optional.empty();
    }

    // Delete product (soft delete)
    public boolean deleteProduct(Long id) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            Product prod = product.get();
            prod.setActive(false);
            productRepository.save(prod);
            return true;
        }
        return false;
    }

    // Search products by name
    public List<ProductDto> searchProductsByName(String searchTerm) {
        List<Product> products = productRepository.searchProductsByName(searchTerm);
        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Get products by category
    public List<ProductDto> getProductsByCategory(Long categoryId) {
        List<Product> products = productRepository.findByCategoryIdAndActiveTrue(categoryId);
        return products.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Lấy tất cả sản phẩm có trong 1 kho (có tồn kho > 0 hoặc có bản ghi tồn)
    public List<ProductDto> getProductsByWarehouse(Long warehouseId) {
        // Gọi inventory-service để lấy danh sách tồn theo warehouse
        String url = "http://" + inventoryServiceName + "/api/inventory/stock?warehouseId=" + warehouseId;
        StockBalanceDto[] stockBalances = restTemplate.getForObject(url, StockBalanceDto[].class);

        if (stockBalances == null || stockBalances.length == 0) {
            return java.util.Collections.emptyList();
        }

        // Lấy productIds từ productUnitId -> truy theo ProductUnit
        java.util.Set<Long> productIds = new java.util.HashSet<>();
        for (StockBalanceDto sb : stockBalances) {
            if (sb == null) continue;
            Long productUnitId = sb.productUnitId;
            if (productUnitId == null) continue;
            com.smartretail.serviceproduct.model.ProductUnit pu = productUnitRepository.findById(productUnitId).orElse(null);
            if (pu != null && pu.getProduct() != null && Boolean.TRUE.equals(pu.getActive())) {
                productIds.add(pu.getProduct().getId());
            }
        }

        if (productIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<Product> products = productRepository.findAllById(productIds);
        return products.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Convert entity to DTO
    private ProductDto convertToDto(Product product) {
        ProductDto dto = new ProductDto(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getExpirationDate(),
            product.getCategory().getId(),
            product.getCategory().getName(),
            product.getCreatedAt(),
            product.getUpdatedAt(),
            product.getActive()
        );

        // Set product code
        dto.setCode(product.getCode());

        // Lấy thông tin về đơn vị và giá hiện tại
        dto.setProductUnits(getProductUnitInfo(product.getId()));

        // Gắn list barcode hiện có của sản phẩm (tổng hợp từ các ProductUnit)
        try {
            List<com.smartretail.serviceproduct.dto.BarcodeDto> barcodes = getBarcodesByProductId(product.getId());
            dto.setBarcodeList(barcodes);

            // Đồng thời đưa barcode vào field input `barcodes` để UI hiển thị/sửa trực tiếp
            java.util.List<ProductDto.BarcodeInput> editableBarcodes = new java.util.ArrayList<>();
            for (com.smartretail.serviceproduct.dto.BarcodeDto b : barcodes) {
                ProductDto.BarcodeInput bi = new ProductDto.BarcodeInput();
                bi.code = b.getCode();
                bi.type = b.getType();
                editableBarcodes.add(bi);
            }
            dto.setBarcodes(editableBarcodes);
        } catch (Exception ignored) {}

        return dto;
    }

    // Convert entity to DTO including inactive productUnits
    private ProductDto convertToDtoIncludingInactive(Product product) {
        ProductDto dto = new ProductDto(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getExpirationDate(),
            product.getCategory().getId(),
            product.getCategory().getName(),
            product.getCreatedAt(),
            product.getUpdatedAt(),
            product.getActive()
        );

        // Set product code
        dto.setCode(product.getCode());

        // Lấy thông tin về đơn vị và giá hiện tại (bao gồm cả inactive)
        dto.setProductUnits(getAllProductUnitInfo(product.getId()));

        // Gắn list barcode hiện có của sản phẩm (tổng hợp từ các ProductUnit)
        try {
            List<com.smartretail.serviceproduct.dto.BarcodeDto> barcodes = getBarcodesByProductId(product.getId());
            dto.setBarcodeList(barcodes);

            // Đồng thời đưa barcode vào field input `barcodes` để UI hiển thị/sửa trực tiếp
            java.util.List<ProductDto.BarcodeInput> editableBarcodes = new java.util.ArrayList<>();
            for (com.smartretail.serviceproduct.dto.BarcodeDto b : barcodes) {
                ProductDto.BarcodeInput bi = new ProductDto.BarcodeInput();
                bi.code = b.getCode();
                bi.type = b.getType();
                editableBarcodes.add(bi);
            }
            dto.setBarcodes(editableBarcodes);
        } catch (Exception ignored) {}

        return dto;
    }

    // DTO dùng để nhận dữ liệu tồn kho từ inventory-service
    private static class StockBalanceDto {
        public Long productUnitId;
        public Long warehouseId;
        public Integer quantity;
        public Integer availableQuantity;
    }
    // Lấy thông tin về đơn vị và giá của sản phẩm
    private java.util.List<ProductUnitInfo> getProductUnitInfo(Long productId) {
        java.util.List<ProductUnit> productUnits = productUnitRepository.findByProductIdAndActiveTrue(productId);
        java.util.List<ProductUnitInfo> result = new java.util.ArrayList<>();

        for (ProductUnit pu : productUnits) {
            // Lấy giá hiện tại cho đơn vị này - sử dụng thời gian hiện tại
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            System.out.println("🔍 Tìm giá cho ProductUnit ID: " + pu.getId() + " tại thời gian: " + now);

            java.util.List<PriceList> allPrices = priceListRepository.findByProductUnitIdAndActiveTrue(pu.getId());
            System.out.println("📊 Tất cả giá cho ProductUnit " + pu.getId() + ": " + allPrices.size() + " records");

            PriceList currentPrice = priceListRepository.findCurrentPricesByProductUnit(pu.getId())
                .stream()
                .findFirst()
                .orElse(null);

            System.out.println("💰 Giá hiện tại: " + (currentPrice != null ? currentPrice.getPrice() : "null"));

            ProductUnitInfo unitInfo = new ProductUnitInfo(
                pu.getId(),
                pu.getUnit().getId(),
                pu.getUnit().getName(),
                pu.getUnit().getDescription(),
                new java.math.BigDecimal(pu.getConversionRate()),
                currentPrice != null ? currentPrice.getPrice() : null,
                currentPrice != null && currentPrice.getPriceHeader() != null ? currentPrice.getPriceHeader().getTimeStart() : null,
                currentPrice != null && currentPrice.getPriceHeader() != null ? currentPrice.getPriceHeader().getTimeEnd() : null,
                pu.getIsDefault() != null ? pu.getIsDefault() : false
            );
            // Set imageUrl từ ProductUnit
            unitInfo.setImageUrl(pu.getImageUrl());

            // Gọi inventory-service để lấy tồn kho cho mỗi productUnit
            try {
                String stockUrl = "http://" + inventoryServiceName + "/api/inventory/stock/" + pu.getId();
                StockBalanceDto[] stocks = restTemplate.getForObject(stockUrl, StockBalanceDto[].class);
                if (stocks != null && stocks.length > 0) {
                    int totalQty = 0;
                    int totalAvail = 0;
                    for (StockBalanceDto sb : stocks) {
                        if (sb == null) continue;
                        Integer qty = sb.quantity;
                        Integer avail = sb.availableQuantity;
                        if (qty != null) totalQty += qty;
                        if (avail != null) totalAvail += avail;
                    }
                    unitInfo.setQuantity(totalQty);
                    unitInfo.setAvailableQuantity(totalAvail);
                }
            } catch (Exception ex) {
                // ignore stock errors to keep product listing responsive
            }

            result.add(unitInfo);
        }

        return result;
    }

    // Lấy thông tin về đơn vị và giá của sản phẩm (bao gồm cả inactive)
    private java.util.List<ProductUnitInfo> getAllProductUnitInfo(Long productId) {
        java.util.List<ProductUnit> productUnits = productUnitRepository.findByProductIdAndActiveTrue(productId);
        // Lấy thêm những productUnit inactive
        java.util.List<ProductUnit> inactiveUnits = productUnitRepository.findAll().stream()
                .filter(pu -> pu.getProduct().getId().equals(productId) && !Boolean.TRUE.equals(pu.getActive()))
                .collect(java.util.stream.Collectors.toList());
        productUnits.addAll(inactiveUnits);
        java.util.List<ProductUnitInfo> result = new java.util.ArrayList<>();

        for (ProductUnit pu : productUnits) {
            // Lấy giá hiện tại cho đơn vị này - sử dụng thời gian hiện tại
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            System.out.println("🔍 Tìm giá cho ProductUnit ID: " + pu.getId() + " tại thời gian: " + now);

            java.util.List<PriceList> allPrices = priceListRepository.findByProductUnitIdAndActiveTrue(pu.getId());
            System.out.println("📊 Tất cả giá cho ProductUnit " + pu.getId() + ": " + allPrices.size() + " records");

            PriceList currentPrice = priceListRepository.findCurrentPricesByProductUnit(pu.getId())
                .stream()
                .findFirst()
                .orElse(null);

            System.out.println("💰 Giá hiện tại: " + (currentPrice != null ? currentPrice.getPrice() : "null"));

            ProductUnitInfo unitInfo = new ProductUnitInfo(
                pu.getId(),
                pu.getUnit().getId(),
                pu.getUnit().getName(),
                pu.getUnit().getDescription(),
                new java.math.BigDecimal(pu.getConversionRate()),
                currentPrice != null ? currentPrice.getPrice() : null,
                pu.getIsDefault(),
                pu.getActive()
            );
            // Set imageUrl từ ProductUnit
            unitInfo.setImageUrl(pu.getImageUrl());

            // Gọi inventory-service để lấy tồn kho cho mỗi productUnit (bao gồm cả inactive)
            try {
                String stockUrl = "http://" + inventoryServiceName + "/api/inventory/stock/" + pu.getId();
                StockBalanceDto[] stocks = restTemplate.getForObject(stockUrl, StockBalanceDto[].class);
                if (stocks != null && stocks.length > 0) {
                    int totalQty = 0;
                    int totalAvail = 0;
                    for (StockBalanceDto sb : stocks) {
                        if (sb == null) continue;
                        Integer qty = sb.quantity;
                        Integer avail = sb.availableQuantity;
                        if (qty != null) totalQty += qty;
                        if (avail != null) totalAvail += avail;
                    }
                    unitInfo.setQuantity(totalQty);
                    unitInfo.setAvailableQuantity(totalAvail);
                } else {
                    unitInfo.setQuantity(0);
                    unitInfo.setAvailableQuantity(0);
                }
            } catch (Exception ex) {
                // ignore stock errors to keep product listing responsive
            }

            result.add(unitInfo);
        }

        return result;
    }

    // Lấy tất cả sản phẩm với thông tin tồn kho (kể cả sản phẩm chưa có tồn)
    public List<ProductDto> getAllProductsWithStock(Long warehouseId, Long stockLocationId) {
        try {
            // Lấy tất cả sản phẩm active
            List<Product> allProducts = productRepository.findByActiveTrue();

            // Lấy thông tin tồn kho từ inventory-service
            Map<Long, StockInfo> stockInfoMap = new HashMap<>();
            try {
                String url = "http://" + inventoryServiceName + "/api/inventory/stock";
                if (warehouseId != null) {
                    url += "?warehouseId=" + warehouseId;
                }
                if (stockLocationId != null) {
                    url += (warehouseId != null ? "&" : "?") + "stockLocationId=" + stockLocationId;
                }

                StockBalanceDto[] stockBalances = restTemplate.getForObject(url, StockBalanceDto[].class);
                if (stockBalances != null) {
                    for (StockBalanceDto sb : stockBalances) {
                        if (sb != null && sb.productUnitId != null) {
                            // Lấy productId từ productUnitId
                            ProductUnit pu = productUnitRepository.findById(sb.productUnitId).orElse(null);
                            if (pu != null && pu.getProduct() != null) {
                                Long productId = pu.getProduct().getId();
                                StockInfo stockInfo = stockInfoMap.getOrDefault(productId, new StockInfo());
                                stockInfo.totalQuantity += (sb.quantity != null ? sb.quantity : 0);
                                stockInfo.totalAvailable += (sb.availableQuantity != null ? sb.availableQuantity : 0);
                                stockInfoMap.put(productId, stockInfo);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to get stock information: {}", e.getMessage());
            }

            // Convert to DTO và gắn thông tin tồn kho
            return allProducts.stream()
                    .map(product -> {
                        ProductDto dto = convertToDto(product);

                        // Gắn thông tin tồn kho tổng hợp
                        StockInfo stockInfo = stockInfoMap.get(product.getId());
                        if (stockInfo != null) {
                            // Cập nhật quantity cho tất cả productUnits
                            if (dto.getProductUnits() != null) {
                                for (ProductUnitInfo unitInfo : dto.getProductUnits()) {
                                    unitInfo.setQuantity(stockInfo.totalQuantity);
                                    unitInfo.setAvailableQuantity(stockInfo.totalAvailable);
                                }
                            }
                        } else {
                            // Sản phẩm chưa có tồn kho - set quantity = 0
                            if (dto.getProductUnits() != null) {
                                for (ProductUnitInfo unitInfo : dto.getProductUnits()) {
                                    unitInfo.setQuantity(0);
                                    unitInfo.setAvailableQuantity(0);
                                }
                            }
                        }

                        return dto;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting all products with stock: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    // Helper class để tổng hợp thông tin tồn kho
    private static class StockInfo {
        int totalQuantity = 0;
        int totalAvailable = 0;
    }

    // Lấy danh sách sản phẩm sắp hết hàng và sản phẩm mới chưa nhập kho
    public Map<String, Object> getProductsInventoryStatus(int lowStockThreshold, Long warehouseId) {
        try {
            // Lấy tất cả sản phẩm active
            List<Product> allProducts = productRepository.findByActiveTrue();

            // Lấy thông tin tồn kho từ inventory-service - Tổng hợp theo productUnitId
            Map<Long, StockBalanceDto> stockBalanceMap = new HashMap<>();
            try {
                String url = "http://" + inventoryServiceName + "/api/inventory/stock";
                if (warehouseId != null) {
                    url += "?warehouseId=" + warehouseId;
                }

                StockBalanceDto[] stockBalances = restTemplate.getForObject(url, StockBalanceDto[].class);
                if (stockBalances != null) {
                    for (StockBalanceDto sb : stockBalances) {
                        if (sb != null && sb.productUnitId != null) {
                            // Tổng hợp theo productUnitId (không phải productId)
                            Long productUnitId = sb.productUnitId;
                            if (stockBalanceMap.containsKey(productUnitId)) {
                                // Cộng dồn nếu có nhiều bản ghi cho cùng 1 productUnitId
                                StockBalanceDto existing = stockBalanceMap.get(productUnitId);
                                existing.quantity = (existing.quantity != null ? existing.quantity : 0) +
                                                 (sb.quantity != null ? sb.quantity : 0);
                                existing.availableQuantity = (existing.availableQuantity != null ? existing.availableQuantity : 0) +
                                                           (sb.availableQuantity != null ? sb.availableQuantity : 0);
                            } else {
                                stockBalanceMap.put(productUnitId, sb);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to get stock information: {}", e.getMessage());
            }

            // Phân loại sản phẩm
            List<ProductDto> lowStockProducts = new java.util.ArrayList<>();
            List<ProductDto> newProducts = new java.util.ArrayList<>();
            List<ProductDto> normalProducts = new java.util.ArrayList<>();

            for (Product product : allProducts) {
                ProductDto dto = convertToDto(product);

                // Kiểm tra từng ProductUnit có tồn kho không
                boolean hasStock = false;
                boolean isLowStock = false;

                if (dto.getProductUnits() != null) {
                    for (ProductUnitInfo unitInfo : dto.getProductUnits()) {
                        Long productUnitId = unitInfo.getId();
                        StockBalanceDto stockBalance = stockBalanceMap.get(productUnitId);

                        if (stockBalance != null) {
                            // Có tồn kho - gán đúng số lượng cho từng ProductUnit
                            unitInfo.setQuantity(stockBalance.quantity != null ? stockBalance.quantity : 0);
                            unitInfo.setAvailableQuantity(stockBalance.availableQuantity != null ? stockBalance.availableQuantity : 0);
                            hasStock = true;

                            // Kiểm tra sắp hết hàng cho ProductUnit này
                            if (stockBalance.availableQuantity != null && stockBalance.availableQuantity <= lowStockThreshold) {
                                isLowStock = true;
                            }
                        } else {
                            // Không có tồn kho
                            unitInfo.setQuantity(0);
                            unitInfo.setAvailableQuantity(0);
                        }
                    }
                }

                // Phân loại sản phẩm
                if (hasStock) {
                    if (isLowStock) {
                        lowStockProducts.add(dto);
                    } else {
                        normalProducts.add(dto);
                    }
                } else {
                    // Sản phẩm mới chưa nhập kho
                    newProducts.add(dto);
                }
            }

            // Tạo kết quả
            Map<String, Object> result = new HashMap<>();
            result.put("lowStockProducts", lowStockProducts);
            result.put("newProducts", newProducts);
            result.put("normalProducts", normalProducts);
            result.put("summary", Map.of(
                "totalProducts", allProducts.size(),
                "lowStockCount", lowStockProducts.size(),
                "newProductsCount", newProducts.size(),
                "normalProductsCount", normalProducts.size(),
                "lowStockThreshold", lowStockThreshold
            ));

            return result;

        } catch (Exception e) {
            log.error("Error getting products inventory status: {}", e.getMessage());
            return Map.of(
                "lowStockProducts", new java.util.ArrayList<>(),
                "newProducts", new java.util.ArrayList<>(),
                "normalProducts", new java.util.ArrayList<>(),
                "summary", Map.of(
                    "totalProducts", 0,
                    "lowStockCount", 0,
                    "newProductsCount", 0,
                    "normalProductsCount", 0,
                    "lowStockThreshold", lowStockThreshold
                )
            );
        }
    }

    // Lấy danh sách sản phẩm sắp hết hàng
    public List<ProductDto> getLowStockProducts(int threshold, Long warehouseId) {
        try {
            // Gọi inventory-service để lấy danh sách tồn kho thấp
            String url = "http://" + inventoryServiceName + "/api/inventory/stock";
            if (warehouseId != null) {
                url += "?warehouseId=" + warehouseId;
            }

            StockBalanceDto[] stockBalances = restTemplate.getForObject(url, StockBalanceDto[].class);

            if (stockBalances == null || stockBalances.length == 0) {
                return java.util.Collections.emptyList();
            }

            // Lọc các sản phẩm có tồn kho <= threshold
            java.util.Set<Long> lowStockProductIds = new java.util.HashSet<>();
            for (StockBalanceDto sb : stockBalances) {
                if (sb == null) continue;

                // Kiểm tra availableQuantity <= threshold
                if (sb.availableQuantity != null && sb.availableQuantity <= threshold) {
                    Long productUnitId = sb.productUnitId;
                    if (productUnitId != null) {
                        com.smartretail.serviceproduct.model.ProductUnit pu =
                            productUnitRepository.findById(productUnitId).orElse(null);
                        if (pu != null && pu.getProduct() != null && Boolean.TRUE.equals(pu.getActive())) {
                            lowStockProductIds.add(pu.getProduct().getId());
                        }
                    }
                }
            }

            if (lowStockProductIds.isEmpty()) {
                return java.util.Collections.emptyList();
            }

            // Lấy thông tin sản phẩm
            List<Product> products = productRepository.findAllById(lowStockProductIds);
            return products.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting low stock products: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
}

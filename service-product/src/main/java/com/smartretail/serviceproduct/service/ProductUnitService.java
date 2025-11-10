package com.smartretail.serviceproduct.service;

import com.smartretail.serviceproduct.dto.ProductUnitDto;
import com.smartretail.serviceproduct.model.Product;
import com.smartretail.serviceproduct.model.ProductUnit;
import com.smartretail.serviceproduct.model.Unit;
import com.smartretail.serviceproduct.repository.ProductRepository;
import com.smartretail.serviceproduct.repository.ProductUnitRepository;
import com.smartretail.serviceproduct.repository.UnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductUnitService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductUnitRepository productUnitRepository;

    @Autowired
    private UnitRepository unitRepository;


    // Thêm đơn vị tính mới cho sản phẩm
    public ProductUnitDto addProductUnit(ProductUnitDto productUnitDto) {
        // Kiểm tra sản phẩm tồn tại
        Product product = productRepository.findById(productUnitDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productUnitDto.getProductId()));

        // Kiểm tra đơn vị tồn tại
        Unit unit = unitRepository.findById(productUnitDto.getUnitId())
                .orElseThrow(() -> new RuntimeException("Unit not found with id: " + productUnitDto.getUnitId()));

        // Kiểm tra đơn vị có active không
        if (!unit.getActive()) {
            throw new RuntimeException("Unit with ID " + productUnitDto.getUnitId() + " is not active");
        }

        // Kiểm tra xem đơn vị này đã tồn tại cho sản phẩm chưa
        if (productUnitRepository.existsByProductIdAndUnitId(productUnitDto.getProductId(), productUnitDto.getUnitId())) {
            throw new RuntimeException("Unit already exists for this product");
        }

        // Tự động tính conversionRate nếu không có
        Integer conversionRate = productUnitDto.getConversionRate();
        if (conversionRate == null) {
            conversionRate = calculateConversionRate(unit);
        }

        // Tạo ProductUnit mới
        ProductUnit productUnit = new ProductUnit();
        productUnit.setProduct(product);
        productUnit.setUnit(unit);
        productUnit.setConversionRate(conversionRate);
        productUnit.setIsDefault(productUnitDto.getIsDefault() != null ? productUnitDto.getIsDefault() : false);
        productUnit.setActive(true);

        ProductUnit savedProductUnit = productUnitRepository.save(productUnit);

        // Nếu đơn vị này được đánh dấu là mặc định, unset các đơn vị khác của cùng sản phẩm
        if (Boolean.TRUE.equals(savedProductUnit.getIsDefault())) {
            unsetOtherDefaults(savedProductUnit);
            // Đảm bảo conversionRate của default = 1
            if (savedProductUnit.getConversionRate() == null || savedProductUnit.getConversionRate() != 1) {
                savedProductUnit.setConversionRate(1);
                savedProductUnit = productUnitRepository.save(savedProductUnit);
            }
        }

        // Không tạo giá cơ bản - giá sẽ được set riêng qua PriceController
        System.out.println("✅ Đã thêm đơn vị " + unit.getName() + " cho sản phẩm " + product.getName() +
                         " (chưa có giá - cần set giá riêng)");

        // Cập nhật thông tin đơn vị
        productUnitDto.setId(savedProductUnit.getId());
        productUnitDto.setUnitName(unit.getName());
        productUnitDto.setUnitDescription(unit.getDescription());
        productUnitDto.setConversionRate(conversionRate);
        productUnitDto.setImageUrl(savedProductUnit.getImageUrl());

        return productUnitDto;
    }

    // Lấy danh sách đơn vị tính của sản phẩm
    public List<ProductUnitDto> getProductUnitsByProductId(Long productId) {
        List<ProductUnit> productUnits = productUnitRepository.findByProductIdAndActiveTrue(productId);
        return productUnits.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Lấy đơn vị tính theo ID (cho Order Service)
    public ProductUnitDto getProductUnitById(Long unitId) {
        ProductUnit productUnit = productUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("ProductUnit not found with id: " + unitId));

        if (!productUnit.getActive()) {
            throw new RuntimeException("ProductUnit with ID " + unitId + " is not active");
        }

        return convertToDto(productUnit);
    }

    // Cập nhật đơn vị tính
    public ProductUnitDto updateProductUnit(Long unitId, ProductUnitDto productUnitDto) {
        ProductUnit existingProductUnit = productUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("ProductUnit not found with id: " + unitId));

        // Cập nhật thông tin
        if (productUnitDto.getConversionRate() != null) {
            existingProductUnit.setConversionRate(productUnitDto.getConversionRate());
        }
        if (productUnitDto.getIsDefault() != null) {
            existingProductUnit.setIsDefault(productUnitDto.getIsDefault());
        }
        if (productUnitDto.getImageUrl() != null) {
            existingProductUnit.setImageUrl(productUnitDto.getImageUrl());
        }

        ProductUnit updatedProductUnit = productUnitRepository.save(existingProductUnit);

        // Nếu sau cập nhật là default thì unset các đơn vị khác và ép conversionRate = 1
        if (Boolean.TRUE.equals(updatedProductUnit.getIsDefault())) {
            unsetOtherDefaults(updatedProductUnit);
            if (updatedProductUnit.getConversionRate() == null || updatedProductUnit.getConversionRate() != 1) {
                updatedProductUnit.setConversionRate(1);
                updatedProductUnit = productUnitRepository.save(updatedProductUnit);
            }
        }
        return convertToDto(updatedProductUnit);
    }

    // Xóa đơn vị tính (soft delete)
    public boolean deleteProductUnit(Long unitId) {
        ProductUnit productUnit = productUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("ProductUnit not found with id: " + unitId));

        productUnit.setActive(false);
        productUnitRepository.save(productUnit);
        return true;
    }

    // Kích hoạt đơn vị tính
    public ProductUnitDto activateProductUnit(Long unitId) {
        ProductUnit productUnit = productUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("ProductUnit not found with id: " + unitId));

        productUnit.setActive(true);
        ProductUnit updatedProductUnit = productUnitRepository.save(productUnit);
        return convertToDto(updatedProductUnit);
    }

    // Tạm dừng đơn vị tính
    public ProductUnitDto deactivateProductUnit(Long unitId) {
        ProductUnit productUnit = productUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("ProductUnit not found with id: " + unitId));

        productUnit.setActive(false);
        ProductUnit updatedProductUnit = productUnitRepository.save(productUnit);
        return convertToDto(updatedProductUnit);
    }


    // Tự động tính conversionRate dựa trên đơn vị
    private Integer calculateConversionRate(Unit unit) {
        String unitName = unit.getName().toLowerCase();
        String description = unit.getDescription() != null ? unit.getDescription().toLowerCase() : "";

        if (unitName.contains("thùng") || description.contains("24 lon")) {
            return 24; // 1 thùng = 24 lon
        } else if (unitName.contains("chai") || description.contains("500ml")) {
            return 1; // 1 chai = 1 chai
        } else if (unitName.contains("gói") || unitName.contains("hộp")) {
            return 1; // 1 gói/hộp = 1 gói/hộp
        } else {
            return 1; // Mặc định
        }
    }

    // Đặt 1 đơn vị làm mặc định cho sản phẩm
    public ProductUnitDto makeDefaultUnit(Long productId, Long productUnitId) {
        ProductUnit productUnit = productUnitRepository.findById(productUnitId)
                .orElseThrow(() -> new RuntimeException("ProductUnit not found with id: " + productUnitId));

        if (!productUnit.getProduct().getId().equals(productId)) {
            throw new RuntimeException("ProductUnit does not belong to the specified product");
        }

        productUnit.setIsDefault(true);
        productUnit.setConversionRate(1);
        productUnit = productUnitRepository.save(productUnit);
        unsetOtherDefaults(productUnit);
        return convertToDto(productUnit);
    }

    private void unsetOtherDefaults(ProductUnit defaultUnit) {
        List<ProductUnit> units = productUnitRepository.findAllActiveByProductId(defaultUnit.getProduct().getId());
        for (ProductUnit pu : units) {
            if (!pu.getId().equals(defaultUnit.getId()) && Boolean.TRUE.equals(pu.getIsDefault())) {
                pu.setIsDefault(false);
                productUnitRepository.save(pu);
            }
        }
    }

    // Convert entity to DTO
    private ProductUnitDto convertToDto(ProductUnit productUnit) {
        ProductUnitDto dto = new ProductUnitDto();
        dto.setId(productUnit.getId());
        dto.setProductId(productUnit.getProduct().getId());
        dto.setProductName(productUnit.getProduct().getName());
        dto.setUnitId(productUnit.getUnit().getId());
        dto.setUnitName(productUnit.getUnit().getName());
        dto.setUnitDescription(productUnit.getUnit().getDescription());
        dto.setConversionRate(productUnit.getConversionRate());
        dto.setIsDefault(productUnit.getIsDefault());
        dto.setActive(productUnit.getActive());
        dto.setImageUrl(productUnit.getImageUrl());
        return dto;
    }
}

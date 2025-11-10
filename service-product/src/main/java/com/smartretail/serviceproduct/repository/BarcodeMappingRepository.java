package com.smartretail.serviceproduct.repository;

import com.smartretail.serviceproduct.model.BarcodeMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BarcodeMappingRepository extends JpaRepository<BarcodeMapping, Long> {
    Optional<BarcodeMapping> findByCode(String code);
    List<BarcodeMapping> findByProductUnit_Product_Id(Long productId);
    List<BarcodeMapping> findByProductUnit_Id(Long productUnitId);
    boolean existsByCode(String code);
}



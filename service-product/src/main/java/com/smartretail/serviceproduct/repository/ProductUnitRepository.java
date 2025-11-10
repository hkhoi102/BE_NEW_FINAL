package com.smartretail.serviceproduct.repository;

import com.smartretail.serviceproduct.model.ProductUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductUnitRepository extends JpaRepository<ProductUnit, Long> {

    List<ProductUnit> findByProductIdAndActiveTrue(Long productId);

    List<ProductUnit> findByProductId(Long productId);

    List<ProductUnit> findByUnitIdAndActiveTrue(Long unitId);

    @Query("SELECT pu FROM ProductUnit pu WHERE pu.product.id = :productId AND pu.unit.id = :unitId AND pu.active = true")
    Optional<ProductUnit> findByProductAndUnit(@Param("productId") Long productId, @Param("unitId") Long unitId);

    List<ProductUnit> findByActiveTrue();

    // Kiểm tra xem đơn vị đã tồn tại cho sản phẩm chưa
    boolean existsByProductIdAndUnitId(Long productId, Long unitId);

    @Query("SELECT pu FROM ProductUnit pu WHERE pu.product.id = :productId AND pu.active = true")
    List<ProductUnit> findAllActiveByProductId(@Param("productId") Long productId);

}

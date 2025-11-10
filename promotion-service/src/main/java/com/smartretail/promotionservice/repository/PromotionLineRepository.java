package com.smartretail.promotionservice.repository;

import com.smartretail.promotionservice.model.PromotionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionLineRepository extends JpaRepository<PromotionLine, Long> {

    // Tìm tất cả promotion lines của một promotion header (chỉ active)
    List<PromotionLine> findByPromotionHeaderIdAndActiveTrue(Long promotionHeaderId);

    // Tìm tất cả promotion lines của một promotion header (cả active và inactive)
    List<PromotionLine> findByPromotionHeaderId(Long promotionHeaderId);

    // Tìm promotion lines theo target type và target id
    @Query("SELECT pl FROM PromotionLine pl WHERE pl.active = true " +
           "AND pl.targetType = :targetType AND pl.targetId = :targetId")
    List<PromotionLine> findByTargetTypeAndTargetId(@Param("targetType") PromotionLine.TargetType targetType,
                                                   @Param("targetId") Long targetId);

    // Tìm promotion lines theo target type
    List<PromotionLine> findByTargetTypeAndActiveTrue(PromotionLine.TargetType targetType);

    // Tìm promotion lines cho một sản phẩm cụ thể (đơn giản hóa)
    @Query("SELECT pl FROM PromotionLine pl WHERE pl.active = true " +
           "AND (pl.targetType = 'PRODUCT' AND pl.targetId = :productId) " +
           "OR (pl.targetType = 'CATEGORY' AND pl.targetId = :categoryId)")
    List<PromotionLine> findPromotionsForProduct(@Param("productId") Long productId,
                                                @Param("categoryId") Long categoryId);
}

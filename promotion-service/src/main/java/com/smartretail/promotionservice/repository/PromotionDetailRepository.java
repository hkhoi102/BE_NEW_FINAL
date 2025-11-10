package com.smartretail.promotionservice.repository;

import com.smartretail.promotionservice.model.PromotionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionDetailRepository extends JpaRepository<PromotionDetail, Long> {

    // Tìm tất cả promotion details của một promotion line (chỉ active)
    List<PromotionDetail> findByPromotionLineIdAndActiveTrue(Long promotionLineId);

    // Tìm tất cả promotion details của một promotion line (cả active và inactive)
    List<PromotionDetail> findByPromotionLineId(Long promotionLineId);

    // Tìm promotion details theo promotion line ids
    List<PromotionDetail> findByPromotionLineIdInAndActiveTrue(List<Long> promotionLineIds);
}

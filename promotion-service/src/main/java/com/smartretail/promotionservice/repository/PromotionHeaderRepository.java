package com.smartretail.promotionservice.repository;

import com.smartretail.promotionservice.model.PromotionHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PromotionHeaderRepository extends JpaRepository<PromotionHeader, Long> {

    // Tìm tất cả chương trình khuyến mãi đang hoạt động
    List<PromotionHeader> findByActiveTrue();

    // ĐÃ BỎ type ở header: không còn API findByType...

    // Tìm chương trình khuyến mãi đang hiệu lực trong khoảng thời gian
    @Query("SELECT ph FROM PromotionHeader ph WHERE ph.active = true " +
           "AND ph.startDate <= :currentDate AND ph.endDate >= :currentDate")
    List<PromotionHeader> findActivePromotionsByDate(@Param("currentDate") LocalDate currentDate);

    // Tìm chương trình khuyến mãi theo tên (tìm kiếm mờ)
    @Query("SELECT ph FROM PromotionHeader ph WHERE ph.active = true " +
           "AND LOWER(ph.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<PromotionHeader> findByNameContainingIgnoreCase(@Param("name") String name);

    // Kiểm tra xem có chương trình khuyến mãi nào trùng tên không
    boolean existsByNameAndActiveTrue(String name);
}

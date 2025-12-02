package com.smartretail.serviceproduct.repository;

import com.smartretail.serviceproduct.model.PriceList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface PriceListRepository extends JpaRepository<PriceList, Long> {

    List<PriceList> findByProductUnitIdAndActiveTrue(Long productUnitId);

    @Query("SELECT p FROM PriceList p LEFT JOIN p.priceHeader h " +
           "WHERE p.productUnit.id = :productUnitId AND p.active = true " +
           "AND (h IS NULL OR (h.active = true AND " +
           "(:time IS NULL OR (h.timeStart IS NULL OR h.timeStart <= :time) " +
           "AND (h.timeEnd IS NULL OR h.timeEnd > :time)))) " +
           "ORDER BY p.createdAt DESC")
    List<PriceList> findCurrentPricesByProductUnit(
        @Param("productUnitId") Long productUnitId,
        @Param("time") java.time.LocalDateTime time
    );

    @Query("SELECT p FROM PriceList p LEFT JOIN p.priceHeader h " +
           "WHERE p.productUnit.product.id = :productId AND p.active = true " +
           "AND (h IS NULL OR h.active = true) " +
           "ORDER BY p.createdAt DESC")
    List<PriceList> findPriceHistoryByProduct(@Param("productId") Long productId);

    Optional<PriceList> findTopByProductUnitIdAndActiveTrueOrderByCreatedAtDesc(Long productUnitId);

    @Query("SELECT p FROM PriceList p WHERE p.priceHeader.id = :headerId AND p.active = true ORDER BY p.createdAt DESC")
    List<PriceList> findByHeaderId(@Param("headerId") Long headerId);

    @Query("SELECT p FROM PriceList p WHERE p.priceHeader.id = :headerId AND p.productUnit.id IN :productUnitIds AND p.active = true")
    List<PriceList> findByPriceHeaderIdAndProductUnitIdIn(@Param("headerId") Long headerId, @Param("productUnitIds") List<Long> productUnitIds);
}

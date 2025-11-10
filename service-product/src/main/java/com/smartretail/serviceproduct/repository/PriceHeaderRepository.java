package com.smartretail.serviceproduct.repository;

import com.smartretail.serviceproduct.model.PriceHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceHeaderRepository extends JpaRepository<PriceHeader, Long> {

    List<PriceHeader> findByActiveTrue();

    @Query("SELECT h FROM PriceHeader h WHERE h.active = true " +
           "AND (:time IS NULL OR (h.timeStart IS NULL OR h.timeStart <= :time) AND (h.timeEnd IS NULL OR h.timeEnd > :time)) " +
           "ORDER BY h.timeStart DESC NULLS LAST")
    List<PriceHeader> findCurrentHeaders(@Param("time") java.time.LocalDateTime time);
}



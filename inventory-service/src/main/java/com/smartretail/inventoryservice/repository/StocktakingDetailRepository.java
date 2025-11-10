package com.smartretail.inventoryservice.repository;

import com.smartretail.inventoryservice.model.StocktakingDetail;
import com.smartretail.inventoryservice.model.Stocktaking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StocktakingDetailRepository extends JpaRepository<StocktakingDetail, Long> {
    List<StocktakingDetail> findByStocktaking(Stocktaking stocktaking);
}



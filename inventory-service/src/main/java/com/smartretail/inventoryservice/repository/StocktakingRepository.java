package com.smartretail.inventoryservice.repository;

import com.smartretail.inventoryservice.model.Stocktaking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StocktakingRepository extends JpaRepository<Stocktaking, Long> {
}



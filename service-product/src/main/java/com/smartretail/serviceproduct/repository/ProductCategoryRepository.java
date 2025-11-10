package com.smartretail.serviceproduct.repository;

import com.smartretail.serviceproduct.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    Optional<ProductCategory> findByName(String name);

    List<ProductCategory> findByActiveTrue();

    @Query("SELECT c FROM ProductCategory c WHERE c.active = true ORDER BY c.name")
    List<ProductCategory> findAllActiveCategories();

    boolean existsByName(String name);
}

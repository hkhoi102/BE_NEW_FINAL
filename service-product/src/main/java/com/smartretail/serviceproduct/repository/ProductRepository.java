package com.smartretail.serviceproduct.repository;

import com.smartretail.serviceproduct.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrue();

    Page<Product> findByActiveTrue(Pageable pageable);

    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(:name IS NULL OR p.name LIKE %:name%) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId)")
    Page<Product> findActiveProductsWithFilters(
        @Param("name") String name,
        @Param("categoryId") Long categoryId,
        Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.name LIKE %:searchTerm%")
    List<Product> searchProductsByName(@Param("searchTerm") String searchTerm);

    // Lấy tất cả sản phẩm (kể cả có productUnit inactive)
    @Query("SELECT p FROM Product p WHERE " +
           "(:name IS NULL OR p.name LIKE %:name%) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId)")
    Page<Product> findAllProductsWithFilters(
        @Param("name") String name,
        @Param("categoryId") Long categoryId,
        Pageable pageable
    );

    boolean existsByNameAndCategoryId(String name, Long categoryId);

    Optional<Product> findByNameAndCategoryId(String name, Long categoryId);

    boolean existsByCode(String code);

    Optional<Product> findByCode(String code);
}

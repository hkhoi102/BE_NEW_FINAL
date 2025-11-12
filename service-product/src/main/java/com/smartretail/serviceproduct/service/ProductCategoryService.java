package com.smartretail.serviceproduct.service;

import com.smartretail.serviceproduct.dto.ProductCategoryDto;
import com.smartretail.serviceproduct.model.ProductCategory;
import com.smartretail.serviceproduct.repository.ProductCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductCategoryService {

    @Autowired
    private ProductCategoryRepository categoryRepository;

    // Create new category
    public ProductCategoryDto createCategory(ProductCategoryDto categoryDto) {
        if (categoryRepository.existsByName(categoryDto.getName())) {
            throw new RuntimeException("Category with name '" + categoryDto.getName() + "' already exists");
        }

        ProductCategory category = new ProductCategory();
        category.setName(categoryDto.getName());
        category.setDescription(categoryDto.getDescription());
        category.setImageUrl(categoryDto.getImageUrl());

        ProductCategory savedCategory = categoryRepository.save(category);
        return convertToDto(savedCategory);
    }

    // Get all active categories
    public List<ProductCategoryDto> getAllCategories() {
        List<ProductCategory> categories = categoryRepository.findAllActiveCategories();
        return categories.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Get category by ID
    public Optional<ProductCategoryDto> getCategoryById(Long id) {
        Optional<ProductCategory> category = categoryRepository.findById(id);
        return category.map(this::convertToDto);
    }

    // Update category
    public Optional<ProductCategoryDto> updateCategory(Long id, ProductCategoryDto categoryDto) {
        Optional<ProductCategory> existingCategory = categoryRepository.findById(id);
        if (existingCategory.isPresent()) {
            ProductCategory category = existingCategory.get();

            String updatedName = categoryDto.getName() != null ? categoryDto.getName() : category.getName();
            String updatedDescription = categoryDto.getDescription() != null ? categoryDto.getDescription() : category.getDescription();
            String updatedImageUrl = categoryDto.getImageUrl() != null ? categoryDto.getImageUrl() : category.getImageUrl();

            // Check if name is being changed and if it conflicts with existing names
            if (!category.getName().equals(updatedName) &&
                categoryRepository.existsByName(updatedName)) {
                throw new RuntimeException("Category with name '" + updatedName + "' already exists");
            }

            category.setName(updatedName);
            category.setDescription(updatedDescription);
            category.setImageUrl(updatedImageUrl);

            ProductCategory updatedCategory = categoryRepository.save(category);
            return Optional.of(convertToDto(updatedCategory));
        }
        return Optional.empty();
    }

    // Delete category (soft delete)
    public boolean deleteCategory(Long id) {
        Optional<ProductCategory> category = categoryRepository.findById(id);
        if (category.isPresent()) {
            ProductCategory cat = category.get();
            cat.setActive(false);
            categoryRepository.save(cat);
            return true;
        }
        return false;
    }

    // Convert entity to DTO
    private ProductCategoryDto convertToDto(ProductCategory category) {
        return new ProductCategoryDto(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getCreatedAt(),
            category.getImageUrl()
        );
    }
}

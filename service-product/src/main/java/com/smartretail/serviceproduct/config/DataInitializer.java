package com.smartretail.serviceproduct.config;

import com.smartretail.serviceproduct.model.ProductCategory;
import com.smartretail.serviceproduct.model.Unit;
import com.smartretail.serviceproduct.repository.ProductCategoryRepository;
import com.smartretail.serviceproduct.repository.UnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Override
    public void run(String... args) throws Exception {
        // Sample data initialization disabled
        // Initialize categories if they don't exist
        // if (categoryRepository.count() == 0) {
        //     createSampleCategories();
        // }

        // Initialize units if they don't exist
        // if (unitRepository.count() == 0) {
        //     createSampleUnits();
        // }
    }

    private void createSampleCategories() {
        try {
            ProductCategory beverages = new ProductCategory("Đồ uống", "Các loại đồ uống giải khát");
            ProductCategory snacks = new ProductCategory("Đồ ăn vặt", "Các loại snack, bánh kẹo");
            ProductCategory dairy = new ProductCategory("Sữa và sản phẩm từ sữa", "Sữa tươi, sữa chua, phô mai");
            ProductCategory household = new ProductCategory("Hàng gia dụng", "Các sản phẩm gia dụng");

            categoryRepository.save(beverages);
            categoryRepository.save(snacks);
            categoryRepository.save(dairy);
            categoryRepository.save(household);

            System.out.println("✅ Sample categories created successfully!");
        } catch (Exception e) {
            System.err.println("❌ Lỗi khởi tạo categories: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createSampleUnits() {
        try {
            // Tạo đơn vị mặc định (isDefault = true)
            Unit lon = new Unit("Lon", "Đơn vị lon (330ml)");
            lon.setIsDefault(true);

            Unit chai = new Unit("Chai", "Đơn vị chai (500ml)");
            Unit thung = new Unit("Thùng", "Đơn vị thùng (24 lon)");
            Unit goi = new Unit("Gói", "Đơn vị gói");
            Unit hop = new Unit("Hộp", "Đơn vị hộp");

            unitRepository.save(lon);
            unitRepository.save(chai);
            unitRepository.save(thung);
            unitRepository.save(goi);
            unitRepository.save(hop);

            System.out.println("✅ Sample units created successfully!");
            System.out.println("✅ Đơn vị mặc định: " + lon.getName());
        } catch (Exception e) {
            System.err.println("❌ Lỗi khởi tạo units: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

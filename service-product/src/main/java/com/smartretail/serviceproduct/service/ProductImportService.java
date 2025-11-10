package com.smartretail.serviceproduct.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.smartretail.serviceproduct.dto.ImportResultDto;
import com.smartretail.serviceproduct.dto.ProductDto;
import com.smartretail.serviceproduct.model.ProductCategory;
import com.smartretail.serviceproduct.model.Unit;
import com.smartretail.serviceproduct.repository.ProductCategoryRepository;
import com.smartretail.serviceproduct.repository.UnitRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductImportService {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private ProductUnitService productUnitService;

    /**
     * Import products from Excel file
     */
    public ImportResultDto importFromExcel(MultipartFile file, Long warehouseId, Long stockLocationId) throws IOException {
        List<ProductDto> products = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;
        int totalRows = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            totalRows = sheet.getLastRowNum();

            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String imageUrl = getCellValueAsString(row.getCell(6)); // Lấy imageUrl từ Excel
                    ProductDto product = parseExcelRow(row);
                    if (product != null) {
                        Integer qty = product.getQuantity();
                        ProductDto createdProduct = productService.createOrUpsertAndInbound(product, qty, warehouseId, stockLocationId);

                        // Gán imageUrl cho ProductUnit mặc định nếu có
                        if (imageUrl != null && !imageUrl.trim().isEmpty() &&
                            createdProduct.getProductUnits() != null && !createdProduct.getProductUnits().isEmpty()) {
                            try {
                                com.smartretail.serviceproduct.dto.ProductUnitInfo defaultUnit = createdProduct.getProductUnits().stream()
                                        .filter(unit -> Boolean.TRUE.equals(unit.getIsDefault()))
                                        .findFirst()
                                        .orElse(createdProduct.getProductUnits().get(0));

                                if (defaultUnit != null) {
                                    com.smartretail.serviceproduct.dto.ProductUnitDto updateUnitDto = new com.smartretail.serviceproduct.dto.ProductUnitDto();
                                    updateUnitDto.setImageUrl(imageUrl.trim());
                                    productUnitService.updateProductUnit(defaultUnit.getId(), updateUnitDto);
                                }
                            } catch (Exception e) {
                                // Log nhưng không fail
                                System.err.println("Lỗi khi cập nhật imageUrl cho ProductUnit: " + e.getMessage());
                            }
                        }

                        products.add(createdProduct);
                        successCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        return new ImportResultDto(
            totalRows, // totalRows
            successCount,
            errorCount,
            errors,
            products
        );
    }

    /**
     * Import products from CSV file
     */
    public ImportResultDto importFromCSV(MultipartFile file, Long warehouseId, Long stockLocationId) throws IOException, CsvException {
        List<ProductDto> products = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;
        int totalRows = 0;

        try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> allRows = csvReader.readAll();
            totalRows = allRows.size() - 1; // Excluding header

            // Skip header row
            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);

                try {
                    String imageUrl = row.length > 6 ? row[6] : null; // Lấy imageUrl từ CSV
                    ProductDto product = parseCSVRow(row);
                    if (product != null) {
                        Integer qty = product.getQuantity();
                        ProductDto createdProduct = productService.createOrUpsertAndInbound(product, qty, warehouseId, stockLocationId);

                        // Gán imageUrl cho ProductUnit mặc định nếu có
                        if (imageUrl != null && !imageUrl.trim().isEmpty() &&
                            createdProduct.getProductUnits() != null && !createdProduct.getProductUnits().isEmpty()) {
                            try {
                                com.smartretail.serviceproduct.dto.ProductUnitInfo defaultUnit = createdProduct.getProductUnits().stream()
                                        .filter(unit -> Boolean.TRUE.equals(unit.getIsDefault()))
                                        .findFirst()
                                        .orElse(createdProduct.getProductUnits().get(0));

                                if (defaultUnit != null) {
                                    com.smartretail.serviceproduct.dto.ProductUnitDto updateUnitDto = new com.smartretail.serviceproduct.dto.ProductUnitDto();
                                    updateUnitDto.setImageUrl(imageUrl.trim());
                                    productUnitService.updateProductUnit(defaultUnit.getId(), updateUnitDto);
                                }
                            } catch (Exception e) {
                                // Log nhưng không fail
                                System.err.println("Lỗi khi cập nhật imageUrl cho ProductUnit: " + e.getMessage());
                            }
                        }

                        products.add(createdProduct);
                        successCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        return new ImportResultDto(
            totalRows, // totalRows (excluding header)
            successCount,
            errorCount,
            errors,
            products
        );
    }

    /**
     * Parse Excel row to ProductDto
     */
    private ProductDto parseExcelRow(Row row) {
        ProductDto product = new ProductDto();

        // Column mapping (adjust based on your Excel template)
        // 0: Name, 1: Description, 2: Category Name, 3: Unit Name, 4: Quantity, 5: Expiration Date, 6: Image URL, 7: Barcode (optional)

        String name = getCellValueAsString(row.getCell(0));
        String description = getCellValueAsString(row.getCell(1));
        String categoryName = getCellValueAsString(row.getCell(2));
        String unitName = getCellValueAsString(row.getCell(3));
        String quantityStr = getCellValueAsString(row.getCell(4));
        String expirationDateStr = getCellValueAsString(row.getCell(5));
        // Note: imageUrl will be extracted outside this method
        String barcode = getCellValueAsString(row.getCell(7));

        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Product name is required");
        }

        product.setName(name.trim());
        product.setDescription(description != null ? description.trim() : "");

        // Find category by name
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            ProductCategory category = categoryRepository.findByName(categoryName.trim())
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryName));
            product.setCategoryId(category.getId());
        }

        // Find unit by name
        if (unitName != null && !unitName.trim().isEmpty()) {
            Unit unit = unitRepository.findByName(unitName.trim())
                .orElseThrow(() -> new RuntimeException("Unit not found: " + unitName));
            product.setDefaultUnitId(unit.getId());
        }

        // Parse quantity
        if (quantityStr != null && !quantityStr.trim().isEmpty()) {
            try {
                int qty = Integer.parseInt(quantityStr.trim());
                if (qty < 0) throw new IllegalArgumentException("Quantity must be >= 0");
                product.setQuantity(qty);
            } catch (NumberFormatException ne) {
                throw new RuntimeException("Invalid quantity: " + quantityStr);
            }
        }

        // Parse expiration date
        if (expirationDateStr != null && !expirationDateStr.trim().isEmpty()) {
            try {
                LocalDate expirationDate = LocalDate.parse(expirationDateStr.trim());
                product.setExpirationDate(expirationDate);
            } catch (Exception e) {
                throw new RuntimeException("Invalid expiration date format: " + expirationDateStr);
            }
        }

        // Note: imageUrl is handled after ProductUnit creation (see importFromExcel/importFromCSV methods)

        // Set barcode (will be handled after product creation)
        if (barcode != null && !barcode.trim().isEmpty()) {
            ProductDto.BarcodeInput barcodeInput = new ProductDto.BarcodeInput();
            barcodeInput.code = barcode.trim();
            barcodeInput.type = "EAN13"; // Default type
            product.setBarcodes(java.util.Arrays.asList(barcodeInput));
        }

        return product;
    }

    /**
     * Parse CSV row to ProductDto
     */
    private ProductDto parseCSVRow(String[] row) {
        ProductDto product = new ProductDto();

        // Column mapping (adjust based on your CSV template)
        // 0: Name, 1: Description, 2: Category Name, 3: Unit Name, 4: Quantity, 5: Expiration Date, 6: Image URL, 7: Barcode (optional)

        String name = row.length > 0 ? row[0] : null;
        String description = row.length > 1 ? row[1] : null;
        String categoryName = row.length > 2 ? row[2] : null;
        String unitName = row.length > 3 ? row[3] : null;
        String quantityStr = row.length > 4 ? row[4] : null;
        String expirationDateStr = row.length > 5 ? row[5] : null;
        // Note: imageUrl will be extracted outside this method
        String barcode = row.length > 7 ? row[7] : null;

        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Product name is required");
        }

        product.setName(name.trim());
        product.setDescription(description != null ? description.trim() : "");

        // Find category by name
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            ProductCategory category = categoryRepository.findByName(categoryName.trim())
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryName));
            product.setCategoryId(category.getId());
        }

        // Find unit by name
        if (unitName != null && !unitName.trim().isEmpty()) {
            Unit unit = unitRepository.findByName(unitName.trim())
                .orElseThrow(() -> new RuntimeException("Unit not found: " + unitName));
            product.setDefaultUnitId(unit.getId());
        }

        // Parse quantity
        if (quantityStr != null && !quantityStr.trim().isEmpty()) {
            try {
                int qty = Integer.parseInt(quantityStr.trim());
                if (qty < 0) throw new IllegalArgumentException("Quantity must be >= 0");
                product.setQuantity(qty);
            } catch (NumberFormatException ne) {
                throw new RuntimeException("Invalid quantity: " + quantityStr);
            }
        }

        // Parse expiration date
        if (expirationDateStr != null && !expirationDateStr.trim().isEmpty()) {
            try {
                LocalDate expirationDate = LocalDate.parse(expirationDateStr.trim());
                product.setExpirationDate(expirationDate);
            } catch (Exception e) {
                throw new RuntimeException("Invalid expiration date format: " + expirationDateStr);
            }
        }

        // Note: imageUrl is handled after ProductUnit creation (see importFromExcel/importFromCSV methods)

        // Set barcode (will be handled after product creation)
        if (barcode != null && !barcode.trim().isEmpty()) {
            ProductDto.BarcodeInput barcodeInput = new ProductDto.BarcodeInput();
            barcodeInput.code = barcode.trim();
            barcodeInput.type = "EAN13"; // Default type
            product.setBarcodes(java.util.Arrays.asList(barcodeInput));
        }

        return product;
    }

    /**
     * Get cell value as string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE);
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
}

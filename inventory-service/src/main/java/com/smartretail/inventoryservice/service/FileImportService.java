package com.smartretail.inventoryservice.service;

import com.smartretail.inventoryservice.dto.InventoryImportDto;
import com.smartretail.inventoryservice.dto.InventoryDto;
import com.smartretail.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileImportService {

    private final InventoryService inventoryService;

    public List<InventoryImportDto.InventoryImportDetailDto> importExcelFile(MultipartFile file) throws IOException {
        List<InventoryImportDto.InventoryImportDetailDto> importDetails = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                InventoryImportDto.InventoryImportDetailDto detail = parseExcelRow(row);
                if (detail != null) {
                    importDetails.add(detail);
                }
            }
        }

        return importDetails;
    }

    public List<InventoryImportDto.InventoryImportDetailDto> importCsvFile(MultipartFile file) throws IOException {
        List<InventoryImportDto.InventoryImportDetailDto> importDetails = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {

            for (CSVRecord record : csvParser) {
                InventoryImportDto.InventoryImportDetailDto detail = parseCsvRecord(record);
                if (detail != null) {
                    importDetails.add(detail);
                }
            }
        }

        return importDetails;
    }

    private InventoryImportDto.InventoryImportDetailDto parseExcelRow(Row row) {
        try {
            InventoryImportDto.InventoryImportDetailDto detail = new InventoryImportDto.InventoryImportDetailDto();

            // Assuming columns: ProductUnitId, Quantity, Note
            detail.setProductUnitId(getLongCellValue(row.getCell(0)));
            detail.setQuantity(getIntCellValue(row.getCell(1)));
            detail.setNote(getStringCellValue(row.getCell(2)));

            // Validate required fields
            if (detail.getProductUnitId() == null || detail.getQuantity() == null || detail.getQuantity() <= 0) {
                log.warn("Skipping invalid row: {}", row.getRowNum());
                return null;
            }

            return detail;
        } catch (Exception e) {
            log.error("Error parsing Excel row {}: {}", row.getRowNum(), e.getMessage());
            return null;
        }
    }

    private InventoryImportDto.InventoryImportDetailDto parseCsvRecord(CSVRecord record) {
        try {
            InventoryImportDto.InventoryImportDetailDto detail = new InventoryImportDto.InventoryImportDetailDto();

            // Assuming columns: ProductUnitId, Quantity, Note
            detail.setProductUnitId(Long.parseLong(record.get(0)));
            detail.setQuantity(Integer.parseInt(record.get(1)));
            detail.setNote(record.get(2));

            // Validate required fields
            if (detail.getProductUnitId() == null || detail.getQuantity() == null || detail.getQuantity() <= 0) {
                log.warn("Skipping invalid CSV record: {}", record.getRecordNumber());
                return null;
            }

            return detail;
        } catch (Exception e) {
            log.error("Error parsing CSV record {}: {}", record.getRecordNumber(), e.getMessage());
            return null;
        }
    }

    private Long getLongCellValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return (long) cell.getNumericCellValue();
            case STRING:
                try {
                    return Long.parseLong(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private Integer getIntCellValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            default:
                return null;
        }
    }

    // Process imported data and create inventory transactions
    public List<InventoryDto> processImportedInventory(
            List<InventoryImportDto.InventoryImportDetailDto> importDetails,
            Long warehouseId,
            Long stockLocationId,
            String referenceNumber,
            String note) {

        List<InventoryDto> createdTransactions = new ArrayList<>();

        for (InventoryImportDto.InventoryImportDetailDto detail : importDetails) {
            try {
                // Create inventory DTO for import
                InventoryDto inventoryDto = new InventoryDto();
                inventoryDto.setTransactionType(com.smartretail.inventoryservice.model.Inventory.TransactionType.IMPORT);
                inventoryDto.setQuantity(detail.getQuantity());
                inventoryDto.setTransactionDate(LocalDateTime.now());
                inventoryDto.setNote(detail.getNote() + " - " + note);
                inventoryDto.setReferenceNumber(referenceNumber + "-" + detail.getProductUnitId());
                inventoryDto.setProductUnitId(detail.getProductUnitId());
                inventoryDto.setStockLocationId(stockLocationId);
                inventoryDto.setWarehouseId(warehouseId);

                // Process the inventory transaction
                InventoryDto createdTransaction = inventoryService.processInboundInventory(inventoryDto);
                createdTransactions.add(createdTransaction);

                log.info("Successfully processed import for product unit ID: {}", detail.getProductUnitId());

            } catch (Exception e) {
                log.error("Error processing import for product unit ID {}: {}", detail.getProductUnitId(), e.getMessage());
                // Continue processing other items
            }
        }

        return createdTransactions;
    }
}

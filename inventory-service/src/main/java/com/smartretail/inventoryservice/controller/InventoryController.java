package com.smartretail.inventoryservice.controller;

import com.smartretail.inventoryservice.dto.*;
import com.smartretail.inventoryservice.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class InventoryController {

    private final FileImportService fileImportService;
    private final InventoryService inventoryService;
    private final StockBalanceService stockBalanceService;
    private final StocktakingService stocktakingService;
    private final StockReservationService stockReservationService;
    private final InventoryAnalyticsService inventoryAnalyticsService;
    private final LotManagementService lotManagementService;

    // Import file nhập kho
    @PostMapping("/inbound/import")
    public ResponseEntity<?> importInventoryFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("warehouseId") Long warehouseId,
            @RequestParam("stockLocationId") Long stockLocationId,
            @RequestParam(value = "referenceNumber", required = false) String referenceNumber,
            @RequestParam(value = "note", required = false) String note) {

        try {
            String fileName = file.getOriginalFilename();
            List<InventoryImportDto.InventoryImportDetailDto> importDetails;

            if (fileName != null && fileName.toLowerCase().endsWith(".xlsx")) {
                importDetails = fileImportService.importExcelFile(file);
            } else if (fileName != null && fileName.toLowerCase().endsWith(".csv")) {
                importDetails = fileImportService.importCsvFile(file);
            } else {
                return ResponseEntity.badRequest().body("Unsupported file format. Please use .xlsx or .csv files.");
            }

            if (importDetails.isEmpty()) {
                return ResponseEntity.badRequest().body("No valid data found in the file.");
            }

            // Process the import details and save to database
            List<InventoryDto> createdTransactions = fileImportService.processImportedInventory(
                importDetails, warehouseId, stockLocationId, referenceNumber, note);

            return ResponseEntity.ok(Map.of(
                "message", "File imported successfully",
                "totalRecords", importDetails.size(),
                "processedRecords", createdTransactions.size(),
                "warehouseId", warehouseId,
                "stockLocationId", stockLocationId,
                "referenceNumber", referenceNumber,
                "createdTransactions", createdTransactions
            ));

        } catch (IOException e) {
            log.error("Error processing file import: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during file import: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    // Tải file mẫu Excel cho nhập kho
    @GetMapping("/inbound/sample.xlsx")
    public ResponseEntity<byte[]> downloadInboundSampleExcel() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("InboundImport");

            // Header
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ProductUnitId");
            header.createCell(1).setCellValue("Quantity");
            header.createCell(2).setCellValue("Note");

            // Sample rows
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue(1L);
            r1.createCell(1).setCellValue(50);
            r1.createCell(2).setCellValue("Nhap lo 1");

            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue(2L);
            r2.createCell(1).setCellValue(30);
            r2.createCell(2).setCellValue("Nhap lo 2");

            for (int i = 0; i < 3; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            byte[] bytes = out.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory_inbound_sample.xlsx");
            headers.setContentLength(bytes.length);

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to generate sample Excel: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ==================== INVENTORY TRANSACTION MANAGEMENT ====================

    // Tạo giao dịch kho (nhập/xuất/điều chỉnh)
    @PostMapping("/transactions")
    public ResponseEntity<InventoryDto> createInventoryTransaction(@RequestBody InventoryDto inventoryDto) {
        try {
            InventoryDto createdTransaction = inventoryService.createInventoryTransaction(inventoryDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
        } catch (Exception e) {
            log.error("Error creating inventory transaction: {}", e.getMessage());
            throw e;
        }
    }

    // Lấy danh sách giao dịch kho
    @GetMapping("/transactions")
    public ResponseEntity<List<InventoryDto>> getInventoryTransactions(
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long stockLocationId) {
        try {
            List<InventoryDto> transactions = inventoryService.getInventoryTransactions(
                transactionType, warehouseId, stockLocationId);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Error getting inventory transactions: {}", e.getMessage());
            throw e;
        }
    }

    // Lấy chi tiết giao dịch kho
    @GetMapping("/transactions/{id}")
    public ResponseEntity<InventoryDto> getInventoryTransactionById(@PathVariable Long id) {
        try {
            return inventoryService.getInventoryTransactionById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting inventory transaction by ID: {}", e.getMessage());
            throw e;
        }
    }

    // Cập nhật giao dịch kho
    @PutMapping("/transactions/{id}")
    public ResponseEntity<InventoryDto> updateInventoryTransaction(@PathVariable Long id, @RequestBody InventoryDto inventoryDto) {
        try {
            InventoryDto updatedTransaction = inventoryService.updateInventoryTransaction(id, inventoryDto);
            return ResponseEntity.ok(updatedTransaction);
        } catch (Exception e) {
            log.error("Error updating inventory transaction: {}", e.getMessage());
            throw e;
        }
    }

    // Xóa giao dịch kho
    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteInventoryTransaction(@PathVariable Long id) {
        try {
            inventoryService.deleteInventoryTransaction(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting inventory transaction: {}", e.getMessage());
            throw e;
        }
    }

    // ==================== INBOUND INVENTORY ====================

    // Danh sách phiếu nhập kho
    @GetMapping("/inbound")
    public ResponseEntity<List<InventoryDto>> getInboundInventory() {
        try {
            List<InventoryDto> inboundTransactions = inventoryService.getInventoryTransactions("IMPORT", null, null);
            return ResponseEntity.ok(inboundTransactions);
        } catch (Exception e) {
            log.error("Error getting inbound inventory: {}", e.getMessage());
            throw e;
        }
    }

    // Chi tiết phiếu nhập
    @GetMapping("/inbound/{id}")
    public ResponseEntity<InventoryDto> getInboundInventoryById(@PathVariable Long id) {
        try {
            return inventoryService.getInventoryTransactionById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting inbound inventory by ID: {}", e.getMessage());
            throw e;
        }
    }

    // Xử lý nhập kho
    @PostMapping("/inbound/process")
    public ResponseEntity<InventoryDto> processInboundInventory(@RequestBody InventoryDto inboundDto) {
        try {
            InventoryDto processedInbound = inventoryService.processInboundInventory(inboundDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(processedInbound);
        } catch (Exception e) {
            log.error("Error processing inbound inventory: {}", e.getMessage());
            throw e;
        }
    }

    // Nhập kho nhiều sản phẩm cùng lúc (bulk inbound)
    @PostMapping("/inbound/bulk")
    public ResponseEntity<?> processBulkInboundInventory(@RequestBody List<InventoryDto> inboundDtos) {
        try {
            List<InventoryDto> createdInbounds = inventoryService.processBulkInboundInventory(inboundDtos);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Bulk inbound inventory processed successfully",
                "totalItems", inboundDtos.size(),
                "processedItems", createdInbounds.size(),
                "createdTransactions", createdInbounds
            ));
        } catch (Exception e) {
            log.error("Error processing bulk inbound inventory: {}", e.getMessage());
            throw e;
        }
    }

    // ==================== OUTBOUND INVENTORY ====================

    // Xuất kho
    @PostMapping("/outbound")
    public ResponseEntity<InventoryDto> createOutboundInventory(@RequestBody InventoryDto outboundDto) {
        try {
            InventoryDto createdOutbound = inventoryService.processOutboundInventory(outboundDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOutbound);
        } catch (Exception e) {
            log.error("Error creating outbound inventory: {}", e.getMessage());
            throw e;
        }
    }

    // Danh sách phiếu xuất kho
    @GetMapping("/outbound")
    public ResponseEntity<List<InventoryDto>> getOutboundInventory() {
        try {
            List<InventoryDto> outboundTransactions = inventoryService.getInventoryTransactions("EXPORT", null, null);
            return ResponseEntity.ok(outboundTransactions);
        } catch (Exception e) {
            log.error("Error getting outbound inventory: {}", e.getMessage());
            throw e;
        }
    }

    // Chi tiết phiếu xuất kho
    @GetMapping("/outbound/{id}")
    public ResponseEntity<InventoryDto> getOutboundInventoryById(@PathVariable Long id) {
        try {
            return inventoryService.getInventoryTransactionById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting outbound inventory by ID: {}", e.getMessage());
            throw e;
        }
    }

    // Xuất kho nhiều sản phẩm cùng lúc (bulk outbound)
    @PostMapping("/outbound/bulk")
    public ResponseEntity<?> createBulkOutboundInventory(@RequestBody List<InventoryDto> outboundDtos) {
        try {
            List<InventoryDto> createdOutbounds = inventoryService.processBulkOutboundInventory(outboundDtos);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Bulk outbound inventory processed successfully",
                "totalItems", outboundDtos.size(),
                "processedItems", createdOutbounds.size(),
                "createdTransactions", createdOutbounds
            ));
        } catch (Exception e) {
            log.error("Error creating bulk outbound inventory: {}", e.getMessage());
            throw e;
        }
    }

    // Xuất kho với FEFO logic (First Expire, First Out)
    @PostMapping("/outbound/fefo")
    public ResponseEntity<?> processOutboundInventoryWithFEFO(@RequestBody InventoryDto outboundDto) {
        try {
            List<InventoryDto> createdOutbounds = inventoryService.processOutboundInventoryWithFEFO(outboundDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Outbound inventory processed successfully with FEFO",
                "createdTransactions", createdOutbounds
            ));
        } catch (Exception e) {
            log.error("Error processing outbound inventory with FEFO: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Chấp nhận xuất kho (approve outbound)
    @PostMapping("/outbound/{id}/accept")
    public ResponseEntity<?> acceptOutboundInventory(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> request) {
        try {
            String note = request != null ? (String) request.get("note") : "Approved outbound";
            InventoryDto acceptedOutbound = inventoryService.acceptOutboundInventory(id, note);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Outbound inventory accepted successfully",
                "data", acceptedOutbound
            ));
        } catch (Exception e) {
            log.error("Error accepting outbound inventory: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Chấp nhận xuất kho hàng loạt (bulk accept)
    @PostMapping("/outbound/bulk/accept")
    public ResponseEntity<?> acceptBulkOutboundInventory(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> inventoryIds = (List<Long>) request.get("inventoryIds");
            String note = (String) request.get("note");

            if (inventoryIds == null || inventoryIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "inventoryIds is required"));
            }

            List<InventoryDto> acceptedOutbounds = inventoryService.acceptBulkOutboundInventory(inventoryIds, note);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Bulk outbound inventory accepted successfully",
                "totalItems", inventoryIds.size(),
                "acceptedItems", acceptedOutbounds.size(),
                "data", acceptedOutbounds
            ));
        } catch (Exception e) {
            log.error("Error accepting bulk outbound inventory: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Xuất kho nhiều sản phẩm với FEFO logic
    @PostMapping("/outbound/bulk/fefo")
    public ResponseEntity<?> processBulkOutboundInventoryWithFEFO(@RequestBody List<InventoryDto> outboundDtos) {
        try {
            List<InventoryDto> createdOutbounds = inventoryService.processBulkOutboundInventoryWithFEFO(outboundDtos);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Bulk outbound inventory processed successfully with FEFO",
                "totalItems", outboundDtos.size(),
                "processedItems", createdOutbounds.size(),
                "createdTransactions", createdOutbounds
            ));
        } catch (Exception e) {
            log.error("Error processing bulk outbound inventory with FEFO: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==================== STOCK BALANCE MANAGEMENT ====================

    // Danh sách tồn kho
    @GetMapping("/stock")
    public ResponseEntity<List<StockBalanceDto>> getStockBalance(
            @RequestParam(required = false) Long productUnitId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long stockLocationId) {
        try {
            List<StockBalanceDto> stockBalances = stockBalanceService.getStockBalance(
                productUnitId, warehouseId, stockLocationId);
            return ResponseEntity.ok(stockBalances);
        } catch (Exception e) {
            log.error("Error getting stock balance: {}", e.getMessage());
            throw e;
        }
    }

    // Tồn kho chi tiết của 1 sản phẩm
    @GetMapping("/stock/{productUnitId}")
    public ResponseEntity<List<StockBalanceDto>> getStockBalanceByProduct(@PathVariable Long productUnitId) {
        try {
            List<StockBalanceDto> stockBalances = stockBalanceService.getStockBalanceByProduct(productUnitId);
            return ResponseEntity.ok(stockBalances);
        } catch (Exception e) {
            log.error("Error getting stock balance by product: {}", e.getMessage());
            throw e;
        }
    }

    // Cập nhật tồn kho thủ công
    @PutMapping("/stock/{id}")
    public ResponseEntity<StockBalanceDto> updateStockBalance(@PathVariable Long id, @RequestBody StockBalanceDto stockBalanceDto) {
        try {
            StockBalanceDto updatedStockBalance = stockBalanceService.updateStockBalance(id, stockBalanceDto);
            return ResponseEntity.ok(updatedStockBalance);
        } catch (Exception e) {
            log.error("Error updating stock balance: {}", e.getMessage());
            throw e;
        }
    }

    // Điều chỉnh tồn kho
    @PostMapping("/stock/adjust")
    public ResponseEntity<StockBalanceDto> adjustStockBalance(@RequestBody StockAdjustmentDto adjustmentDto) {
        try {
            StockBalanceDto adjustedStockBalance = stockBalanceService.adjustStockBalance(adjustmentDto);
            return ResponseEntity.ok(adjustedStockBalance);
        } catch (Exception e) {
            log.error("Error adjusting stock balance: {}", e.getMessage());
            throw e;
        }
    }

    // ==================== STOCK TRANSFER ====================

    // Tạo phiếu chuyển kho
    @PostMapping("/transfer")
    public ResponseEntity<InventoryDto> createStockTransfer(@RequestBody InventoryDto transferDto) {
        try {
            transferDto.setTransactionType(com.smartretail.inventoryservice.model.Inventory.TransactionType.TRANSFER);
            InventoryDto createdTransfer = inventoryService.createInventoryTransaction(transferDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTransfer);
        } catch (Exception e) {
            log.error("Error creating stock transfer: {}", e.getMessage());
            throw e;
        }
    }

    // Danh sách phiếu chuyển kho
    @GetMapping("/transfer")
    public ResponseEntity<List<InventoryDto>> getStockTransfers() {
        try {
            List<InventoryDto> transferTransactions = inventoryService.getInventoryTransactions("TRANSFER", null, null);
            return ResponseEntity.ok(transferTransactions);
        } catch (Exception e) {
            log.error("Error getting stock transfers: {}", e.getMessage());
            throw e;
        }
    }

    // Chi tiết phiếu chuyển kho
    @GetMapping("/transfer/{id}")
    public ResponseEntity<InventoryDto> getStockTransferById(@PathVariable Long id) {
        try {
            return inventoryService.getInventoryTransactionById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting stock transfer by ID: {}", e.getMessage());
            throw e;
        }
    }

    // Xử lý chuyển kho từ kho A sang kho B: EXPORT nguồn + IMPORT đích
    @PostMapping("/transfer/process")
    public ResponseEntity<?> processStockTransfer(@RequestBody com.smartretail.inventoryservice.dto.TransferRequestDto request) {
        try {
            java.util.List<InventoryDto> result = inventoryService.processStockTransfer(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Đã tạo giao dịch chuyển kho (EXPORT + IMPORT)",
                    "transactions", result,
                    "count", result.size()
            ));
        } catch (Exception e) {
            log.error("Error processing stock transfer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ==================== STOCKTAKING ====================

    // Tạo phiếu kiểm kê
    @PostMapping("/stocktaking")
    public ResponseEntity<?> createStocktaking(@RequestBody Map<String, Object> payload,
                                               @RequestHeader(value = "X-User-Id", required = false) String userId,
                                               @RequestHeader(value = "X-User-Name", required = false) String username) {
        try {
            java.time.LocalDateTime date = payload.get("stocktakingDate") != null ? java.time.LocalDateTime.parse((String) payload.get("stocktakingDate")) : java.time.LocalDateTime.now();
            Long warehouseId = Long.valueOf(payload.get("warehouseId").toString());
            Long stockLocationId = payload.get("stockLocationId") == null ? null : Long.valueOf(payload.get("stockLocationId").toString());
            String note = payload.get("note") != null ? payload.get("note").toString() : null;
            Long createdBy = null;
            String createdByUsername = null;
            if (userId != null) {
                // Nếu gateway set userId là số, parse; nếu là email/username, gán sang username
                try {
                    createdBy = Long.valueOf(userId);
                } catch (NumberFormatException ex) {
                    createdByUsername = userId;
                }
            }
            if (createdByUsername == null && username != null) {
                createdByUsername = username;
            }
            if (createdBy == null && payload.get("createdBy") != null) {
                try {
                    createdBy = Long.valueOf(payload.get("createdBy").toString());
                } catch (NumberFormatException ignore) { /* bỏ qua */ }
            }
            if (createdByUsername == null && payload.get("createdByUsername") != null) {
                createdByUsername = payload.get("createdByUsername").toString();
            }

            var st = stocktakingService.createStocktaking(date, warehouseId, stockLocationId, note, createdBy, createdByUsername);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "data", st
            ));
        } catch (Exception e) {
            log.error("Error creating stocktaking: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Danh sách phiếu kiểm kê
    @GetMapping("/stocktaking")
    public ResponseEntity<?> listStocktakings(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long stockLocationId,
            @RequestParam(required = false) String status
    ) {
        try {
            var items = stocktakingService.toDtoList(
                    stocktakingService.listStocktakings(warehouseId, stockLocationId, status)
            );
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "total", items.size(),
                    "data", items
            ));
        } catch (Exception e) {
            log.error("Error listing stocktakings: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Nhập số lượng thực tế cho từng sản phẩm
    @PostMapping("/stocktaking/{id}/details")
    public ResponseEntity<?> addStocktakingDetail(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Long productUnitId = Long.valueOf(payload.get("productUnitId").toString());
            Integer systemQty = Integer.valueOf(payload.get("systemQuantity").toString());
            Integer actualQty = Integer.valueOf(payload.get("actualQuantity").toString());
            String note = payload.get("note") != null ? payload.get("note").toString() : null;
            var detail = stocktakingService.upsertDetail(id, productUnitId, systemQty, actualQty, note);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "data", detail));
        } catch (Exception e) {
            log.error("Error adding stocktaking detail: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Lấy 1 phiếu kiểm kê theo ID
    @GetMapping("/stocktaking/{id}")
    public ResponseEntity<?> getStocktaking(@PathVariable Long id) {
        try {
            var st = stocktakingService.toDto(stocktakingService.getStocktakingById(id));
            return ResponseEntity.ok(Map.of("success", true, "data", st));
        } catch (Exception e) {
            log.error("Error getting stocktaking: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Lấy danh sách chi tiết của 1 phiếu
    @GetMapping("/stocktaking/{id}/details")
    public ResponseEntity<?> getStocktakingDetails(@PathVariable Long id) {
        try {
            var details = stocktakingService.toDetailDtoList(stocktakingService.getDetailsByStocktakingId(id));
            return ResponseEntity.ok(Map.of("success", true, "total", details.size(), "data", details));
        } catch (Exception e) {
            log.error("Error getting stocktaking details: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Xóa phiếu kiểm kê
    @DeleteMapping("/stocktaking/{id}")
    public ResponseEntity<?> deleteStocktaking(@PathVariable Long id) {
        try {
            stocktakingService.deleteStocktaking(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Stocktaking deleted"));
        } catch (Exception e) {
            log.error("Error deleting stocktaking: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Xuất Excel 1 phiếu kiểm kê
    @GetMapping("/stocktaking/{id}/export.xlsx")
    public ResponseEntity<byte[]> exportStocktakingExcel(@PathVariable Long id) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var st = stocktakingService.toDto(stocktakingService.getStocktakingById(id));
            var details = stocktakingService.toDetailDtoList(stocktakingService.getDetailsByStocktakingId(id));

            Sheet sheet = workbook.createSheet("Stocktaking");

            int r = 0;
            Row title = sheet.createRow(r++);
            title.createCell(0).setCellValue("Phiếu kiểm kê");

            Row meta1 = sheet.createRow(r++);
            meta1.createCell(0).setCellValue("Số phiếu");
            meta1.createCell(1).setCellValue(st.getStocktakingNumber());
            meta1.createCell(3).setCellValue("Trạng thái");
            meta1.createCell(4).setCellValue(st.getStatus());

            Row meta2 = sheet.createRow(r++);
            meta2.createCell(0).setCellValue("Kho");
            meta2.createCell(1).setCellValue(st.getWarehouseId() != null ? st.getWarehouseId() : 0);
            meta2.createCell(3).setCellValue("Vị trí");
            meta2.createCell(4).setCellValue(st.getStockLocationId() != null ? st.getStockLocationId() : 0);

            Row meta3 = sheet.createRow(r++);
            meta3.createCell(0).setCellValue("Ngày kiểm kê");
            meta3.createCell(1).setCellValue(st.getStocktakingDate() != null ? st.getStocktakingDate().toString() : "");
            meta3.createCell(3).setCellValue("Ngày hoàn tất");
            meta3.createCell(4).setCellValue(st.getCompletedDate() != null ? st.getCompletedDate().toString() : "");

            Row meta4 = sheet.createRow(r++);
            meta4.createCell(0).setCellValue("Người tạo");
            meta4.createCell(1).setCellValue(st.getCreatedBy() != null ? st.getCreatedBy().toString() : (st.getCreatedByUsername() != null ? st.getCreatedByUsername() : ""));

            Row meta5 = sheet.createRow(r++);
            meta5.createCell(0).setCellValue("Ghi chú");
            meta5.createCell(1).setCellValue(st.getNote() != null ? st.getNote() : "");

            r++;
            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("#");
            header.createCell(1).setCellValue("Sản phẩm");
            header.createCell(2).setCellValue("Đơn vị");
            header.createCell(3).setCellValue("Tồn hệ thống");
            header.createCell(4).setCellValue("Thực tế");
            header.createCell(5).setCellValue("Chênh lệch");
            header.createCell(6).setCellValue("Ghi chú");

            int idx = 1;
            for (var d : details) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(idx++);
                row.createCell(1).setCellValue(d.getProductName() != null ? d.getProductName() : "");
                row.createCell(2).setCellValue(d.getUnitName() != null ? d.getUnitName() : "");
                row.createCell(3).setCellValue(d.getSystemQuantity() != null ? d.getSystemQuantity() : 0);
                row.createCell(4).setCellValue(d.getActualQuantity() != null ? d.getActualQuantity() : 0);
                row.createCell(5).setCellValue(d.getDifferenceQuantity() != null ? d.getDifferenceQuantity() : 0);
                row.createCell(6).setCellValue(d.getNote() != null ? d.getNote() : "");
            }

            for (int c = 0; c <= 6; c++) sheet.autoSizeColumn(c);

            workbook.write(out);
            byte[] bytes = out.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stocktaking_" + id + ".xlsx");
            headers.setContentLength(bytes.length);
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error exporting stocktaking excel: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    // Xác nhận kết quả kiểm kê
    // Xác nhận kiểm kê: FE có thể gửi toàn bộ danh sách chi tiết trong 1 request, không cần lưu tạm từng dòng
    @PutMapping("/stocktaking/{id}/confirm")
    public ResponseEntity<?> confirmStocktaking(@PathVariable Long id, @RequestBody(required = false) java.util.List<com.smartretail.inventoryservice.dto.StocktakingDetailDto> details) {
        try {
            if (details != null && !details.isEmpty()) {
                stocktakingService.confirmWithPayload(id, details);
            } else {
                stocktakingService.confirm(id);
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "Stocktaking confirmed and adjustments applied"));
        } catch (Exception e) {
            log.error("Error confirming stocktaking: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Hoàn tất phiếu kiểm kê - chuyển từ CONFIRMED sang COMPLETED
    @PutMapping("/stocktaking/{id}/complete")
    public ResponseEntity<?> completeStocktaking(@PathVariable Long id) {
        try {
            var completedStocktaking = stocktakingService.complete(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Stocktaking completed successfully",
                    "data", completedStocktaking
            ));
        } catch (Exception e) {
            log.error("Error completing stocktaking: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Hủy phiếu kiểm kê - chuyển sang CANCELLED
    @PutMapping("/stocktaking/{id}/cancel")
    public ResponseEntity<?> cancelStocktaking(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : null;
            var cancelledStocktaking = stocktakingService.cancel(id, reason);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Stocktaking cancelled successfully",
                    "data", cancelledStocktaking
            ));
        } catch (Exception e) {
            log.error("Error cancelling stocktaking: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==================== LOT MANAGEMENT ====================

    // Tạo lô mới
    @PostMapping("/lots")
    public ResponseEntity<?> createLot(@RequestBody com.smartretail.inventoryservice.dto.StockLotDto lotDto) {
        try {
            com.smartretail.inventoryservice.dto.StockLotDto createdLot = lotManagementService.createLot(lotDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Lot created successfully",
                    "data", createdLot
            ));
        } catch (Exception e) {
            log.error("Error creating lot: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Lấy lô theo ID
    @GetMapping("/lots/{id}")
    public ResponseEntity<?> getLotById(@PathVariable Long id) {
        try {
            return lotManagementService.getLotById(id)
                    .map(lot -> ResponseEntity.ok(Map.of("success", true, "data", lot)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting lot by ID: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Lấy lô theo số lô
    @GetMapping("/lots/by-number/{lotNumber}")
    public ResponseEntity<?> getLotByNumber(@PathVariable String lotNumber) {
        try {
            return lotManagementService.getLotByNumber(lotNumber)
                    .map(lot -> ResponseEntity.ok(Map.of("success", true, "data", lot)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error getting lot by number: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Lấy danh sách lô theo sản phẩm và kho
    @GetMapping("/lots")
    public ResponseEntity<?> getLots(
            @RequestParam(required = false) Long productUnitId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long stockLocationId) {
        try {
            List<com.smartretail.inventoryservice.dto.StockLotDto> lots;
            if (productUnitId != null && warehouseId != null && stockLocationId != null) {
                lots = lotManagementService.getLotsByProductAndWarehouse(productUnitId, warehouseId, stockLocationId);
            } else {
                lots = new java.util.ArrayList<>();
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "total", lots.size(),
                    "data", lots
            ));
        } catch (Exception e) {
            log.error("Error getting lots: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Cập nhật lô
    @PutMapping("/lots/{id}")
    public ResponseEntity<?> updateLot(@PathVariable Long id, @RequestBody com.smartretail.inventoryservice.dto.StockLotDto lotDto) {
        try {
            com.smartretail.inventoryservice.dto.StockLotDto updatedLot = lotManagementService.updateLot(id, lotDto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Lot updated successfully",
                    "data", updatedLot
            ));
        } catch (Exception e) {
            log.error("Error updating lot: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Xóa lô
    @DeleteMapping("/lots/{id}")
    public ResponseEntity<?> deleteLot(@PathVariable Long id) {
        try {
            lotManagementService.deleteLot(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Lot deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting lot: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==================== EXPIRY DATE MANAGEMENT ====================

    // Lấy lô sắp hết hạn
    @GetMapping("/lots/near-expiry")
    public ResponseEntity<?> getLotsNearExpiry(@RequestParam(defaultValue = "30") int days) {
        try {
            List<com.smartretail.inventoryservice.dto.StockLotDto> lots = lotManagementService.getLotsNearExpiry(days);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "total", lots.size(),
                    "days", days,
                    "data", lots
            ));
        } catch (Exception e) {
            log.error("Error getting lots near expiry: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Lấy lô đã hết hạn
    @GetMapping("/lots/expired")
    public ResponseEntity<?> getExpiredLots() {
        try {
            List<com.smartretail.inventoryservice.dto.StockLotDto> lots = lotManagementService.getExpiredLots();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "total", lots.size(),
                    "data", lots
            ));
        } catch (Exception e) {
            log.error("Error getting expired lots: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==================== FEFO ALLOCATION ====================

    // Phân bổ số lượng theo FEFO
    @PostMapping("/lots/allocate-fefo")
    public ResponseEntity<?> allocateQuantityFEFO(@RequestBody Map<String, Object> request) {
        try {
            Long productUnitId = Long.valueOf(request.get("productUnitId").toString());
            Long warehouseId = Long.valueOf(request.get("warehouseId").toString());
            Long stockLocationId = Long.valueOf(request.get("stockLocationId").toString());
            Integer quantity = Integer.valueOf(request.get("quantity").toString());

            List<com.smartretail.inventoryservice.dto.StockLotDto> allocatedLots =
                    lotManagementService.allocateQuantityFEFO(productUnitId, warehouseId, stockLocationId, quantity);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Quantity allocated successfully using FEFO",
                    "allocatedLots", allocatedLots,
                    "totalAllocated", allocatedLots.stream().mapToInt(com.smartretail.inventoryservice.dto.StockLotDto::getReservedQuantity).sum()
            ));
        } catch (Exception e) {
            log.error("Error allocating quantity with FEFO: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Kiểm tra tồn kho có sẵn (không thực hiện xuất kho)
    @PostMapping("/stock/check-availability")
    public ResponseEntity<?> checkStockAvailability(@RequestBody Map<String, Object> request) {
        try {
            Long productUnitId = Long.valueOf(request.get("productUnitId").toString());
            Long warehouseId = request.get("warehouseId") != null ? Long.valueOf(request.get("warehouseId").toString()) : null;
            Long stockLocationId = request.get("stockLocationId") != null ? Long.valueOf(request.get("stockLocationId").toString()) : null;
            Integer requiredQuantity = Integer.valueOf(request.get("requiredQuantity").toString());

            com.smartretail.inventoryservice.service.LotManagementService.StockAvailabilityResult result =
                    lotManagementService.checkStockAvailability(productUnitId, warehouseId, stockLocationId, requiredQuantity);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Stock availability checked successfully",
                    "data", result
            ));
        } catch (Exception e) {
            log.error("Error checking stock availability: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Kiểm tra available quantity từ cả stock_balance và stock_lots
    @PostMapping("/stock/check-available-quantity")
    public ResponseEntity<?> checkAvailableQuantity(@RequestBody Map<String, Object> request) {
        try {
            Long productUnitId = Long.valueOf(request.get("productUnitId").toString());
            Long warehouseId = Long.valueOf(request.get("warehouseId").toString());
            Long stockLocationId = Long.valueOf(request.get("stockLocationId").toString());

            StockReservationService.AvailableQuantityInfo info = stockReservationService.getAvailableQuantityInfo(
                    productUnitId, warehouseId, stockLocationId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Available quantity checked successfully",
                    "data", info
            ));
        } catch (Exception e) {
            log.error("Error checking available quantity: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==================== LOT STATISTICS ====================

    // Lấy thống kê lô
    @GetMapping("/lots/statistics")
    public ResponseEntity<?> getLotStatistics() {
        try {
            com.smartretail.inventoryservice.service.LotManagementService.LotStatistics stats =
                    lotManagementService.getLotStatistics();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", stats
            ));
        } catch (Exception e) {
            log.error("Error getting lot statistics: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==================== INVENTORY ANALYTICS ====================

    // Thống kê tồn kho theo sản phẩm (tổng, khả dụng, đã đặt trước)
    @GetMapping("/stock/summary")
    public ResponseEntity<?> getStockSummaryByProduct(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long stockLocationId) {
        try {
            List<com.smartretail.inventoryservice.dto.ProductStockSummaryDto> data =
                    inventoryAnalyticsService.getStockSummaryByProduct(warehouseId, stockLocationId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "total", data.size(),
                    "data", data
            ));
        } catch (Exception e) {
            log.error("Error getting stock summary: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Cảnh báo hàng sắp hết theo ngưỡng availableQuantity
    @GetMapping("/stock/alerts/low")
    public ResponseEntity<?> getLowStockAlerts(
            @RequestParam(defaultValue = "10") Integer threshold,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long stockLocationId) {
        try {
            List<com.smartretail.inventoryservice.dto.LowStockAlertDto> data =
                    inventoryAnalyticsService.getLowStockAlerts(threshold, warehouseId, stockLocationId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "threshold", threshold,
                    "total", data.size(),
                    "data", data
            ));
        } catch (Exception e) {
            log.error("Error getting low stock alerts: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==================== RETURN PROCESSING ====================

    // Xử lý trả hàng với lot tracking
    @PostMapping("/return/process")
    public ResponseEntity<?> processReturn(@RequestBody Map<String, Object> request) {
        try {
            Long productUnitId = Long.valueOf(request.get("productUnitId").toString());
            Long warehouseId = Long.valueOf(request.get("warehouseId").toString());
            Long stockLocationId = Long.valueOf(request.get("stockLocationId").toString());
            Integer quantity = Integer.valueOf(request.get("quantity").toString());
            String lotNumber = request.get("lotNumber") != null ? request.get("lotNumber").toString() : null;
            String note = request.get("note") != null ? request.get("note").toString() : "Return processing";
            String referenceNumber = request.get("referenceNumber") != null ? request.get("referenceNumber").toString() : "RET-" + System.currentTimeMillis();

            // Tạo hoặc tìm lô cho sản phẩm trả về
            com.smartretail.inventoryservice.dto.StockLotDto lotDto;
            if (lotNumber != null) {
                // Tìm lô hiện có
                lotDto = lotManagementService.getLotByNumber(lotNumber)
                        .orElseThrow(() -> new RuntimeException("Lot not found: " + lotNumber));
            } else {
                // Tạo lô mới cho sản phẩm trả về
                lotDto = com.smartretail.inventoryservice.dto.StockLotDto.builder()
                        .lotNumber("RET-" + System.currentTimeMillis())
                        .productUnitId(productUnitId)
                        .warehouseId(warehouseId)
                        .stockLocationId(stockLocationId)
                        .initialQuantity(quantity)
                        .note("Return lot: " + note)
                        .build();
                lotDto = lotManagementService.createLot(lotDto);
            }

            // Tạo giao dịch nhập kho cho sản phẩm trả về
            com.smartretail.inventoryservice.dto.InventoryDto inboundDto = new com.smartretail.inventoryservice.dto.InventoryDto();
            inboundDto.setProductUnitId(productUnitId);
            inboundDto.setWarehouseId(warehouseId);
            inboundDto.setStockLocationId(stockLocationId);
            inboundDto.setQuantity(quantity);
            inboundDto.setNote("Return: " + note);
            inboundDto.setReferenceNumber(referenceNumber);
            inboundDto.setTransactionDate(java.time.LocalDateTime.now());

            com.smartretail.inventoryservice.dto.InventoryDto createdTransaction =
                    inventoryService.processInboundInventory(inboundDto);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Return processed successfully",
                    "lot", lotDto,
                    "transaction", createdTransaction
            ));
        } catch (Exception e) {
            log.error("Error processing return: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Xử lý trả hàng nhiều sản phẩm
    @PostMapping("/return/bulk")
    public ResponseEntity<?> processBulkReturn(@RequestBody List<Map<String, Object>> requests) {
        try {
            List<Map<String, Object>> results = new java.util.ArrayList<>();

            for (Map<String, Object> request : requests) {
                ResponseEntity<?> response = processReturn(request);
                results.add(Map.of(
                        "request", request,
                        "response", response.getBody()
                ));
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Bulk return processed successfully",
                    "totalItems", requests.size(),
                    "results", results
            ));
        } catch (Exception e) {
            log.error("Error processing bulk return: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

}

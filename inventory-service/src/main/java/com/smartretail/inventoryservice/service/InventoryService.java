package com.smartretail.inventoryservice.service;

import com.smartretail.inventoryservice.dto.InventoryDto;
import com.smartretail.inventoryservice.client.ProductServiceClient;
import com.smartretail.inventoryservice.dto.StockAdjustmentDto;
import com.smartretail.inventoryservice.dto.TransferRequestDto;
import com.smartretail.inventoryservice.model.Inventory;
import com.smartretail.inventoryservice.model.StockBalance;
import com.smartretail.inventoryservice.model.StockLocation;
import com.smartretail.inventoryservice.model.StockLot;
import com.smartretail.inventoryservice.model.Warehouse;
import com.smartretail.inventoryservice.repository.InventoryRepository;
import com.smartretail.inventoryservice.repository.StockBalanceRepository;
import com.smartretail.inventoryservice.repository.StockLotRepository;
import com.smartretail.inventoryservice.repository.WarehouseRepository;
import com.smartretail.inventoryservice.repository.StockLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockLotRepository stockLotRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockLocationRepository stockLocationRepository;
    private final ProductServiceClient productServiceClient;
    private final LotManagementService lotManagementService;

    // Tạo giao dịch kho
    public InventoryDto createInventoryTransaction(InventoryDto inventoryDto) {
        log.info("Creating inventory transaction: {}", inventoryDto);

                // Get warehouse and stock location entities
        Warehouse warehouse = warehouseRepository.findById(inventoryDto.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + inventoryDto.getWarehouseId()));
        StockLocation stockLocation = stockLocationRepository.findById(inventoryDto.getStockLocationId())
                .orElseThrow(() -> new RuntimeException("Stock location not found with id: " + inventoryDto.getStockLocationId()));

        // Create inventory record
        Inventory inventory = new Inventory();
        inventory.setTransactionType(inventoryDto.getTransactionType());
        // Lưu số lượng theo đơn vị thực tế (không chuyển đổi)
        inventory.setQuantity(inventoryDto.getQuantity());
        inventory.setTransactionDate(inventoryDto.getTransactionDate());
        inventory.setNote(inventoryDto.getNote());
        inventory.setReferenceNumber(inventoryDto.getReferenceNumber());
        inventory.setProductUnitId(inventoryDto.getProductUnitId());
        inventory.setStockLocation(stockLocation);
        inventory.setWarehouse(warehouse);

        Inventory savedInventory = inventoryRepository.save(inventory);

        // Update stock balance
        updateStockBalance(savedInventory);

        return convertToDto(savedInventory);
    }

    // Lấy danh sách giao dịch kho
    public List<InventoryDto> getInventoryTransactions(
            String transactionType,
            Long warehouseId,
            Long stockLocationId) {

        List<Inventory> inventories;

        if (transactionType != null) {
            inventories = inventoryRepository.findByTransactionType(
                Inventory.TransactionType.valueOf(transactionType.toUpperCase()));
        } else if (warehouseId != null) {
            inventories = inventoryRepository.findByWarehouse_Id(warehouseId);
        } else if (stockLocationId != null) {
            inventories = inventoryRepository.findByStockLocation_Id(stockLocationId);
        } else {
            inventories = inventoryRepository.findAll();
        }

        return inventories.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Lấy chi tiết giao dịch kho
    public Optional<InventoryDto> getInventoryTransactionById(Long id) {
        return inventoryRepository.findById(id)
                .map(this::convertToDto);
    }

    // Cập nhật giao dịch kho
    public InventoryDto updateInventoryTransaction(Long id, InventoryDto inventoryDto) {
        Inventory existingInventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory transaction not found with id: " + id));

                // Get warehouse and stock location entities
        Warehouse warehouse = warehouseRepository.findById(inventoryDto.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + inventoryDto.getWarehouseId()));
        StockLocation stockLocation = stockLocationRepository.findById(inventoryDto.getStockLocationId())
                .orElseThrow(() -> new RuntimeException("Stock location not found with id: " + inventoryDto.getStockLocationId()));

        // Update fields
        existingInventory.setTransactionType(inventoryDto.getTransactionType());
        // Lưu số lượng theo đơn vị thực tế (không chuyển đổi)
        existingInventory.setQuantity(inventoryDto.getQuantity());
        existingInventory.setTransactionDate(inventoryDto.getTransactionDate());
        existingInventory.setNote(inventoryDto.getNote());
        existingInventory.setReferenceNumber(inventoryDto.getReferenceNumber());
        existingInventory.setProductUnitId(inventoryDto.getProductUnitId());
        existingInventory.setStockLocation(stockLocation);
        existingInventory.setWarehouse(warehouse);

        Inventory updatedInventory = inventoryRepository.save(existingInventory);

        // Update stock balance
        updateStockBalance(updatedInventory);

        return convertToDto(updatedInventory);
    }

    // Xóa giao dịch kho
    public void deleteInventoryTransaction(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory transaction not found with id: " + id));

        // Reverse stock balance impact
        reverseStockBalanceImpact(inventory);

        inventoryRepository.delete(inventory);
    }

    // Xử lý nhập kho
    public InventoryDto processInboundInventory(InventoryDto inboundDto) {
        inboundDto.setTransactionType(Inventory.TransactionType.IMPORT);

        // Auto-create/merge lot if lot fields provided
        if (inboundDto.getLotNumber() != null || inboundDto.getExpiryDate() != null) {
            lotManagementService.upsertLotOnInbound(
                    inboundDto.getProductUnitId(),
                    inboundDto.getWarehouseId(),
                    inboundDto.getStockLocationId(),
                    inboundDto.getQuantity(),
                    inboundDto.getLotNumber(),
                    inboundDto.getExpiryDate(),
                    inboundDto.getManufacturingDate(),
                    inboundDto.getSupplierName(),
                    inboundDto.getSupplierBatchNumber(),
                    inboundDto.getNote()
            );
        }

        return createInventoryTransaction(inboundDto);
    }

    // Xử lý nhập kho nhiều sản phẩm cùng lúc (bulk inbound)
    @Transactional
    public List<InventoryDto> processBulkInboundInventory(List<InventoryDto> inboundDtos) {
        List<InventoryDto> createdTransactions = new ArrayList<>();

        for (InventoryDto inboundDto : inboundDtos) {
            inboundDto.setTransactionType(Inventory.TransactionType.IMPORT);
            if (inboundDto.getLotNumber() != null || inboundDto.getExpiryDate() != null) {
                lotManagementService.upsertLotOnInbound(
                        inboundDto.getProductUnitId(),
                        inboundDto.getWarehouseId(),
                        inboundDto.getStockLocationId(),
                        inboundDto.getQuantity(),
                        inboundDto.getLotNumber(),
                        inboundDto.getExpiryDate(),
                        inboundDto.getManufacturingDate(),
                        inboundDto.getSupplierName(),
                        inboundDto.getSupplierBatchNumber(),
                        inboundDto.getNote()
                );
            }
            InventoryDto created = createInventoryTransaction(inboundDto);
            createdTransactions.add(created);
        }

        return createdTransactions;
    }

    // Xử lý xuất kho
    public InventoryDto processOutboundInventory(InventoryDto outboundDto) {
        // Check stock availability
        checkStockAvailability(outboundDto.getProductUnitId(),
                             outboundDto.getStockLocationId(),
                             outboundDto.getQuantity());

        outboundDto.setTransactionType(Inventory.TransactionType.EXPORT);
        return createInventoryTransaction(outboundDto);
    }

    // Chấp nhận xuất kho (approve outbound)
    public InventoryDto acceptOutboundInventory(Long inventoryId, String note) {
        log.info("Accepting outbound inventory: {}", inventoryId);

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new RuntimeException("Inventory transaction not found: " + inventoryId));

        if (inventory.getTransactionType() != Inventory.TransactionType.EXPORT) {
            throw new RuntimeException("Only EXPORT transactions can be accepted");
        }

        // Cập nhật note nếu có
        if (note != null && !note.trim().isEmpty()) {
            inventory.setNote(inventory.getNote() + " | Accepted: " + note);
        }

        inventory.setUpdatedAt(LocalDateTime.now());
        Inventory savedInventory = inventoryRepository.save(inventory);

        log.info("Outbound inventory {} accepted successfully", inventoryId);
        return convertToDto(savedInventory);
    }

    // Chấp nhận xuất kho hàng loạt (bulk accept)
    @Transactional
    public List<InventoryDto> acceptBulkOutboundInventory(List<Long> inventoryIds, String note) {
        log.info("Accepting bulk outbound inventory: {}", inventoryIds);

        List<InventoryDto> acceptedOutbounds = new ArrayList<>();

        for (Long inventoryId : inventoryIds) {
            try {
                InventoryDto accepted = acceptOutboundInventory(inventoryId, note);
                acceptedOutbounds.add(accepted);
            } catch (Exception e) {
                log.error("Failed to accept inventory {}: {}", inventoryId, e.getMessage());
                // Continue with other items instead of failing the entire batch
            }
        }

        log.info("Bulk outbound inventory accepted: {}/{} items", acceptedOutbounds.size(), inventoryIds.size());
        return acceptedOutbounds;
    }

    // Xử lý xuất kho nhiều sản phẩm cùng lúc (bulk outbound)
    @Transactional
    public List<InventoryDto> processBulkOutboundInventory(List<InventoryDto> outboundDtos) {
        List<InventoryDto> createdTransactions = new ArrayList<>();

        // Validate all items first
        for (InventoryDto outboundDto : outboundDtos) {
            // Check stock availability for each item
            checkStockAvailability(outboundDto.getProductUnitId(),
                                 outboundDto.getStockLocationId(),
                                 outboundDto.getQuantity());
        }

        // Process all items if validation passes
        for (InventoryDto outboundDto : outboundDtos) {
            outboundDto.setTransactionType(Inventory.TransactionType.EXPORT);
            InventoryDto createdTransaction = createInventoryTransaction(outboundDto);
            createdTransactions.add(createdTransaction);
        }

        return createdTransactions;
    }

    // Xử lý xuất kho với FEFO logic (First Expire, First Out)
    @Transactional
    public List<InventoryDto> processOutboundInventoryWithFEFO(InventoryDto outboundDto) {
        log.info("Processing outbound with FEFO for product {} quantity {}",
                outboundDto.getProductUnitId(), outboundDto.getQuantity());

        // Check stock availability first
        checkStockAvailability(outboundDto.getProductUnitId(),
                             outboundDto.getStockLocationId(),
                             outboundDto.getQuantity());

        // Allocate lots using FEFO
        List<com.smartretail.inventoryservice.dto.StockLotDto> allocatedLots =
                lotManagementService.allocateQuantityFEFO(
                        outboundDto.getProductUnitId(),
                        outboundDto.getWarehouseId(),
                        outboundDto.getStockLocationId(),
                        outboundDto.getQuantity()
                );

        // Create inventory transaction
        outboundDto.setTransactionType(Inventory.TransactionType.EXPORT);
        InventoryDto createdTransaction = createInventoryTransaction(outboundDto);

        // Consume quantity from allocated lots - sử dụng đúng số lượng vừa được allocate
        for (com.smartretail.inventoryservice.dto.StockLotDto lot : allocatedLots) {
            lotManagementService.consumeQuantity(lot.getId(), lot.getAllocatedQuantity());
        }

        // Add stockLotId to transaction for order tracking
        if (!allocatedLots.isEmpty()) {
            createdTransaction.setStockLotId(allocatedLots.get(0).getId()); // Use first lot ID
        }

        return java.util.Arrays.asList(createdTransaction);
    }

    // Xử lý xuất kho nhiều sản phẩm với FEFO logic
    @Transactional
    public List<InventoryDto> processBulkOutboundInventoryWithFEFO(List<InventoryDto> outboundDtos) {
        List<InventoryDto> createdTransactions = new ArrayList<>();

        // Validate all items first
        for (InventoryDto outboundDto : outboundDtos) {
            checkStockAvailability(outboundDto.getProductUnitId(),
                                 outboundDto.getStockLocationId(),
                                 outboundDto.getQuantity());
        }

        // Process each item with FEFO
        for (InventoryDto outboundDto : outboundDtos) {
            List<InventoryDto> transactions = processOutboundInventoryWithFEFO(outboundDto);
            createdTransactions.addAll(transactions);
        }

        return createdTransactions;
    }

    // Xử lý chuyển kho: EXPORT từ kho nguồn, IMPORT vào kho đích
    public List<InventoryDto> processStockTransfer(TransferRequestDto request) {
        // 1) EXPORT từ kho nguồn
        InventoryDto exportTx = new InventoryDto();
        exportTx.setTransactionType(Inventory.TransactionType.EXPORT);
        exportTx.setQuantity(request.getQuantity());
        exportTx.setTransactionDate(request.getTransactionDate());
        exportTx.setNote("Xuất chuyển kho: " + request.getNote());
        exportTx.setReferenceNumber(request.getReferenceNumber());
        exportTx.setProductUnitId(request.getProductUnitId());
        exportTx.setStockLocationId(request.getSourceStockLocationId());
        exportTx.setWarehouseId(request.getSourceWarehouseId());

        InventoryDto createdExport = processOutboundInventory(exportTx);

        // 2) IMPORT vào kho đích
        InventoryDto importTx = new InventoryDto();
        importTx.setTransactionType(Inventory.TransactionType.IMPORT);
        importTx.setQuantity(request.getQuantity());
        importTx.setTransactionDate(request.getTransactionDate());
        importTx.setNote("Nhập chuyển kho: " + request.getNote());
        importTx.setReferenceNumber(request.getReferenceNumber());
        importTx.setProductUnitId(request.getProductUnitId());
        importTx.setStockLocationId(request.getDestinationStockLocationId());
        importTx.setWarehouseId(request.getDestinationWarehouseId());

        InventoryDto createdImport = processInboundInventory(importTx);

        return java.util.Arrays.asList(createdExport, createdImport);
    }

    // Xử lý điều chỉnh kho
    public InventoryDto processStockAdjustment(StockAdjustmentDto adjustmentDto) {
        InventoryDto inventoryDto = new InventoryDto();
        inventoryDto.setTransactionType(Inventory.TransactionType.ADJUST);
        // For ADJUST, quantity must be the new counted quantity
        inventoryDto.setQuantity(adjustmentDto.getNewQuantity());
        inventoryDto.setTransactionDate(adjustmentDto.getAdjustmentDate() != null
                ? adjustmentDto.getAdjustmentDate() : java.time.LocalDateTime.now());
        inventoryDto.setNote(adjustmentDto.getReason() + ": " + adjustmentDto.getNote());
        inventoryDto.setReferenceNumber(adjustmentDto.getReferenceNumber());
        inventoryDto.setProductUnitId(adjustmentDto.getProductUnitId());
        inventoryDto.setStockLocationId(adjustmentDto.getStockLocationId());
        inventoryDto.setWarehouseId(adjustmentDto.getWarehouseId());

        return createInventoryTransaction(inventoryDto);
    }

    // Cập nhật tồn kho
    private void updateStockBalance(Inventory inventory) {
        // Sử dụng trực tiếp productUnitId thay vì chuyển về đơn vị cơ bản
        Optional<StockBalance> stockBalanceOpt = stockBalanceRepository
                .findByProductUnitIdAndStockLocationAndWarehouse(
                        inventory.getProductUnitId(),
                        inventory.getStockLocation(),
                        inventory.getWarehouse());

        StockBalance stockBalance;

        switch (inventory.getTransactionType()) {
            case IMPORT:
                stockBalance = stockBalanceOpt.orElseGet(() -> createNewStockBalance(inventory, inventory.getProductUnitId()));
                stockBalance.setQuantity(stockBalance.getQuantity() + inventory.getQuantity());
                break;
            case EXPORT:
                stockBalance = stockBalanceOpt.orElseThrow(() ->
                        new RuntimeException("No stock balance found for export at this warehouse/location"));
                stockBalance.setQuantity(stockBalance.getQuantity() - inventory.getQuantity());
                break;
            case ADJUST:
                stockBalance = stockBalanceOpt.orElseGet(() -> createNewStockBalance(inventory, inventory.getProductUnitId()));
                stockBalance.setQuantity(inventory.getQuantity());
                break;
            case TRANSFER:
                stockBalance = stockBalanceOpt.orElseGet(() -> createNewStockBalance(inventory, inventory.getProductUnitId()));
                // Implement real transfer logic if you have source/destination
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + inventory.getTransactionType());
        }

        // Recalculate available quantity after any change
        stockBalance.setAvailableQuantity(stockBalance.getQuantity() - stockBalance.getReservedQuantity());
        stockBalance.setLastUpdatedAt(LocalDateTime.now());
        stockBalanceRepository.save(stockBalance);
    }

    // Tạo tồn kho mới
    private StockBalance createNewStockBalance(Inventory inventory, Long baseProductUnitId) {
        StockBalance stockBalance = new StockBalance();
        stockBalance.setProductUnitId(baseProductUnitId);
        stockBalance.setStockLocation(inventory.getStockLocation());
        stockBalance.setWarehouse(inventory.getWarehouse());
        stockBalance.setQuantity(0);
        stockBalance.setReservedQuantity(0);
        stockBalance.setAvailableQuantity(0);
        return stockBalance;
    }

    // Kiểm tra tồn kho - Sử dụng StockLot thay vì StockBalance để đồng bộ
    private void checkStockAvailability(Long productUnitId, Long stockLocationId, Integer quantity) {
        StockLocation stockLocation = stockLocationRepository.findById(stockLocationId)
                .orElseThrow(() -> new RuntimeException("Stock location not found with id: " + stockLocationId));
        Warehouse warehouse = stockLocation.getWarehouse();

        // Kiểm tra tồn kho thực tế từ StockLot
        List<StockLot> availableLots = stockLotRepository.findAvailableLotsForFEFO(
                productUnitId, warehouse.getId(), stockLocationId);

        int totalAvailable = availableLots.stream()
                .mapToInt(StockLot::getAvailableQuantity)
                .sum();

        if (totalAvailable < quantity) {
            throw new RuntimeException("Số sản phẩm yêu cầu vượt quá số lượng trong kho. " +
                "Số lượng yêu cầu: " + quantity +
                ", Số lượng trong kho còn: " + totalAvailable +
                " (ProductUnitId: " + productUnitId + ")");
        }
    }

    // Đảo ngược tác động tồn kho
    private void reverseStockBalanceImpact(Inventory inventory) {
        // Sử dụng trực tiếp productUnitId thay vì chuyển về đơn vị cơ bản
        StockBalance stockBalance = stockBalanceRepository
                .findByProductUnitIdAndStockLocationAndWarehouse(
                    inventory.getProductUnitId(),
                    inventory.getStockLocation(),
                    inventory.getWarehouse())
                .orElse(null);

        if (stockBalance != null) {
            switch (inventory.getTransactionType()) {
                case IMPORT:
                    stockBalance.setQuantity(stockBalance.getQuantity() - inventory.getQuantity());
                    break;
                case EXPORT:
                    stockBalance.setQuantity(stockBalance.getQuantity() + inventory.getQuantity());
                    break;
                case ADJUST:
                    // Reset to previous value or handle as needed
                    break;
                case TRANSFER:
                    // No-op for reverse as real transfer should have two transactions
                    break;
            }
            stockBalanceRepository.save(stockBalance);
        }
    }



    // Convert entity to DTO
    private InventoryDto convertToDto(Inventory inventory) {
        InventoryDto dto = new InventoryDto();
        dto.setId(inventory.getId());
        dto.setTransactionType(inventory.getTransactionType());
        dto.setQuantity(inventory.getQuantity());
        dto.setTransactionDate(inventory.getTransactionDate());
        dto.setNote(inventory.getNote());
        dto.setReferenceNumber(inventory.getReferenceNumber());
        dto.setProductUnitId(inventory.getProductUnitId());
        dto.setStockLocationId(inventory.getStockLocation().getId());
        dto.setWarehouseId(inventory.getWarehouse().getId());
        dto.setCreatedAt(inventory.getCreatedAt());
        dto.setUpdatedAt(inventory.getUpdatedAt());
        return dto;
    }

    // ===== Helper: Quy đổi về đơn vị cơ bản của sản phẩm =====
    private int normalizeToBaseQuantity(Long productUnitId, Integer quantity) {
        if (quantity == null) return 0;
        try {
            ProductServiceClient.ProductUnitResponse unit = productServiceClient.getProductUnitById(productUnitId);
            if (unit == null) return quantity;

            double conv = unit.getConversionRate() != null ? unit.getConversionRate() : 1.0d;
            // Nếu đơn vị hiện tại là default (conv=1) thì giữ nguyên; nếu là lốc/thùng thì nhân lên
            double result = quantity * conv;
            return (int)Math.round(result);
        } catch (Exception ex) {
            // Fallback: nếu không gọi được product-service thì không quy đổi
            return quantity;
        }
    }

    private Long resolveBaseProductUnitId(Long productUnitId) {
        try {
            ProductServiceClient.ProductUnitResponse unit = productServiceClient.getProductUnitById(productUnitId);
            if (unit == null) return productUnitId;
            if (Boolean.TRUE.equals(unit.getIsDefault())) return productUnitId;

            // Lấy đơn vị default của cùng product
            java.util.List<ProductServiceClient.ProductUnitResponse> list =
                    productServiceClient.getProductUnitsByProductId(unit.getProductId());
            if (list != null) {
                for (ProductServiceClient.ProductUnitResponse u : list) {
                    if (Boolean.TRUE.equals(u.getIsDefault())) {
                        return u.getId();
                    }
                }
            }
            return productUnitId;
        } catch (Exception ex) {
            return productUnitId;
        }
    }
}

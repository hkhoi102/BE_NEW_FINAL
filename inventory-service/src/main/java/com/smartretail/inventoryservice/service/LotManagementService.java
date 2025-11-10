package com.smartretail.inventoryservice.service;

import com.smartretail.inventoryservice.dto.StockLotDto;
import com.smartretail.inventoryservice.model.StockLot;
import com.smartretail.inventoryservice.repository.StockLotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LotManagementService {

    private final StockLotRepository stockLotRepository;

    // Validate lot number before adding to a draft document line
    public void validateLotNumberForInboundDraft(Long productUnitId,
                                                 Long warehouseId,
                                                 Long stockLocationId,
                                                 String lotNumber) {
        if (lotNumber == null || lotNumber.isBlank()) return; // allow empty, will be auto-generated later

        // If the lot number already exists for another product/location → reject
        java.util.Optional<StockLot> conflictOpt = stockLotRepository.findByLotNumber(lotNumber);
        if (conflictOpt.isPresent()) {
            StockLot conflict = conflictOpt.get();
            boolean sameContext = java.util.Objects.equals(conflict.getProductUnitId(), productUnitId)
                    && java.util.Objects.equals(conflict.getWarehouseId(), warehouseId)
                    && java.util.Objects.equals(conflict.getStockLocationId(), stockLocationId);
            if (!sameContext) {
                throw new RuntimeException(String.format(
                        "Số lô '%s' đã được sử dụng cho sản phẩm khác (ProductUnitId: %d, WarehouseId: %d, StockLocationId: %d).",
                        lotNumber, conflict.getProductUnitId(), conflict.getWarehouseId(), conflict.getStockLocationId()));
            }
        }
    }

    // Validate outbound availability at draft creation time
    public void validateOutboundAvailability(Long productUnitId,
                                             Long warehouseId,
                                             Long stockLocationId,
                                             Integer requiredQuantity) {
        if (requiredQuantity == null || requiredQuantity <= 0) return;
        List<StockLot> availableLots = stockLotRepository.findAvailableLotsForFEFO(productUnitId, warehouseId, stockLocationId);
        int totalAvailable = availableLots.stream().mapToInt(StockLot::getAvailableQuantity).sum();
        if (totalAvailable < requiredQuantity) {
            throw new RuntimeException("Số sản phẩm yêu cầu vượt quá số lượng trong kho. " +
                "Số lượng yêu cầu: " + requiredQuantity +
                ", Số lượng trong kho còn: " + totalAvailable +
                " (ProductUnitId: " + productUnitId + ")");
        }
    }

    // Check stock availability without performing outbound operation
    public StockAvailabilityResult checkStockAvailability(Long productUnitId,
                                                         Long warehouseId,
                                                         Long stockLocationId,
                                                         Integer requiredQuantity) {
        if (requiredQuantity == null || requiredQuantity <= 0) {
            return StockAvailabilityResult.builder()
                    .isAvailable(true)
                    .requiredQuantity(0)
                    .availableQuantity(0)
                    .shortageQuantity(0)
                    .availableLots(new ArrayList<>())
                    .build();
        }

        List<StockLot> availableLots;
        if (warehouseId == null && stockLocationId == null) {
            availableLots = stockLotRepository.findAvailableLotsForFEFOAll(productUnitId);
        } else if (warehouseId != null && stockLocationId == null) {
            availableLots = stockLotRepository.findAvailableLotsForFEFOByWarehouse(productUnitId, warehouseId);
        } else if (warehouseId == null && stockLocationId != null) {
            availableLots = stockLotRepository.findAvailableLotsForFEFOByLocation(productUnitId, stockLocationId);
        } else {
            availableLots = stockLotRepository.findAvailableLotsForFEFO(productUnitId, warehouseId, stockLocationId);
        }
        int totalAvailable = availableLots.stream().mapToInt(StockLot::getAvailableQuantity).sum();

        List<StockLotDto> availableLotDtos = availableLots.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        boolean isAvailable = totalAvailable >= requiredQuantity;
        int shortageQuantity = Math.max(0, requiredQuantity - totalAvailable);

        return StockAvailabilityResult.builder()
                .isAvailable(isAvailable)
                .requiredQuantity(requiredQuantity)
                .availableQuantity(totalAvailable)
                .shortageQuantity(shortageQuantity)
                .availableLots(availableLotDtos)
                .build();
    }
    // Upsert lot when inbound: if lotNumber exists, increase quantities; else create new lot
    public StockLotDto upsertLotOnInbound(Long productUnitId,
                                          Long warehouseId,
                                          Long stockLocationId,
                                          Integer inboundQuantity,
                                          String lotNumber,
                                          java.time.LocalDate expiryDate,
                                          java.time.LocalDate manufacturingDate,
                                          String supplierName,
                                          String supplierBatchNumber,
                                          String note) {
        if (lotNumber == null || lotNumber.isBlank()) {
            // If no lot number specified, generate one based on time
            lotNumber = "LOT-" + System.currentTimeMillis();
        }

        // Tìm lô hiện có với cùng productUnitId, warehouseId, stockLocationId
        var existingOpt = stockLotRepository.findByLotNumberAndProductUnitIdAndWarehouseIdAndStockLocationId(
                lotNumber, productUnitId, warehouseId, stockLocationId);

        if (existingOpt.isPresent()) {
            // Cùng sản phẩm/kho/vị trí → Cộng dồn số lượng
            StockLot existing = existingOpt.get();
            existing.setCurrentQuantity(existing.getCurrentQuantity() + inboundQuantity);
            existing.setInitialQuantity(existing.getInitialQuantity() + inboundQuantity);
            if (note != null) existing.setNote(note);
            if (expiryDate != null) existing.setExpiryDate(expiryDate);
            if (manufacturingDate != null) existing.setManufacturingDate(manufacturingDate);
            if (supplierName != null) existing.setSupplierName(supplierName);
            if (supplierBatchNumber != null) existing.setSupplierBatchNumber(supplierBatchNumber);
            existing.setUpdatedAt(LocalDateTime.now());
            stockLotRepository.save(existing);
            return convertToDto(existing);
        } else {
            // Kiểm tra xem số lô có được sử dụng cho sản phẩm/kho/vị trí khác không
            var conflictOpt = stockLotRepository.findByLotNumber(lotNumber);
            if (conflictOpt.isPresent()) {
                // Số lô đã được sử dụng cho sản phẩm/kho/vị trí khác
                StockLot conflictLot = conflictOpt.get();
                throw new RuntimeException(String.format(
                    "Số lô '%s' đã được sử dụng cho sản phẩm khác (ProductUnitId: %d, WarehouseId: %d, StockLocationId: %d). " +
                    "Không thể sử dụng cho sản phẩm hiện tại (ProductUnitId: %d, WarehouseId: %d, StockLocationId: %d). " +
                    "Vui lòng sử dụng số lô khác hoặc kiểm tra lại thông tin.",
                    lotNumber,
                    conflictLot.getProductUnitId(), conflictLot.getWarehouseId(), conflictLot.getStockLocationId(),
                    productUnitId, warehouseId, stockLocationId
                ));
            }
        }

        StockLot newLot = StockLot.builder()
                .lotNumber(lotNumber)
                .productUnitId(productUnitId)
                .warehouseId(warehouseId)
                .stockLocationId(stockLocationId)
                .expiryDate(expiryDate)
                .manufacturingDate(manufacturingDate)
                .supplierName(supplierName)
                .supplierBatchNumber(supplierBatchNumber)
                .initialQuantity(inboundQuantity)
                .currentQuantity(inboundQuantity)
                .reservedQuantity(0)
                .status(StockLot.LotStatus.ACTIVE)
                .note(note)
                .build();
        stockLotRepository.save(newLot);
        return convertToDto(newLot);
    }

    // Tạo lô mới
    public StockLotDto createLot(StockLotDto lotDto) {
        log.info("Creating new lot: {}", lotDto.getLotNumber());

        // Kiểm tra số lô đã tồn tại
        if (stockLotRepository.findByLotNumber(lotDto.getLotNumber()).isPresent()) {
            throw new RuntimeException("Lot number already exists: " + lotDto.getLotNumber());
        }

        StockLot lot = StockLot.builder()
                .lotNumber(lotDto.getLotNumber())
                .productUnitId(lotDto.getProductUnitId())
                .warehouseId(lotDto.getWarehouseId())
                .stockLocationId(lotDto.getStockLocationId())
                .expiryDate(lotDto.getExpiryDate())
                .manufacturingDate(lotDto.getManufacturingDate())
                .supplierName(lotDto.getSupplierName())
                .supplierBatchNumber(lotDto.getSupplierBatchNumber())
                .initialQuantity(lotDto.getInitialQuantity())
                .currentQuantity(lotDto.getInitialQuantity())
                .reservedQuantity(0)
                .status(StockLot.LotStatus.ACTIVE)
                .createdBy(lotDto.getCreatedBy())
                .createdByUsername(lotDto.getCreatedByUsername())
                .note(lotDto.getNote())
                .build();

        StockLot savedLot = stockLotRepository.save(lot);
        return convertToDto(savedLot);
    }

    // Cập nhật lô
    public StockLotDto updateLot(Long lotId, StockLotDto lotDto) {
        log.info("Updating lot: {}", lotId);

        StockLot lot = stockLotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Lot not found with id: " + lotId));

        // Cập nhật các trường có thể sửa
        lot.setExpiryDate(lotDto.getExpiryDate());
        lot.setManufacturingDate(lotDto.getManufacturingDate());
        lot.setSupplierName(lotDto.getSupplierName());
        lot.setSupplierBatchNumber(lotDto.getSupplierBatchNumber());
        lot.setNote(lotDto.getNote());
        lot.setUpdatedAt(LocalDateTime.now());

        StockLot savedLot = stockLotRepository.save(lot);
        return convertToDto(savedLot);
    }

    // Lấy lô theo ID
    public Optional<StockLotDto> getLotById(Long lotId) {
        return stockLotRepository.findById(lotId)
                .map(this::convertToDto);
    }

    // Lấy lô theo số lô
    public Optional<StockLotDto> getLotByNumber(String lotNumber) {
        return stockLotRepository.findByLotNumber(lotNumber)
                .map(this::convertToDto);
    }

    // Lấy danh sách lô theo sản phẩm và kho
    public List<StockLotDto> getLotsByProductAndWarehouse(Long productUnitId, Long warehouseId, Long stockLocationId) {
        List<StockLot> lots = stockLotRepository.findByProductUnitIdAndWarehouseIdAndStockLocationIdAndStatus(
                productUnitId, warehouseId, stockLocationId, StockLot.LotStatus.ACTIVE);
        return lots.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // FEFO: Lấy lô có sẵn theo thứ tự hết hạn sớm nhất
    public List<StockLotDto> getAvailableLotsForFEFO(Long productUnitId, Long warehouseId, Long stockLocationId) {
        List<StockLot> lots = stockLotRepository.findAvailableLotsForFEFO(productUnitId, warehouseId, stockLocationId);
        return lots.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // Lấy lô sắp hết hạn
    public List<StockLotDto> getLotsNearExpiry(int days) {
        LocalDate expiryThreshold = LocalDate.now().plusDays(days);
        List<StockLot> lots = stockLotRepository.findLotsNearExpiry(expiryThreshold);
        return lots.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // Lấy lô đã hết hạn
    public List<StockLotDto> getExpiredLots() {
        List<StockLot> lots = stockLotRepository.findExpiredLots(LocalDate.now());
        return lots.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // Dự trữ số lượng từ lô (reserve)
    public void reserveQuantity(Long lotId, Integer quantity) {
        log.info("Reserving {} units from lot {}", quantity, lotId);

        StockLot lot = stockLotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Lot not found with id: " + lotId));

        if (lot.getStatus() != StockLot.LotStatus.ACTIVE) {
            throw new RuntimeException("Cannot reserve from inactive lot: " + lot.getLotNumber());
        }

        lot.reserveQuantity(quantity);
        stockLotRepository.save(lot);
    }

    // Giải phóng dự trữ
    public void releaseReservation(Long lotId, Integer quantity) {
        log.info("Releasing {} units from lot {}", quantity, lotId);

        StockLot lot = stockLotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Lot not found with id: " + lotId));

        lot.releaseReservation(quantity);
        stockLotRepository.save(lot);
    }

    // Tiêu thụ số lượng từ lô (consume)
    public void consumeQuantity(Long lotId, Integer quantity) {
        log.info("Consuming {} units from lot {}", quantity, lotId);

        StockLot lot = stockLotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Lot not found with id: " + lotId));

        if (lot.getStatus() != StockLot.LotStatus.ACTIVE) {
            throw new RuntimeException("Cannot consume from inactive lot: " + lot.getLotNumber());
        }

        lot.consumeQuantity(quantity);
        stockLotRepository.save(lot);
    }

    // FEFO: Phân bổ số lượng theo thứ tự hết hạn sớm nhất
    public List<StockLotDto> allocateQuantityFEFO(Long productUnitId, Long warehouseId, Long stockLocationId, Integer requiredQuantity) {
        log.info("Allocating {} units using FEFO for product {} at warehouse {} location {}",
                requiredQuantity, productUnitId, warehouseId, stockLocationId);

        List<StockLot> availableLots = stockLotRepository.findAvailableLotsForFEFO(productUnitId, warehouseId, stockLocationId);
        List<StockLotDto> allocatedLots = new ArrayList<>();
        int remainingQuantity = requiredQuantity;

        for (StockLot lot : availableLots) {
            if (remainingQuantity <= 0) break;

            int availableInLot = lot.getAvailableQuantity();
            int toAllocate = Math.min(remainingQuantity, availableInLot);

            if (toAllocate > 0) {
                lot.reserveQuantity(toAllocate);
                stockLotRepository.save(lot);

                // Tạo DTO và set allocatedQuantity
                StockLotDto lotDto = convertToDto(lot);
                lotDto.setAllocatedQuantity(toAllocate);
                allocatedLots.add(lotDto);

                remainingQuantity -= toAllocate;
            }
        }

        if (remainingQuantity > 0) {
            throw new RuntimeException("Insufficient stock. Required: " + requiredQuantity +
                    ", Available: " + (requiredQuantity - remainingQuantity));
        }

        return allocatedLots;
    }

    // Cập nhật trạng thái lô
    public StockLotDto updateLotStatus(Long lotId, StockLot.LotStatus newStatus) {
        log.info("Updating lot {} status to {}", lotId, newStatus);

        StockLot lot = stockLotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Lot not found with id: " + lotId));

        lot.setStatus(newStatus);
        lot.setUpdatedAt(LocalDateTime.now());

        StockLot savedLot = stockLotRepository.save(lot);
        return convertToDto(savedLot);
    }

    // Xóa lô (soft delete)
    public void deleteLot(Long lotId) {
        log.info("Deleting lot: {}", lotId);

        StockLot lot = stockLotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Lot not found with id: " + lotId));

        if (lot.getCurrentQuantity() > 0) {
            throw new RuntimeException("Cannot delete lot with remaining quantity: " + lot.getCurrentQuantity());
        }

        lot.setStatus(StockLot.LotStatus.CANCELLED);
        lot.setUpdatedAt(LocalDateTime.now());
        stockLotRepository.save(lot);
    }

    // Lấy thống kê lô
    public LotStatistics getLotStatistics() {
        long totalLots = stockLotRepository.count();
        long activeLots = stockLotRepository.countByStatus(StockLot.LotStatus.ACTIVE);
        long expiredLots = stockLotRepository.countByStatus(StockLot.LotStatus.EXPIRED);
        long depletedLots = stockLotRepository.countByStatus(StockLot.LotStatus.DEPLETED);

        return LotStatistics.builder()
                .totalLots(totalLots)
                .activeLots(activeLots)
                .expiredLots(expiredLots)
                .depletedLots(depletedLots)
                .build();
    }

    // Convert entity to DTO
    private StockLotDto convertToDto(StockLot lot) {
        return StockLotDto.builder()
                .id(lot.getId())
                .lotNumber(lot.getLotNumber())
                .productUnitId(lot.getProductUnitId())
                .warehouseId(lot.getWarehouseId())
                .stockLocationId(lot.getStockLocationId())
                .expiryDate(lot.getExpiryDate())
                .manufacturingDate(lot.getManufacturingDate())
                .supplierName(lot.getSupplierName())
                .supplierBatchNumber(lot.getSupplierBatchNumber())
                .initialQuantity(lot.getInitialQuantity())
                .currentQuantity(lot.getCurrentQuantity())
                .reservedQuantity(lot.getReservedQuantity())
                .availableQuantity(lot.getAvailableQuantity())
                .status(lot.getStatus())
                .createdAt(lot.getCreatedAt())
                .updatedAt(lot.getUpdatedAt())
                .createdBy(lot.getCreatedBy())
                .createdByUsername(lot.getCreatedByUsername())
                .note(lot.getNote())
                .build();
    }

    // Inner class for statistics
    @lombok.Data
    @lombok.Builder
    public static class LotStatistics {
        private long totalLots;
        private long activeLots;
        private long expiredLots;
        private long depletedLots;
    }

    // Inner class for stock availability result
    @lombok.Data
    @lombok.Builder
    public static class StockAvailabilityResult {
        private boolean isAvailable;
        private Integer requiredQuantity;
        private Integer availableQuantity;
        private Integer shortageQuantity;
        private List<StockLotDto> availableLots;
    }
}

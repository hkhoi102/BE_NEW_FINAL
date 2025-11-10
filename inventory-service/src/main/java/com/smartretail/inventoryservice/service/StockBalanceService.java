package com.smartretail.inventoryservice.service;

import com.smartretail.inventoryservice.dto.StockBalanceDto;
import com.smartretail.inventoryservice.dto.StockAdjustmentDto;
import com.smartretail.inventoryservice.model.StockBalance;
import com.smartretail.inventoryservice.model.Warehouse;
import com.smartretail.inventoryservice.model.StockLocation;
import com.smartretail.inventoryservice.repository.StockBalanceRepository;
import com.smartretail.inventoryservice.repository.WarehouseRepository;
import com.smartretail.inventoryservice.repository.StockLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StockBalanceService {

    private final StockBalanceRepository stockBalanceRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockLocationRepository stockLocationRepository;

    // Lấy tồn kho theo nhiều tiêu chí
    public List<StockBalanceDto> getStockBalance(Long productUnitId, Long warehouseId, Long stockLocationId) {
        List<StockBalance> stockBalances;

        if (productUnitId != null) {
            stockBalances = stockBalanceRepository.findByProductUnitId(productUnitId);
        } else if (warehouseId != null) {
            stockBalances = stockBalanceRepository.findByWarehouse_Id(warehouseId);
        } else if (stockLocationId != null) {
            stockBalances = stockBalanceRepository.findByStockLocation_Id(stockLocationId);
        } else {
            stockBalances = stockBalanceRepository.findAll();
        }

        return stockBalances.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Lấy tồn kho theo sản phẩm
    public List<StockBalanceDto> getStockBalanceByProduct(Long productUnitId) {
        return stockBalanceRepository.findByProductUnitId(productUnitId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Lấy tồn kho theo kho
    public List<StockBalanceDto> getStockBalanceByWarehouse(Long warehouseId) {
        return stockBalanceRepository.findByWarehouse_Id(warehouseId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Lấy tồn kho theo vị trí
    public List<StockBalanceDto> getStockBalanceByLocation(Long stockLocationId) {
        return stockBalanceRepository.findByStockLocation_Id(stockLocationId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Lấy tồn kho theo sản phẩm và kho
    public List<StockBalanceDto> getStockBalanceByProductAndWarehouse(Long productUnitId, Long warehouseId) {
        return stockBalanceRepository.findByProductUnitIdAndWarehouse_Id(productUnitId, warehouseId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Lấy tồn kho theo sản phẩm và vị trí (có thể trả về multiple kết quả nếu có nhiều warehouse)
    public List<StockBalanceDto> getStockBalanceByProductAndLocation(Long productUnitId, Long stockLocationId) {
        return stockBalanceRepository.findByProductUnitIdAndStockLocation_Id(productUnitId, stockLocationId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Lấy tồn kho theo sản phẩm, vị trí và kho (unique result)
    public Optional<StockBalanceDto> getStockBalanceByProductLocationAndWarehouse(Long productUnitId, Long stockLocationId, Long warehouseId) {
        return stockBalanceRepository.findByProductUnitIdAndStockLocation_IdAndWarehouse_Id(productUnitId, stockLocationId, warehouseId)
                .map(this::convertToDto);
    }

    // Cập nhật tồn kho thủ công
    public StockBalanceDto updateStockBalance(Long id, StockBalanceDto stockBalanceDto) {
        StockBalance existingStockBalance = stockBalanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock balance not found with id: " + id));

        // Update fields
        existingStockBalance.setQuantity(stockBalanceDto.getQuantity());
        existingStockBalance.setReservedQuantity(stockBalanceDto.getReservedQuantity());
        existingStockBalance.setAvailableQuantity(stockBalanceDto.getAvailableQuantity());
        existingStockBalance.setLastUpdatedAt(LocalDateTime.now());

        StockBalance updatedStockBalance = stockBalanceRepository.save(existingStockBalance);
        return convertToDto(updatedStockBalance);
    }

        // Điều chỉnh tồn kho
    public StockBalanceDto adjustStockBalance(StockAdjustmentDto adjustmentDto) {
        log.info("Adjusting stock balance: {}", adjustmentDto);

        // Get warehouse and stock location entities
        Warehouse warehouse = warehouseRepository.findById(adjustmentDto.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + adjustmentDto.getWarehouseId()));
        StockLocation stockLocation = stockLocationRepository.findById(adjustmentDto.getStockLocationId())
                .orElseThrow(() -> new RuntimeException("Stock location not found with id: " + adjustmentDto.getStockLocationId()));

        // Find existing stock balance
        Optional<StockBalance> existingStockBalance = stockBalanceRepository
                .findByProductUnitIdAndStockLocationAndWarehouse(
                    adjustmentDto.getProductUnitId(),
                    stockLocation,
                    warehouse);

        StockBalance stockBalance;

        if (existingStockBalance.isPresent()) {
            stockBalance = existingStockBalance.get();
            // Store old quantity for difference calculation
            adjustmentDto.setOldQuantity(stockBalance.getQuantity());
            stockBalance.setQuantity(adjustmentDto.getNewQuantity());
        } else {
            // Create new stock balance if doesn't exist
            stockBalance = new StockBalance();
            stockBalance.setProductUnitId(adjustmentDto.getProductUnitId());
            stockBalance.setStockLocation(stockLocation);
            stockBalance.setWarehouse(warehouse);
            stockBalance.setQuantity(adjustmentDto.getNewQuantity());
            stockBalance.setReservedQuantity(0);
            stockBalance.setAvailableQuantity(adjustmentDto.getNewQuantity());
            adjustmentDto.setOldQuantity(0);
        }

        stockBalance.setLastUpdatedAt(LocalDateTime.now());
        StockBalance savedStockBalance = stockBalanceRepository.save(stockBalance);

        return convertToDto(savedStockBalance);
    }

    // Đặt trước hàng (reserve stock)
    public StockBalanceDto reserveStock(Long productUnitId, Long stockLocationId, Long warehouseId, Integer quantity) {
        StockBalance stockBalance = stockBalanceRepository
                .findByProductUnitIdAndStockLocationAndWarehouse(productUnitId,
                        stockLocationRepository.findById(stockLocationId).orElseThrow(() -> new RuntimeException("Stock location not found")),
                        warehouseRepository.findById(warehouseId).orElseThrow(() -> new RuntimeException("Warehouse not found")))
                .orElseThrow(() -> new RuntimeException("Stock balance not found"));

        if (stockBalance.getAvailableQuantity() < quantity) {
            throw new RuntimeException("Insufficient available stock. Available: " +
                stockBalance.getAvailableQuantity() + ", Requested: " + quantity);
        }

        stockBalance.setReservedQuantity(stockBalance.getReservedQuantity() + quantity);
        stockBalance.setLastUpdatedAt(LocalDateTime.now());

        StockBalance savedStockBalance = stockBalanceRepository.save(stockBalance);
        return convertToDto(savedStockBalance);
    }

    // Giải phóng hàng đặt trước (release reserved stock)
    public StockBalanceDto releaseReservedStock(Long productUnitId, Long stockLocationId, Long warehouseId, Integer quantity) {
        StockBalance stockBalance = stockBalanceRepository
                .findByProductUnitIdAndStockLocationAndWarehouse(productUnitId,
                        stockLocationRepository.findById(stockLocationId).orElseThrow(() -> new RuntimeException("Stock location not found")),
                        warehouseRepository.findById(warehouseId).orElseThrow(() -> new RuntimeException("Warehouse not found")))
                .orElseThrow(() -> new RuntimeException("Stock balance not found"));

        if (stockBalance.getReservedQuantity() < quantity) {
            throw new RuntimeException("Insufficient reserved stock. Reserved: " +
                stockBalance.getReservedQuantity() + ", Requested: " + quantity);
        }

        stockBalance.setReservedQuantity(stockBalance.getReservedQuantity() - quantity);
        stockBalance.setLastUpdatedAt(LocalDateTime.now());

        StockBalance savedStockBalance = stockBalanceRepository.save(stockBalance);
        return convertToDto(savedStockBalance);
    }

    // Lấy tổng tồn kho theo sản phẩm
    public Integer getTotalQuantityByProduct(Long productUnitId) {
        return stockBalanceRepository.getTotalQuantityByProductUnitId(productUnitId);
    }

    // Lấy tổng tồn kho khả dụng theo sản phẩm
    public Integer getTotalAvailableQuantityByProduct(Long productUnitId) {
        return stockBalanceRepository.getTotalAvailableQuantityByProductUnitId(productUnitId);
    }

    // Lấy số lượng hiện tại theo bộ 3: productUnitId + stockLocationId + warehouseId
    public int getCurrentQuantity(Long productUnitId, Long stockLocationId, Long warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + warehouseId));
        StockLocation stockLocation = stockLocationRepository.findById(stockLocationId)
                .orElseThrow(() -> new RuntimeException("Stock location not found with id: " + stockLocationId));

        return stockBalanceRepository
                .findByProductUnitIdAndStockLocationAndWarehouse(productUnitId, stockLocation, warehouse)
                .map(StockBalance::getQuantity)
                .orElse(0);
    }

    // Convert entity to DTO
    private StockBalanceDto convertToDto(StockBalance stockBalance) {
        StockBalanceDto dto = new StockBalanceDto();
        dto.setId(stockBalance.getId());
        dto.setProductUnitId(stockBalance.getProductUnitId());
        dto.setStockLocationId(stockBalance.getStockLocation().getId());
        dto.setWarehouseId(stockBalance.getWarehouse().getId());
        dto.setQuantity(stockBalance.getQuantity());
        dto.setReservedQuantity(stockBalance.getReservedQuantity());
        dto.setAvailableQuantity(stockBalance.getAvailableQuantity());
        dto.setLastUpdatedAt(stockBalance.getLastUpdatedAt());
        dto.setCreatedAt(stockBalance.getCreatedAt());

        // Enrich names
        Warehouse wh = stockBalance.getWarehouse();
        if (wh != null) {
            dto.setWarehouseName(wh.getName());
        }
        StockLocation sl = stockBalance.getStockLocation();
        if (sl != null) {
            dto.setStockLocationName(sl.getName());
        }
        return dto;
    }
}

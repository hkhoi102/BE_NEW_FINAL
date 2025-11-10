package com.smartretail.inventoryservice.service;

import com.smartretail.inventoryservice.dto.StockLocationDto;
import com.smartretail.inventoryservice.model.StockLocation;
import com.smartretail.inventoryservice.model.Warehouse;
import com.smartretail.inventoryservice.repository.StockLocationRepository;
import com.smartretail.inventoryservice.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StockLocationService {

    private final StockLocationRepository stockLocationRepository;
    private final WarehouseRepository warehouseRepository;

    public StockLocationDto createStockLocation(StockLocationDto stockLocationDto) {
        Warehouse warehouse = warehouseRepository.findById(stockLocationDto.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + stockLocationDto.getWarehouseId()));

        if (stockLocationRepository.existsByNameAndWarehouseIdAndIdNot(
                stockLocationDto.getName(), stockLocationDto.getWarehouseId(), 0L)) {
            throw new RuntimeException("Stock location with name '" + stockLocationDto.getName() + "' already exists in this warehouse");
        }

        StockLocation stockLocation = new StockLocation();
        stockLocation.setName(stockLocationDto.getName());
        stockLocation.setDescription(stockLocationDto.getDescription());
        stockLocation.setWarehouse(warehouse);
        stockLocation.setZone(stockLocationDto.getZone());
        stockLocation.setAisle(stockLocationDto.getAisle());
        stockLocation.setRack(stockLocationDto.getRack());
        stockLocation.setLevel(stockLocationDto.getLevel());
        stockLocation.setPosition(stockLocationDto.getPosition());
        stockLocation.setActive(stockLocationDto.getActive());

        StockLocation savedLocation = stockLocationRepository.save(stockLocation);
        return convertToDto(savedLocation);
    }

    public List<StockLocationDto> getAllStockLocations() {
        return stockLocationRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<StockLocationDto> getStockLocationsByWarehouse(Long warehouseId) {
        return stockLocationRepository.findByWarehouseIdAndActiveTrue(warehouseId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<StockLocationDto> getAllStockLocationsByWarehouse(Long warehouseId) {
        return stockLocationRepository.findByWarehouseId(warehouseId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<StockLocationDto> getActiveStockLocations() {
        return stockLocationRepository.findAllActiveLocationsWithActiveWarehouse().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<StockLocationDto> getStockLocationById(Long id) {
        return stockLocationRepository.findById(id)
                .map(this::convertToDto);
    }

    public StockLocationDto updateStockLocation(Long id, StockLocationDto stockLocationDto) {
        StockLocation stockLocation = stockLocationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock location not found with id: " + id));

        Warehouse warehouse = warehouseRepository.findById(stockLocationDto.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + stockLocationDto.getWarehouseId()));

        if (!stockLocation.getName().equals(stockLocationDto.getName()) &&
            stockLocationRepository.existsByNameAndWarehouseIdAndIdNot(
                stockLocationDto.getName(), stockLocationDto.getWarehouseId(), id)) {
            throw new RuntimeException("Stock location with name '" + stockLocationDto.getName() + "' already exists in this warehouse");
        }

        stockLocation.setName(stockLocationDto.getName());
        stockLocation.setDescription(stockLocationDto.getDescription());
        stockLocation.setWarehouse(warehouse);
        stockLocation.setZone(stockLocationDto.getZone());
        stockLocation.setAisle(stockLocationDto.getAisle());
        stockLocation.setRack(stockLocationDto.getRack());
        stockLocation.setLevel(stockLocationDto.getLevel());
        stockLocation.setPosition(stockLocationDto.getPosition());
        stockLocation.setActive(stockLocationDto.getActive());

        StockLocation updatedLocation = stockLocationRepository.save(stockLocation);
        return convertToDto(updatedLocation);
    }

    public void deleteStockLocation(Long id) {
        StockLocation stockLocation = stockLocationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock location not found with id: " + id));

        // Check if location has stock balance
        // TODO: Add validation logic here

        stockLocation.setActive(false);
        stockLocationRepository.save(stockLocation);
    }

    public StockLocationDto activateStockLocation(Long id) {
        StockLocation stockLocation = stockLocationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock location not found with id: " + id));

        stockLocation.setActive(true);
        StockLocation updatedLocation = stockLocationRepository.save(stockLocation);
        return convertToDto(updatedLocation);
    }

    public StockLocationDto deactivateStockLocation(Long id) {
        StockLocation stockLocation = stockLocationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock location not found with id: " + id));

        stockLocation.setActive(false);
        StockLocation updatedLocation = stockLocationRepository.save(stockLocation);
        return convertToDto(updatedLocation);
    }

    public List<StockLocationDto> searchStockLocationsInWarehouse(Long warehouseId, String keyword) {
        return stockLocationRepository.searchActiveLocationsInWarehouse(warehouseId, keyword).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private StockLocationDto convertToDto(StockLocation stockLocation) {
        StockLocationDto dto = new StockLocationDto();
        dto.setId(stockLocation.getId());
        dto.setName(stockLocation.getName());
        dto.setDescription(stockLocation.getDescription());
        dto.setWarehouseId(stockLocation.getWarehouse().getId());
        dto.setZone(stockLocation.getZone());
        dto.setAisle(stockLocation.getAisle());
        dto.setRack(stockLocation.getRack());
        dto.setLevel(stockLocation.getLevel());
        dto.setPosition(stockLocation.getPosition());
        dto.setActive(stockLocation.getActive());
        dto.setCreatedAt(stockLocation.getCreatedAt());
        dto.setUpdatedAt(stockLocation.getUpdatedAt());
        dto.setWarehouseName(stockLocation.getWarehouse().getName());
        return dto;
    }
}

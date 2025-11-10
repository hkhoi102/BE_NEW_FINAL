package com.smartretail.inventoryservice.service;

import com.smartretail.inventoryservice.dto.WarehouseDto;
import com.smartretail.inventoryservice.model.Warehouse;
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
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseDto createWarehouse(WarehouseDto warehouseDto) {
        if (warehouseRepository.existsByName(warehouseDto.getName())) {
            throw new RuntimeException("Warehouse with name '" + warehouseDto.getName() + "' already exists");
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setName(warehouseDto.getName());
        warehouse.setDescription(warehouseDto.getDescription());
        warehouse.setAddress(warehouseDto.getAddress());
        warehouse.setPhone(warehouseDto.getPhone());
        warehouse.setContactPerson(warehouseDto.getContactPerson());
        warehouse.setActive(warehouseDto.getActive());

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);
        return convertToDto(savedWarehouse);
    }

    public List<WarehouseDto> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<WarehouseDto> getActiveWarehouses() {
        return warehouseRepository.findByActiveTrue().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<WarehouseDto> getWarehouseById(Long id) {
        return warehouseRepository.findById(id)
                .map(this::convertToDto);
    }

    public WarehouseDto updateWarehouse(Long id, WarehouseDto warehouseDto) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + id));

        if (!warehouse.getName().equals(warehouseDto.getName()) &&
            warehouseRepository.existsByNameAndIdNot(warehouseDto.getName(), id)) {
            throw new RuntimeException("Warehouse with name '" + warehouseDto.getName() + "' already exists");
        }

        warehouse.setName(warehouseDto.getName());
        warehouse.setDescription(warehouseDto.getDescription());
        warehouse.setAddress(warehouseDto.getAddress());
        warehouse.setPhone(warehouseDto.getPhone());
        warehouse.setContactPerson(warehouseDto.getContactPerson());
        warehouse.setActive(warehouseDto.getActive());

        Warehouse updatedWarehouse = warehouseRepository.save(warehouse);
        return convertToDto(updatedWarehouse);
    }

    public void deleteWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + id));

        // Check if warehouse has stock locations
        // TODO: Add validation logic here

        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
    }

    public List<WarehouseDto> searchWarehouses(String keyword) {
        return warehouseRepository.searchActiveWarehouses(keyword).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private WarehouseDto convertToDto(Warehouse warehouse) {
        WarehouseDto dto = new WarehouseDto();
        dto.setId(warehouse.getId());
        dto.setName(warehouse.getName());
        dto.setDescription(warehouse.getDescription());
        dto.setAddress(warehouse.getAddress());
        dto.setPhone(warehouse.getPhone());
        dto.setContactPerson(warehouse.getContactPerson());
        dto.setActive(warehouse.getActive());
        dto.setCreatedAt(warehouse.getCreatedAt());
        dto.setUpdatedAt(warehouse.getUpdatedAt());
        return dto;
    }
}

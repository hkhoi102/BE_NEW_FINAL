package com.smartretail.serviceproduct.service;

import com.smartretail.serviceproduct.dto.UnitDto;
import com.smartretail.serviceproduct.model.Unit;
import com.smartretail.serviceproduct.repository.UnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UnitService {

    @Autowired
    private UnitRepository unitRepository;

    // Create new unit
    public UnitDto createUnit(UnitDto unitDto) {
        if (unitRepository.existsByName(unitDto.getName())) {
            throw new RuntimeException("Unit with name '" + unitDto.getName() + "' already exists");
        }

        Unit unit = new Unit();
        unit.setName(unitDto.getName());
        unit.setDescription(unitDto.getDescription());
        if (unitDto.getIsDefault() != null) {
            unit.setIsDefault(unitDto.getIsDefault());
        }

        Unit savedUnit = unitRepository.save(unit);
        return convertToDto(savedUnit);
    }

    // Get all active units
    public List<UnitDto> getAllUnits() {
        List<Unit> units = unitRepository.findByActiveTrue();
        return units.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Get unit by ID
    public Optional<UnitDto> getUnitById(Long id) {
        Optional<Unit> unit = unitRepository.findById(id);
        return unit.map(this::convertToDto);
    }

    // Update unit
    public Optional<UnitDto> updateUnit(Long id, UnitDto unitDto) {
        Optional<Unit> existingUnit = unitRepository.findById(id);
        if (existingUnit.isPresent()) {
            Unit unit = existingUnit.get();

            // Check if name is being changed and if it conflicts with existing names
            if (!unit.getName().equals(unitDto.getName()) &&
                unitRepository.existsByName(unitDto.getName())) {
                throw new RuntimeException("Unit with name '" + unitDto.getName() + "' already exists");
            }

            unit.setName(unitDto.getName());
            unit.setDescription(unitDto.getDescription());
            if (unitDto.getIsDefault() != null) {
                unit.setIsDefault(unitDto.getIsDefault());
            }

            Unit updatedUnit = unitRepository.save(unit);
            return Optional.of(convertToDto(updatedUnit));
        }
        return Optional.empty();
    }

    // Delete unit (soft delete)
    public boolean deleteUnit(Long id) {
        Optional<Unit> unit = unitRepository.findById(id);
        if (unit.isPresent()) {
            Unit u = unit.get();
            u.setActive(false);
            unitRepository.save(u);
            return true;
        }
        return false;
    }

    // Convert entity to DTO
    private UnitDto convertToDto(Unit unit) {
        return new UnitDto(
            unit.getId(),
            unit.getName(),
            unit.getDescription(),
            unit.getIsDefault()
        );
    }
}

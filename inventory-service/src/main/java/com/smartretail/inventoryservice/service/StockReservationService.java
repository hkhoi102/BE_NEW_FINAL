package com.smartretail.inventoryservice.service;

import com.smartretail.inventoryservice.model.StockBalance;
import com.smartretail.inventoryservice.model.StockLot;
import com.smartretail.inventoryservice.repository.StockBalanceRepository;
import com.smartretail.inventoryservice.repository.StockLotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockReservationService {

    private final StockBalanceRepository stockBalanceRepository;
    private final StockLotRepository stockLotRepository;

    /**
     * Reserve stock khi tạo phiếu xuất (chưa duyệt)
     * - Giảm available_quantity
     * - Tăng reserved_quantity
     * - Giữ nguyên quantity
     */
    @Transactional
    public ReserveResult reserveStock(Long productUnitId, Long warehouseId, Long stockLocationId, Integer requiredQuantity) {
        log.info("Reserving {} units for product {} at warehouse {} location {}",
                requiredQuantity, productUnitId, warehouseId, stockLocationId);

        if (requiredQuantity == null || requiredQuantity <= 0) {
            throw new RuntimeException("Required quantity must be greater than 0");
        }

        // 1. Kiểm tra available_quantity từ stock_balance
        StockBalance stockBalance = stockBalanceRepository
                .findByProductUnitIdAndStockLocation_IdAndWarehouse_Id(productUnitId, stockLocationId, warehouseId)
                .orElseThrow(() -> new RuntimeException("Stock balance not found for product " + productUnitId +
                        " at warehouse " + warehouseId + " location " + stockLocationId));

        if (stockBalance.getAvailableQuantity() < requiredQuantity) {
            throw new RuntimeException("Số sản phẩm yêu cầu vượt quá số lượng trong kho. " +
                    "Số lượng yêu cầu: " + requiredQuantity +
                    ", Số lượng trong kho còn: " + stockBalance.getAvailableQuantity() +
                    " (ProductUnitId: " + productUnitId + ")");
        }

        // 2. Reserve trong stock_lots theo FEFO
        List<StockLot> availableLots = stockLotRepository.findAvailableLotsForFEFO(
                productUnitId, warehouseId, stockLocationId);

        int totalAvailableFromLots = availableLots.stream()
                .mapToInt(StockLot::getAvailableQuantity)
                .sum();

        if (totalAvailableFromLots < requiredQuantity) {
            throw new RuntimeException("Số sản phẩm yêu cầu vượt quá số lượng trong kho. " +
                    "Số lượng yêu cầu: " + requiredQuantity +
                    ", Số lượng trong kho còn: " + totalAvailableFromLots +
                    " (ProductUnitId: " + productUnitId + ")");
        }

        // 3. Reserve từng lô theo FEFO
        List<LotReservation> lotReservations = new ArrayList<>();
        int remainingQuantity = requiredQuantity;

        for (StockLot lot : availableLots) {
            if (remainingQuantity <= 0) break;

            int availableInLot = lot.getAvailableQuantity();
            int toReserve = Math.min(remainingQuantity, availableInLot);

            if (toReserve > 0) {
                // Reserve trong lot
                lot.setReservedQuantity(lot.getReservedQuantity() + toReserve);
                lot.setAvailableQuantity(lot.getCurrentQuantity() - lot.getReservedQuantity());
                stockLotRepository.save(lot);

                lotReservations.add(new LotReservation(lot.getId(), lot.getLotNumber(), toReserve));
                remainingQuantity -= toReserve;

                log.info("Reserved {} units from lot {} (ID: {})", toReserve, lot.getLotNumber(), lot.getId());
            }
        }

        // 4. Reserve trong stock_balance
        stockBalance.setReservedQuantity(stockBalance.getReservedQuantity() + requiredQuantity);
        stockBalance.setAvailableQuantity(stockBalance.getQuantity() - stockBalance.getReservedQuantity());
        stockBalanceRepository.save(stockBalance);

        log.info("Reserved {} units in stock balance for product {} at location {}",
                requiredQuantity, productUnitId, stockLocationId);

        return new ReserveResult(requiredQuantity, lotReservations);
    }

    /**
     * Consume stock khi duyệt phiếu xuất
     * - Giảm reserved_quantity
     * - Giảm quantity
     * - available_quantity = quantity - reserved_quantity
     */
    @Transactional
    public ConsumeResult consumeReservedStock(Long productUnitId, Long warehouseId, Long stockLocationId,
                                            Integer quantityToConsume, List<LotReservation> lotReservations) {
        log.info("Consuming {} reserved units for product {} at warehouse {} location {}",
                quantityToConsume, productUnitId, warehouseId, stockLocationId);

        if (quantityToConsume == null || quantityToConsume <= 0) {
            throw new RuntimeException("Quantity to consume must be greater than 0");
        }

        // 1. Consume trong stock_lots
        int totalConsumedFromLots = 0;
        for (LotReservation reservation : lotReservations) {
            StockLot lot = stockLotRepository.findById(reservation.getLotId())
                    .orElseThrow(() -> new RuntimeException("Lot not found: " + reservation.getLotId()));

            if (lot.getReservedQuantity() < reservation.getReservedQuantity()) {
                throw new RuntimeException("Lot " + lot.getLotNumber() + " has insufficient reserved quantity");
            }

            // Consume reserved quantity
            lot.setReservedQuantity(lot.getReservedQuantity() - reservation.getReservedQuantity());
            lot.setCurrentQuantity(lot.getCurrentQuantity() - reservation.getReservedQuantity());
            lot.setAvailableQuantity(lot.getCurrentQuantity() - lot.getReservedQuantity());

            // Update lot status if depleted
            if (lot.getCurrentQuantity() <= 0) {
                lot.setStatus(StockLot.LotStatus.DEPLETED);
            }

            stockLotRepository.save(lot);
            totalConsumedFromLots += reservation.getReservedQuantity();

            log.info("Consumed {} units from lot {} (ID: {})", reservation.getReservedQuantity(),
                    lot.getLotNumber(), lot.getId());
        }

        // 2. Consume trong stock_balance
        StockBalance stockBalance = stockBalanceRepository
                .findByProductUnitIdAndStockLocation_IdAndWarehouse_Id(productUnitId, stockLocationId, warehouseId)
                .orElseThrow(() -> new RuntimeException("Stock balance not found for product " + productUnitId +
                        " at warehouse " + warehouseId + " location " + stockLocationId));

        if (stockBalance.getReservedQuantity() < quantityToConsume) {
            throw new RuntimeException("Insufficient reserved quantity in stock balance");
        }

        // Log trước khi consume
        log.info("Before consume - StockBalance: quantity={}, reserved={}, available={}",
                stockBalance.getQuantity(), stockBalance.getReservedQuantity(), stockBalance.getAvailableQuantity());

        stockBalance.setQuantity(stockBalance.getQuantity() - quantityToConsume);
        stockBalance.setReservedQuantity(stockBalance.getReservedQuantity() - quantityToConsume);
        stockBalance.setAvailableQuantity(stockBalance.getQuantity() - stockBalance.getReservedQuantity());

        // Log sau khi consume
        log.info("After consume - StockBalance: quantity={}, reserved={}, available={}",
                stockBalance.getQuantity(), stockBalance.getReservedQuantity(), stockBalance.getAvailableQuantity());

        StockBalance savedBalance = stockBalanceRepository.save(stockBalance);

        // Log sau khi save
        log.info("After save - StockBalance: quantity={}, reserved={}, available={}",
                savedBalance.getQuantity(), savedBalance.getReservedQuantity(), savedBalance.getAvailableQuantity());

        log.info("Consumed {} units in stock balance for product {} at location {}",
                quantityToConsume, productUnitId, stockLocationId);

        return new ConsumeResult(quantityToConsume, totalConsumedFromLots);
    }

    /**
     * Release reservation khi hủy phiếu xuất
     */
    @Transactional
    public void releaseReservation(Long productUnitId, Long warehouseId, Long stockLocationId,
                                 Integer quantityToRelease, List<LotReservation> lotReservations) {
        log.info("Releasing {} reserved units for product {} at warehouse {} location {}",
                quantityToRelease, productUnitId, warehouseId, stockLocationId);

        // 1. Release trong stock_lots
        for (LotReservation reservation : lotReservations) {
            StockLot lot = stockLotRepository.findById(reservation.getLotId())
                    .orElseThrow(() -> new RuntimeException("Lot not found: " + reservation.getLotId()));

            lot.setReservedQuantity(lot.getReservedQuantity() - reservation.getReservedQuantity());
            lot.setAvailableQuantity(lot.getCurrentQuantity() - lot.getReservedQuantity());
            stockLotRepository.save(lot);

            log.info("Released {} units from lot {} (ID: {})", reservation.getReservedQuantity(),
                    lot.getLotNumber(), lot.getId());
        }

        // 2. Release trong stock_balance
        StockBalance stockBalance = stockBalanceRepository
                .findByProductUnitIdAndStockLocation_IdAndWarehouse_Id(productUnitId, stockLocationId, warehouseId)
                .orElseThrow(() -> new RuntimeException("Stock balance not found for product " + productUnitId +
                        " at warehouse " + warehouseId + " location " + stockLocationId));

        stockBalance.setReservedQuantity(stockBalance.getReservedQuantity() - quantityToRelease);
        stockBalance.setAvailableQuantity(stockBalance.getQuantity() - stockBalance.getReservedQuantity());
        stockBalanceRepository.save(stockBalance);

        log.info("Released {} units in stock balance for product {} at location {}",
                quantityToRelease, productUnitId, stockLocationId);
    }

    /**
     * Kiểm tra available_quantity trước khi reserve
     */
    public boolean checkAvailableQuantity(Long productUnitId, Long warehouseId, Long stockLocationId, Integer requiredQuantity) {
        // Kiểm tra từ stock_balance
        StockBalance stockBalance = stockBalanceRepository
                .findByProductUnitIdAndStockLocation_IdAndWarehouse_Id(productUnitId, stockLocationId, warehouseId)
                .orElse(null);

        if (stockBalance == null || stockBalance.getAvailableQuantity() < requiredQuantity) {
            return false;
        }

        // Kiểm tra từ stock_lots
        List<StockLot> availableLots = stockLotRepository.findAvailableLotsForFEFO(
                productUnitId, warehouseId, stockLocationId);

        int totalAvailableFromLots = availableLots.stream()
                .mapToInt(StockLot::getAvailableQuantity)
                .sum();

        return totalAvailableFromLots >= requiredQuantity;
    }

    /**
     * Lấy thông tin available quantity
     */
    public AvailableQuantityInfo getAvailableQuantityInfo(Long productUnitId, Long warehouseId, Long stockLocationId) {
        // Từ stock_balance
        StockBalance stockBalance = stockBalanceRepository
                .findByProductUnitIdAndStockLocation_IdAndWarehouse_Id(productUnitId, stockLocationId, warehouseId)
                .orElse(null);

        int availableFromBalance = stockBalance != null ? stockBalance.getAvailableQuantity() : 0;

        // Từ stock_lots
        List<StockLot> availableLots = stockLotRepository.findAvailableLotsForFEFO(
                productUnitId, warehouseId, stockLocationId);

        int availableFromLots = availableLots.stream()
                .mapToInt(StockLot::getAvailableQuantity)
                .sum();

        return new AvailableQuantityInfo(availableFromBalance, availableFromLots, availableLots.size());
    }

    // Inner classes for results
    public static class ReserveResult {
        private final Integer totalReserved;
        private final List<LotReservation> lotReservations;

        public ReserveResult(Integer totalReserved, List<LotReservation> lotReservations) {
            this.totalReserved = totalReserved;
            this.lotReservations = lotReservations;
        }

        public Integer getTotalReserved() { return totalReserved; }
        public List<LotReservation> getLotReservations() { return lotReservations; }
    }

    public static class ConsumeResult {
        private final Integer totalConsumed;
        private final Integer consumedFromLots;

        public ConsumeResult(Integer totalConsumed, Integer consumedFromLots) {
            this.totalConsumed = totalConsumed;
            this.consumedFromLots = consumedFromLots;
        }

        public Integer getTotalConsumed() { return totalConsumed; }
        public Integer getConsumedFromLots() { return consumedFromLots; }
    }

    public static class LotReservation {
        private final Long lotId;
        private final String lotNumber;
        private final Integer reservedQuantity;

        public LotReservation(Long lotId, String lotNumber, Integer reservedQuantity) {
            this.lotId = lotId;
            this.lotNumber = lotNumber;
            this.reservedQuantity = reservedQuantity;
        }

        public Long getLotId() { return lotId; }
        public String getLotNumber() { return lotNumber; }
        public Integer getReservedQuantity() { return reservedQuantity; }
    }

    public static class AvailableQuantityInfo {
        private final Integer availableFromBalance;
        private final Integer availableFromLots;
        private final Integer numberOfLots;

        public AvailableQuantityInfo(Integer availableFromBalance, Integer availableFromLots, Integer numberOfLots) {
            this.availableFromBalance = availableFromBalance;
            this.availableFromLots = availableFromLots;
            this.numberOfLots = numberOfLots;
        }

        public Integer getAvailableFromBalance() { return availableFromBalance; }
        public Integer getAvailableFromLots() { return availableFromLots; }
        public Integer getNumberOfLots() { return numberOfLots; }
    }
}

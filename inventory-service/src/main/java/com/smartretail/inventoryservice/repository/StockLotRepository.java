package com.smartretail.inventoryservice.repository;

import com.smartretail.inventoryservice.model.StockLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockLotRepository extends JpaRepository<StockLot, Long> {

    // Tìm lô theo số lô
    Optional<StockLot> findByLotNumber(String lotNumber);

    // Tìm lô theo số lô và thông tin sản phẩm/kho/vị trí chính xác
    Optional<StockLot> findByLotNumberAndProductUnitIdAndWarehouseIdAndStockLocationId(
            String lotNumber, Long productUnitId, Long warehouseId, Long stockLocationId);

    // Tìm lô theo product unit và kho
    List<StockLot> findByProductUnitIdAndWarehouseIdAndStockLocationIdAndStatus(
            Long productUnitId, Long warehouseId, Long stockLocationId, StockLot.LotStatus status);

    // FEFO: Tìm lô có sẵn theo thứ tự hết hạn sớm nhất (First Expire, First Out)
    @Query("SELECT l FROM StockLot l WHERE l.productUnitId = :productUnitId " +
           "AND l.warehouseId = :warehouseId AND l.stockLocationId = :stockLocationId " +
           "AND l.status = 'ACTIVE' AND l.availableQuantity > 0 " +
           "ORDER BY l.expiryDate ASC NULLS LAST, l.createdAt ASC")
    List<StockLot> findAvailableLotsForFEFO(@Param("productUnitId") Long productUnitId,
                                           @Param("warehouseId") Long warehouseId,
                                           @Param("stockLocationId") Long stockLocationId);

    // FEFO: Lấy tất cả lô có sẵn theo sản phẩm trên mọi kho & vị trí
    @Query("SELECT l FROM StockLot l WHERE l.productUnitId = :productUnitId " +
           "AND l.status = 'ACTIVE' AND l.availableQuantity > 0 " +
           "ORDER BY l.expiryDate ASC NULLS LAST, l.createdAt ASC")
    List<StockLot> findAvailableLotsForFEFOAll(@Param("productUnitId") Long productUnitId);

    // FEFO: Lấy lô có sẵn theo sản phẩm và kho (mọi vị trí trong kho)
    @Query("SELECT l FROM StockLot l WHERE l.productUnitId = :productUnitId " +
           "AND l.warehouseId = :warehouseId " +
           "AND l.status = 'ACTIVE' AND l.availableQuantity > 0 " +
           "ORDER BY l.expiryDate ASC NULLS LAST, l.createdAt ASC")
    List<StockLot> findAvailableLotsForFEFOByWarehouse(@Param("productUnitId") Long productUnitId,
                                                       @Param("warehouseId") Long warehouseId);

    // FEFO: Lấy lô có sẵn theo sản phẩm và vị trí (mọi kho chứa vị trí này nếu mô hình cho phép)
    @Query("SELECT l FROM StockLot l WHERE l.productUnitId = :productUnitId " +
           "AND l.stockLocationId = :stockLocationId " +
           "AND l.status = 'ACTIVE' AND l.availableQuantity > 0 " +
           "ORDER BY l.expiryDate ASC NULLS LAST, l.createdAt ASC")
    List<StockLot> findAvailableLotsForFEFOByLocation(@Param("productUnitId") Long productUnitId,
                                                      @Param("stockLocationId") Long stockLocationId);

    // Tìm lô sắp hết hạn
    @Query("SELECT l FROM StockLot l WHERE l.expiryDate IS NOT NULL " +
           "AND l.expiryDate <= :expiryDate AND l.status = 'ACTIVE' " +
           "AND l.availableQuantity > 0 " +
           "ORDER BY l.expiryDate ASC")
    List<StockLot> findLotsNearExpiry(@Param("expiryDate") LocalDate expiryDate);

    // Tìm lô đã hết hạn
    @Query("SELECT l FROM StockLot l WHERE l.expiryDate IS NOT NULL " +
           "AND l.expiryDate < :currentDate AND l.status = 'ACTIVE' " +
           "ORDER BY l.expiryDate ASC")
    List<StockLot> findExpiredLots(@Param("currentDate") LocalDate currentDate);

    // Tìm lô theo supplier batch
    List<StockLot> findBySupplierBatchNumberAndProductUnitId(String supplierBatchNumber, Long productUnitId);

    // Đếm số lô theo trạng thái
    @Query("SELECT COUNT(l) FROM StockLot l WHERE l.status = :status")
    long countByStatus(@Param("status") StockLot.LotStatus status);

    // Tìm lô theo kho và vị trí
    List<StockLot> findByWarehouseIdAndStockLocationIdAndStatus(Long warehouseId, Long stockLocationId, StockLot.LotStatus status);

    // Tìm lô có sẵn cho một sản phẩm cụ thể
    @Query("SELECT l FROM StockLot l WHERE l.productUnitId = :productUnitId " +
           "AND l.status = 'ACTIVE' AND l.availableQuantity > 0 " +
           "ORDER BY l.expiryDate ASC NULLS LAST, l.createdAt ASC")
    List<StockLot> findAvailableLotsByProduct(@Param("productUnitId") Long productUnitId);

    // Tìm lô theo kho
    List<StockLot> findByWarehouseIdAndStatus(Long warehouseId, StockLot.LotStatus status);

    // Tìm lô theo vị trí kho
    List<StockLot> findByStockLocationIdAndStatus(Long stockLocationId, StockLot.LotStatus status);

    // Tìm lô có số lượng dự trữ
    @Query("SELECT l FROM StockLot l WHERE l.productUnitId = :productUnitId " +
           "AND l.warehouseId = :warehouseId AND l.stockLocationId = :stockLocationId " +
           "AND l.status = 'ACTIVE' AND l.reservedQuantity > 0 " +
           "ORDER BY l.expiryDate ASC NULLS LAST")
    List<StockLot> findReservedLots(@Param("productUnitId") Long productUnitId,
                                   @Param("warehouseId") Long warehouseId,
                                   @Param("stockLocationId") Long stockLocationId);

    // Tìm lô theo khoảng thời gian hết hạn
    @Query("SELECT l FROM StockLot l WHERE l.expiryDate IS NOT NULL " +
           "AND l.expiryDate BETWEEN :startDate AND :endDate " +
           "AND l.status = 'ACTIVE' " +
           "ORDER BY l.expiryDate ASC")
    List<StockLot> findLotsByExpiryDateRange(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
}

package com.smartretail.inventoryservice.repository;

import com.smartretail.inventoryservice.model.StockBalance;
import com.smartretail.inventoryservice.model.StockLocation;
import com.smartretail.inventoryservice.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    // Tìm theo sản phẩm
    List<StockBalance> findByProductUnitId(Long productUnitId);

    // Tìm theo kho (theo id của entity)
    List<StockBalance> findByWarehouse_Id(Long warehouseId);

    // Tìm theo vị trí kho (theo id của entity)
    List<StockBalance> findByStockLocation_Id(Long stockLocationId);

    // Tìm theo sản phẩm và kho
    List<StockBalance> findByProductUnitIdAndWarehouse_Id(Long productUnitId, Long warehouseId);

    // Tìm theo sản phẩm và vị trí
    Optional<StockBalance> findByProductUnitIdAndStockLocation_Id(Long productUnitId, Long stockLocationId);

    // Tìm theo sản phẩm, vị trí và kho
    Optional<StockBalance> findByProductUnitIdAndStockLocation_IdAndWarehouse_Id(Long productUnitId, Long stockLocationId, Long warehouseId);

    // Tìm theo kho và vị trí
    List<StockBalance> findByWarehouse_IdAndStockLocation_Id(Long warehouseId, Long stockLocationId);

    // Tìm theo sản phẩm, kho và vị trí
    Optional<StockBalance> findByProductUnitIdAndStockLocationAndWarehouse(
            Long productUnitId, StockLocation stockLocation, Warehouse warehouse);

    // Tìm theo sản phẩm có tồn kho > 0
    List<StockBalance> findByProductUnitIdAndQuantityGreaterThan(Long productUnitId, Integer quantity);

    // Tìm theo kho có tồn kho > 0
    List<StockBalance> findByWarehouseIdAndQuantityGreaterThan(Long warehouseId, Integer quantity);

    // Tìm theo vị trí có tồn kho > 0
    List<StockBalance> findByStockLocationIdAndQuantityGreaterThan(Long stockLocationId, Integer quantity);

    // Tìm theo sản phẩm có tồn kho khả dụng > 0
    List<StockBalance> findByProductUnitIdAndAvailableQuantityGreaterThan(Long productUnitId, Integer availableQuantity);

    // Tìm theo kho có tồn kho khả dụng > 0
    List<StockBalance> findByWarehouseIdAndAvailableQuantityGreaterThan(Long warehouseId, Integer availableQuantity);

    // Tìm theo vị trí có tồn kho khả dụng > 0
    List<StockBalance> findByStockLocationIdAndAvailableQuantityGreaterThan(Long stockLocationId, Integer availableQuantity);

    // Tìm theo sản phẩm có tồn kho đặt trước > 0
    List<StockBalance> findByProductUnitIdAndReservedQuantityGreaterThan(Long productUnitId, Integer reservedQuantity);

    // Tìm theo kho có tồn kho đặt trước > 0
    List<StockBalance> findByWarehouseIdAndReservedQuantityGreaterThan(Long warehouseId, Integer reservedQuantity);

    // Tìm theo vị trí có tồn kho đặt trước > 0
    List<StockBalance> findByStockLocationIdAndReservedQuantityGreaterThan(Long stockLocationId, Integer reservedQuantity);

    // Tính tổng tồn kho theo sản phẩm
    @Query("SELECT COALESCE(SUM(sb.quantity), 0) FROM StockBalance sb WHERE sb.productUnitId = :productUnitId")
    Integer getTotalQuantityByProductUnitId(@Param("productUnitId") Long productUnitId);

    // Tính tổng tồn kho khả dụng theo sản phẩm
    @Query("SELECT COALESCE(SUM(sb.availableQuantity), 0) FROM StockBalance sb WHERE sb.productUnitId = :productUnitId")
    Integer getTotalAvailableQuantityByProductUnitId(@Param("productUnitId") Long productUnitId);

    // Tính tổng tồn kho đặt trước theo sản phẩm
    @Query("SELECT COALESCE(SUM(sb.reservedQuantity), 0) FROM StockBalance sb WHERE sb.productUnitId = :productUnitId")
    Integer getTotalReservedQuantityByProductUnitId(@Param("productUnitId") Long productUnitId);

    // Tính tổng tồn kho theo kho
    @Query("SELECT COALESCE(SUM(sb.quantity), 0) FROM StockBalance sb WHERE sb.warehouse.id = :warehouseId")
    Integer getTotalQuantityByWarehouseId(@Param("warehouseId") Long warehouseId);

    // Tính tổng tồn kho theo vị trí
    @Query("SELECT COALESCE(SUM(sb.quantity), 0) FROM StockBalance sb WHERE sb.stockLocation.id = :stockLocationId")
    Integer getTotalQuantityByStockLocationId(@Param("stockLocationId") Long stockLocationId);

    // Đếm số sản phẩm có tồn kho > 0
    long countByQuantityGreaterThan(Integer quantity);

    // Đếm số sản phẩm có tồn kho khả dụng > 0
    long countByAvailableQuantityGreaterThan(Integer availableQuantity);

    // Đếm số sản phẩm có tồn kho đặt trước > 0
    long countByReservedQuantityGreaterThan(Integer reservedQuantity);

    // Aggregation projection for product stock summary
    interface ProductStockAggregation {
        Long getProductUnitId();
        Long getTotalQuantity();
        Long getAvailableQuantity();
        Long getReservedQuantity();
    }

    // Summaries grouped by product, optional warehouse/location filters
    @Query("SELECT sb.productUnitId as productUnitId, "+
           "COALESCE(SUM(sb.quantity), 0) as totalQuantity, "+
           "COALESCE(SUM(sb.availableQuantity), 0) as availableQuantity, "+
           "COALESCE(SUM(sb.reservedQuantity), 0) as reservedQuantity "+
           "FROM StockBalance sb "+
           "WHERE (:warehouseId IS NULL OR sb.warehouse.id = :warehouseId) "+
           "AND (:stockLocationId IS NULL OR sb.stockLocation.id = :stockLocationId) "+
           "GROUP BY sb.productUnitId")
    List<ProductStockAggregation> getProductStockSummaries(@Param("warehouseId") Long warehouseId,
                                                           @Param("stockLocationId") Long stockLocationId);

    // Low stock alerts by available quantity threshold
    @Query("SELECT sb.productUnitId as productUnitId, "+
           "COALESCE(SUM(sb.quantity), 0) as totalQuantity, "+
           "COALESCE(SUM(sb.availableQuantity), 0) as availableQuantity, "+
           "COALESCE(SUM(sb.reservedQuantity), 0) as reservedQuantity "+
           "FROM StockBalance sb "+
           "WHERE (:warehouseId IS NULL OR sb.warehouse.id = :warehouseId) "+
           "AND (:stockLocationId IS NULL OR sb.stockLocation.id = :stockLocationId) "+
           "GROUP BY sb.productUnitId "+
           "HAVING COALESCE(SUM(sb.availableQuantity), 0) <= :threshold")
    List<ProductStockAggregation> getLowStockProducts(@Param("warehouseId") Long warehouseId,
                                                      @Param("stockLocationId") Long stockLocationId,
                                                      @Param("threshold") Integer threshold);
}

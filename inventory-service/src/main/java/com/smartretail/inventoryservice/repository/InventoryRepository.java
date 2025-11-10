package com.smartretail.inventoryservice.repository;

import com.smartretail.inventoryservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // Tìm theo loại giao dịch
    List<Inventory> findByTransactionType(Inventory.TransactionType transactionType);

    // Tìm theo kho (theo id của entity)
    List<Inventory> findByWarehouse_Id(Long warehouseId);

    // Tìm theo vị trí kho (theo id của entity)
    List<Inventory> findByStockLocation_Id(Long stockLocationId);

    // Tìm theo sản phẩm
    List<Inventory> findByProductUnitId(Long productUnitId);

    // Tìm theo kho và vị trí
    List<Inventory> findByWarehouse_IdAndStockLocation_Id(Long warehouseId, Long stockLocationId);

    // Tìm theo kho và sản phẩm
    List<Inventory> findByWarehouse_IdAndProductUnitId(Long warehouseId, Long productUnitId);

    // Tìm theo vị trí và sản phẩm
    List<Inventory> findByStockLocation_IdAndProductUnitId(Long stockLocationId, Long productUnitId);

    // Tìm theo kho, vị trí và sản phẩm
    List<Inventory> findByWarehouse_IdAndStockLocation_IdAndProductUnitId(
        Long warehouseId, Long stockLocationId, Long productUnitId);

    // Tìm theo thời gian giao dịch
    List<Inventory> findByTransactionDateBetween(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    // Tìm theo số tham chiếu
    List<Inventory> findByReferenceNumberContainingIgnoreCase(String referenceNumber);

    // Tìm theo ghi chú
    List<Inventory> findByNoteContainingIgnoreCase(String note);

    // Đếm số giao dịch theo loại
    long countByTransactionType(Inventory.TransactionType transactionType);

    // Đếm số giao dịch theo kho
    long countByWarehouse_Id(Long warehouseId);

    // Đếm số giao dịch theo vị trí
    long countByStockLocation_Id(Long stockLocationId);
}

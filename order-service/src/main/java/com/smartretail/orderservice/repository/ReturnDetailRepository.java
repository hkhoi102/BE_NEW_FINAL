package com.smartretail.orderservice.repository;

import com.smartretail.orderservice.model.ReturnDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnDetailRepository extends JpaRepository<ReturnDetail, Long> {

    // Tìm return details theo return order ID
    List<ReturnDetail> findByReturnOrderIdOrderById(Long returnOrderId);

    // Tìm return details theo order detail ID
    List<ReturnDetail> findByOrderDetailIdOrderById(Long orderDetailId);

    // Xóa tất cả return details của một return order
    void deleteByReturnOrderId(Long returnOrderId);

    // Đếm số lượng sản phẩm trong return order
    @Query("SELECT COUNT(rd) FROM ReturnDetail rd WHERE rd.returnOrderId = :returnOrderId")
    Long countByReturnOrderId(@Param("returnOrderId") Long returnOrderId);

    // Tính tổng số lượng sản phẩm trong return order
    @Query("SELECT SUM(rd.quantity) FROM ReturnDetail rd WHERE rd.returnOrderId = :returnOrderId")
    Integer sumQuantityByReturnOrderId(@Param("returnOrderId") Long returnOrderId);

    // Tính tổng tiền hoàn trả của return order
    @Query("SELECT SUM(rd.refundAmount) FROM ReturnDetail rd WHERE rd.returnOrderId = :returnOrderId")
    java.math.BigDecimal sumRefundAmountByReturnOrderId(@Param("returnOrderId") Long returnOrderId);

    // Tìm return details theo product unit ID
    @Query("SELECT rd FROM ReturnDetail rd JOIN OrderDetail od ON rd.orderDetailId = od.id WHERE od.productUnitId = :productUnitId ORDER BY rd.id DESC")
    List<ReturnDetail> findByProductUnitId(@Param("productUnitId") Long productUnitId);
}

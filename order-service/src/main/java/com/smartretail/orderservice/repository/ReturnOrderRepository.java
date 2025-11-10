package com.smartretail.orderservice.repository;

import com.smartretail.orderservice.model.ReturnOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnOrderRepository extends JpaRepository<ReturnOrder, Long> {

    // Tìm return orders theo customer ID
    List<ReturnOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    // Tìm return orders theo customer ID với phân trang
    Page<ReturnOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    // Tìm return orders theo order ID
    @Query("SELECT ro FROM ReturnOrder ro WHERE ro.order.id = :orderId ORDER BY ro.createdAt DESC")
    List<ReturnOrder> findByOrderIdOrderByCreatedAtDesc(@Param("orderId") Long orderId);

    // Tìm return orders theo trạng thái
    List<ReturnOrder> findByStatusOrderByCreatedAtDesc(ReturnOrder.ReturnStatus status);

    // Tìm return orders theo trạng thái với phân trang
    Page<ReturnOrder> findByStatusOrderByCreatedAtDesc(ReturnOrder.ReturnStatus status, Pageable pageable);

    // Tìm return orders theo customer ID và trạng thái
    List<ReturnOrder> findByCustomerIdAndStatusOrderByCreatedAtDesc(Long customerId, ReturnOrder.ReturnStatus status);

    // Tìm return orders theo khoảng thời gian
    @Query("SELECT ro FROM ReturnOrder ro WHERE ro.createdAt BETWEEN :startDate AND :endDate ORDER BY ro.createdAt DESC")
    List<ReturnOrder> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    // Tìm return orders theo khoảng thời gian với phân trang
    @Query("SELECT ro FROM ReturnOrder ro WHERE ro.createdAt BETWEEN :startDate AND :endDate ORDER BY ro.createdAt DESC")
    Page<ReturnOrder> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           Pageable pageable);

    // Tìm return orders có thể xử lý (REQUESTED)
    @Query("SELECT ro FROM ReturnOrder ro WHERE ro.status = 'REQUESTED' ORDER BY ro.createdAt ASC")
    List<ReturnOrder> findPendingReturns();

    // Thống kê return orders theo trạng thái
    @Query("SELECT ro.status, COUNT(ro) FROM ReturnOrder ro GROUP BY ro.status")
    List<Object[]> countReturnOrdersByStatus();

    // Tìm return orders theo lý do
    @Query("SELECT ro FROM ReturnOrder ro WHERE ro.reason LIKE %:reason% ORDER BY ro.createdAt DESC")
    List<ReturnOrder> findByReasonContaining(@Param("reason") String reason);

    // Kiểm tra xem order đã có return request chưa
    @Query("SELECT COUNT(ro) > 0 FROM ReturnOrder ro WHERE ro.order.id = :orderId AND ro.status IN ('REQUESTED', 'APPROVED')")
    boolean existsActiveReturnByOrderId(@Param("orderId") Long orderId);

    // Kiểm tra return code có tồn tại không
    boolean existsByReturnCode(String returnCode);

    // Đếm số return orders trong ngày
    long countByCreatedAtBetween(LocalDateTime startOfDay, LocalDateTime endOfDay);

    // Tìm return order theo return code
    Optional<ReturnOrder> findByReturnCode(String returnCode);
}

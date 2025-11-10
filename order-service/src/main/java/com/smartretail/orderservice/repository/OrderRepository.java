package com.smartretail.orderservice.repository;

import com.smartretail.orderservice.model.Order;
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
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Tìm đơn hàng theo customer ID
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    // Tìm đơn hàng theo customer ID với phân trang
    Page<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    // Tìm đơn hàng theo trạng thái
    List<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status);

    // Tìm đơn hàng theo trạng thái với phân trang
    Page<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status, Pageable pageable);

	// Tìm theo mã đơn hàng
	Optional<Order> findByOrderCode(String orderCode);

	// Kiểm tra tồn tại theo mã đơn hàng
	boolean existsByOrderCode(String orderCode);

    // Tìm đơn hàng theo customer ID và trạng thái
    List<Order> findByCustomerIdAndStatusOrderByCreatedAtDesc(Long customerId, Order.OrderStatus status);

    // Tìm đơn hàng theo customer ID và trạng thái với phân trang
    Page<Order> findByCustomerIdAndStatusOrderByCreatedAtDesc(Long customerId, Order.OrderStatus status, Pageable pageable);

    // Tìm đơn hàng theo khoảng thời gian
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    // Tìm đơn hàng theo khoảng thời gian với phân trang
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    Page<Order> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate,
                                     Pageable pageable);

	// Đếm số đơn trong một khoảng thời gian (phục vụ sinh số thứ tự trong ngày)
	long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Tìm đơn hàng có thể hủy (PENDING hoặc CONFIRMED)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId AND o.status IN ('PENDING', 'CONFIRMED')")
    Optional<Order> findCancellableOrder(@Param("orderId") Long orderId);

    // Thống kê đơn hàng theo trạng thái
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();

    // Tìm đơn hàng có promotion được áp dụng
    List<Order> findByPromotionAppliedIdIsNotNull();

    // Tìm đơn hàng theo tổng tiền
    @Query("SELECT o FROM Order o WHERE o.totalAmount >= :minAmount AND o.totalAmount <= :maxAmount ORDER BY o.createdAt DESC")
    List<Order> findByTotalAmountBetween(@Param("minAmount") java.math.BigDecimal minAmount,
                                       @Param("maxAmount") java.math.BigDecimal maxAmount);

    // Analytics queries
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate AND o.status = :status")
    List<Order> findByCreatedAtBetweenAndStatus(@Param("startDate") java.time.LocalDateTime startDate,
                                               @Param("endDate") java.time.LocalDateTime endDate,
                                               @Param("status") com.smartretail.orderservice.model.Order.OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    Page<Order> findByCreatedAtBetweenWithPagination(@Param("startDate") java.time.LocalDateTime startDate,
                                                    @Param("endDate") java.time.LocalDateTime endDate,
                                                    Pageable pageable);

	// Revenue grouped by DAY
	@Query(value = "SELECT DATE(o.created_at) AS period, SUM(o.total_amount) AS revenue " +
			"FROM orders o " +
			"WHERE o.created_at BETWEEN :startDate AND :endDate AND o.status = 'COMPLETED' " +
			"GROUP BY DATE(o.created_at) ORDER BY period", nativeQuery = true)
	List<Object[]> sumRevenueByDayBetween(@Param("startDate") java.time.LocalDateTime startDate,
	                                    @Param("endDate") java.time.LocalDateTime endDate);

	// Revenue grouped by WEEK (ISO week, mode 1)
	@Query(value = "SELECT DATE(DATE_SUB(o.created_at, INTERVAL WEEKDAY(o.created_at) DAY)) AS period, SUM(o.total_amount) AS revenue " +
			"FROM orders o " +
			"WHERE o.created_at BETWEEN :startDate AND :endDate AND o.status = 'COMPLETED' " +
			"GROUP BY DATE(DATE_SUB(o.created_at, INTERVAL WEEKDAY(o.created_at) DAY)) ORDER BY period", nativeQuery = true)
	List<Object[]> sumRevenueByWeekBetween(@Param("startDate") java.time.LocalDateTime startDate,
	                                     @Param("endDate") java.time.LocalDateTime endDate);

	// Revenue grouped by MONTH
	@Query(value = "SELECT DATE_FORMAT(o.created_at, '%Y-%m-01') AS period, SUM(o.total_amount) AS revenue " +
			"FROM orders o " +
			"WHERE o.created_at BETWEEN :startDate AND :endDate AND o.status = 'COMPLETED' " +
			"GROUP BY DATE_FORMAT(o.created_at, '%Y-%m') ORDER BY period", nativeQuery = true)
	List<Object[]> sumRevenueByMonthBetween(@Param("startDate") java.time.LocalDateTime startDate,
	                                      @Param("endDate") java.time.LocalDateTime endDate);

	// Revenue grouped by YEAR
	@Query(value = "SELECT CAST(YEAR(o.created_at) AS CHAR) AS period, SUM(o.total_amount) AS revenue " +
			"FROM orders o " +
			"WHERE o.created_at BETWEEN :startDate AND :endDate AND o.status = 'COMPLETED' " +
			"GROUP BY YEAR(o.created_at) ORDER BY period", nativeQuery = true)
	List<Object[]> sumRevenueByYearBetween(@Param("startDate") java.time.LocalDateTime startDate,
	                                     @Param("endDate") java.time.LocalDateTime endDate);

	// Order count grouped by DAY
	@Query(value = "SELECT DATE(o.created_at) AS period, COUNT(*) AS cnt " +
			"FROM orders o " +
			"WHERE o.created_at BETWEEN :startDate AND :endDate " +
			"GROUP BY DATE(o.created_at) ORDER BY period", nativeQuery = true)
	List<Object[]> countOrdersByDayBetween(@Param("startDate") java.time.LocalDateTime startDate,
	                                     @Param("endDate") java.time.LocalDateTime endDate);

	// Order count grouped by WEEK (ISO week)
	@Query(value = "SELECT DATE(DATE_SUB(o.created_at, INTERVAL WEEKDAY(o.created_at) DAY)) AS period, COUNT(*) AS cnt " +
			"FROM orders o " +
			"WHERE o.created_at BETWEEN :startDate AND :endDate " +
			"GROUP BY DATE(DATE_SUB(o.created_at, INTERVAL WEEKDAY(o.created_at) DAY)) ORDER BY period", nativeQuery = true)
	List<Object[]> countOrdersByWeekBetween(@Param("startDate") java.time.LocalDateTime startDate,
	                                      @Param("endDate") java.time.LocalDateTime endDate);

	// Order count grouped by MONTH
	@Query(value = "SELECT DATE_FORMAT(o.created_at, '%Y-%m-01') AS period, COUNT(*) AS cnt " +
			"FROM orders o " +
			"WHERE o.created_at BETWEEN :startDate AND :endDate " +
			"GROUP BY DATE_FORMAT(o.created_at, '%Y-%m') ORDER BY period", nativeQuery = true)
	List<Object[]> countOrdersByMonthBetween(@Param("startDate") java.time.LocalDateTime startDate,
	                                       @Param("endDate") java.time.LocalDateTime endDate);

	// Order count grouped by YEAR
	@Query(value = "SELECT CAST(YEAR(o.created_at) AS CHAR) AS period, COUNT(*) AS cnt " +
			"FROM orders o " +
			"WHERE o.created_at BETWEEN :startDate AND :endDate " +
			"GROUP BY YEAR(o.created_at) ORDER BY period", nativeQuery = true)
	List<Object[]> countOrdersByYearBetween(@Param("startDate") java.time.LocalDateTime startDate,
	                                      @Param("endDate") java.time.LocalDateTime endDate);
}

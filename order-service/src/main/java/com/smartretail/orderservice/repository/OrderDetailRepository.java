package com.smartretail.orderservice.repository;

import com.smartretail.orderservice.model.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {

    // Tìm order details theo order ID
    List<OrderDetail> findByOrderIdOrderById(Long orderId);

    // Tìm order detail theo order ID và product unit ID
    @Query("SELECT od FROM OrderDetail od WHERE od.orderId = :orderId AND od.productUnitId = :productUnitId")
    Optional<OrderDetail> findByOrderIdAndProductUnitId(@Param("orderId") Long orderId,
                                                        @Param("productUnitId") Long productUnitId);

    // Xóa tất cả order details của một order
    void deleteByOrderId(Long orderId);

    // Đếm số lượng sản phẩm trong đơn hàng
    @Query("SELECT COUNT(od) FROM OrderDetail od WHERE od.orderId = :orderId")
    Long countByOrderId(@Param("orderId") Long orderId);

    // Tính tổng số lượng sản phẩm trong đơn hàng
    @Query("SELECT SUM(od.quantity) FROM OrderDetail od WHERE od.orderId = :orderId")
    Integer sumQuantityByOrderId(@Param("orderId") Long orderId);

    // Tìm order details theo product unit ID
    List<OrderDetail> findByProductUnitIdOrderByOrderIdDesc(Long productUnitId);

    // Tìm order details có giá trị cao nhất
    @Query("SELECT od FROM OrderDetail od ORDER BY od.subtotal DESC")
    List<OrderDetail> findTopByOrderBySubtotalDesc();

    // Analytics query - Lấy doanh thu theo sản phẩm
    @Query("SELECT od.productUnitId, SUM(od.subtotal), SUM(od.quantity), COUNT(DISTINCT od.orderId) " +
           "FROM OrderDetail od " +
           "JOIN od.order o " +
           "WHERE o.createdAt BETWEEN :startDate AND :endDate " +
           "AND o.status = 'COMPLETED' " +
           "GROUP BY od.productUnitId " +
           "ORDER BY SUM(od.subtotal) DESC")
    List<Object[]> findProductRevenueByDateRange(@Param("startDate") java.time.LocalDateTime startDate,
                                                 @Param("endDate") java.time.LocalDateTime endDate);

    // Analytics query - Sản phẩm bán chạy nhất (theo số lượng)
    @Query("SELECT od.productUnitId, SUM(od.quantity) as totalQuantity, SUM(od.subtotal) as totalRevenue, COUNT(DISTINCT od.orderId) as orderCount " +
           "FROM OrderDetail od " +
           "JOIN od.order o " +
           "WHERE o.createdAt BETWEEN :startDate AND :endDate " +
           "AND o.status = 'COMPLETED' " +
           "GROUP BY od.productUnitId " +
           "ORDER BY totalQuantity DESC")
    List<Object[]> findBestSellingProductsByQuantity(@Param("startDate") java.time.LocalDateTime startDate,
                                                     @Param("endDate") java.time.LocalDateTime endDate);

    // Analytics query - Sản phẩm bán chạy nhất (theo doanh thu)
    @Query("SELECT od.productUnitId, SUM(od.quantity) as totalQuantity, SUM(od.subtotal) as totalRevenue, COUNT(DISTINCT od.orderId) as orderCount " +
           "FROM OrderDetail od " +
           "JOIN od.order o " +
           "WHERE o.createdAt BETWEEN :startDate AND :endDate " +
           "AND o.status = 'COMPLETED' " +
           "GROUP BY od.productUnitId " +
           "ORDER BY totalRevenue DESC")
    List<Object[]> findBestSellingProductsByRevenue(@Param("startDate") java.time.LocalDateTime startDate,
                                                   @Param("endDate") java.time.LocalDateTime endDate);

    // Analytics query - Sản phẩm bán ế nhất (theo số lượng)
    @Query("SELECT od.productUnitId, SUM(od.quantity) as totalQuantity, SUM(od.subtotal) as totalRevenue, COUNT(DISTINCT od.orderId) as orderCount " +
           "FROM OrderDetail od " +
           "JOIN od.order o " +
           "WHERE o.createdAt BETWEEN :startDate AND :endDate " +
           "AND o.status = 'COMPLETED' " +
           "GROUP BY od.productUnitId " +
           "ORDER BY totalQuantity ASC")
    List<Object[]> findWorstSellingProductsByQuantity(@Param("startDate") java.time.LocalDateTime startDate,
                                                       @Param("endDate") java.time.LocalDateTime endDate);

    // Analytics query - Sản phẩm bán ế nhất (theo doanh thu)
    @Query("SELECT od.productUnitId, SUM(od.quantity) as totalQuantity, SUM(od.subtotal) as totalRevenue, COUNT(DISTINCT od.orderId) as orderCount " +
           "FROM OrderDetail od " +
           "JOIN od.order o " +
           "WHERE o.createdAt BETWEEN :startDate AND :endDate " +
           "AND o.status = 'COMPLETED' " +
           "GROUP BY od.productUnitId " +
           "ORDER BY totalRevenue ASC")
    List<Object[]> findWorstSellingProductsByRevenue(@Param("startDate") java.time.LocalDateTime startDate,
                                                     @Param("endDate") java.time.LocalDateTime endDate);
}

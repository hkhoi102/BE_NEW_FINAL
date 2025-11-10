package com.smartretail.orderservice.controller;

import com.smartretail.orderservice.model.Order;
import com.smartretail.orderservice.repository.OrderRepository;
import com.smartretail.orderservice.repository.OrderDetailRepository;
import com.smartretail.orderservice.service.CustomerInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private CustomerInfoService customerInfoService;

    // GET /api/orders/analytics/revenue/series - Doanh thu theo ngày/tuần/tháng/năm
    @GetMapping("/revenue/series")
    public ResponseEntity<?> getRevenueSeries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "day") String groupBy,
            @RequestHeader("Authorization") String authHeader) {

        try {
            customerInfoService.getCustomerIdFromToken(authHeader);

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            List<Object[]> rows;
            switch (groupBy.toLowerCase()) {
                case "week":
                    rows = orderRepository.sumRevenueByWeekBetween(startDateTime, endDateTime);
                    break;
                case "month":
                    rows = orderRepository.sumRevenueByMonthBetween(startDateTime, endDateTime);
                    break;
                case "year":
                    rows = orderRepository.sumRevenueByYearBetween(startDateTime, endDateTime);
                    break;
                case "day":
                default:
                    rows = orderRepository.sumRevenueByDayBetween(startDateTime, endDateTime);
                    break;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("groupBy", groupBy);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("data", rows);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy chuỗi doanh thu: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/orders/analytics/revenue - Lấy doanh thu theo khoảng thời gian
    @GetMapping("/revenue")
    public ResponseEntity<?> getRevenueAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String groupBy,
            @RequestHeader("Authorization") String authHeader) {

        try {
            // Validate JWT token
            customerInfoService.getCustomerIdFromToken(authHeader);

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            // Lấy tất cả đơn hàng trong khoảng thời gian (có thể filter theo customerId nếu cần)
            List<Order> orders = orderRepository.findByCreatedAtBetweenAndStatus(
                startDateTime, endDateTime, Order.OrderStatus.COMPLETED);

            // Tính tổng doanh thu
            BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Tính số đơn hàng
            Integer totalOrders = orders.size();

            // Tính số lượng sản phẩm
            Integer totalQuantity = orders.stream()
                .mapToInt(order -> order.getOrderDetails() != null ?
                    order.getOrderDetails().stream()
                        .mapToInt(detail -> detail.getQuantity())
                        .sum() : 0)
                .sum();

            // Tính giá trị đơn hàng trung bình
            BigDecimal averageOrderValue = totalOrders > 0 ?
                totalRevenue.divide(BigDecimal.valueOf(totalOrders)) :
                BigDecimal.ZERO;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "totalRevenue", totalRevenue,
                "totalOrders", totalOrders,
                "totalQuantity", totalQuantity,
                "averageOrderValue", averageOrderValue,
                "startDate", startDate,
                "endDate", endDate,
                "groupBy", groupBy
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy dữ liệu doanh thu: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/orders/analytics/status - Lấy thống kê theo trạng thái đơn hàng
    @GetMapping("/status")
    public ResponseEntity<?> getOrderStatusAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestHeader("Authorization") String authHeader) {

        try {
            // Validate JWT token
            customerInfoService.getCustomerIdFromToken(authHeader);

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            // Lấy tất cả đơn hàng trong khoảng thời gian
            List<Order> orders = orderRepository.findByCreatedAtBetweenWithPagination(
                startDateTime, endDateTime, Pageable.unpaged()).getContent();

            // Nhóm theo trạng thái
            Map<Order.OrderStatus, Long> statusCounts = orders.stream()
                .collect(Collectors.groupingBy(
                    Order::getStatus,
                    Collectors.counting()
                ));

            // Tính doanh thu theo trạng thái
            Map<Order.OrderStatus, BigDecimal> statusRevenue = orders.stream()
                .collect(Collectors.groupingBy(
                    Order::getStatus,
                    Collectors.reducing(BigDecimal.ZERO, Order::getTotalAmount, BigDecimal::add)
                ));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "statusCounts", statusCounts,
                "statusRevenue", statusRevenue,
                "startDate", startDate,
                "endDate", endDate
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy dữ liệu trạng thái đơn hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/orders/analytics/products - Lấy doanh thu theo sản phẩm
    @GetMapping("/products")
    public ResponseEntity<?> getProductRevenueAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestHeader("Authorization") String authHeader) {

        try {
            // Validate JWT token
            customerInfoService.getCustomerIdFromToken(authHeader);

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            // Lấy tất cả order details trong khoảng thời gian
            List<Object[]> productRevenue = orderDetailRepository.findProductRevenueByDateRange(
                startDateTime, endDateTime);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productRevenue);
            response.put("startDate", startDate);
            response.put("endDate", endDate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy dữ liệu doanh thu sản phẩm: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/orders/analytics/products/best-selling - Sản phẩm bán chạy nhất
    @GetMapping("/products/best-selling")
    public ResponseEntity<?> getBestSellingProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "quantity") String sortBy,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestHeader("Authorization") String authHeader) {

        try {
            customerInfoService.getCustomerIdFromToken(authHeader);

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            List<Object[]> bestSellingProducts;
            if ("revenue".equalsIgnoreCase(sortBy)) {
                bestSellingProducts = orderDetailRepository.findBestSellingProductsByRevenue(
                    startDateTime, endDateTime);
            } else {
                bestSellingProducts = orderDetailRepository.findBestSellingProductsByQuantity(
                    startDateTime, endDateTime);
            }

            // Giới hạn số lượng kết quả
            if (limit > 0 && bestSellingProducts.size() > limit) {
                bestSellingProducts = bestSellingProducts.subList(0, limit);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", bestSellingProducts);
            response.put("sortBy", sortBy);
            response.put("limit", limit);
            response.put("startDate", startDate);
            response.put("endDate", endDate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy sản phẩm bán chạy nhất: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/orders/analytics/products/worst-selling - Sản phẩm bán ế nhất
    @GetMapping("/products/worst-selling")
    public ResponseEntity<?> getWorstSellingProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "quantity") String sortBy,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestHeader("Authorization") String authHeader) {

        try {
            customerInfoService.getCustomerIdFromToken(authHeader);

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            List<Object[]> worstSellingProducts;
            if ("revenue".equalsIgnoreCase(sortBy)) {
                worstSellingProducts = orderDetailRepository.findWorstSellingProductsByRevenue(
                    startDateTime, endDateTime);
            } else {
                worstSellingProducts = orderDetailRepository.findWorstSellingProductsByQuantity(
                    startDateTime, endDateTime);
            }

            // Giới hạn số lượng kết quả
            if (limit > 0 && worstSellingProducts.size() > limit) {
                worstSellingProducts = worstSellingProducts.subList(0, limit);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", worstSellingProducts);
            response.put("sortBy", sortBy);
            response.put("limit", limit);
            response.put("startDate", startDate);
            response.put("endDate", endDate);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy sản phẩm bán ế nhất: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/orders/analytics/orders/count-series - Số đơn hàng theo thời gian
    @GetMapping("/orders/count-series")
    public ResponseEntity<?> getOrderCountSeries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "day") String groupBy,
            @RequestHeader("Authorization") String authHeader) {

        try {
            customerInfoService.getCustomerIdFromToken(authHeader);

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            List<Object[]> rows;
            switch (groupBy.toLowerCase()) {
                case "week":
                    rows = orderRepository.countOrdersByWeekBetween(startDateTime, endDateTime);
                    break;
                case "month":
                    rows = orderRepository.countOrdersByMonthBetween(startDateTime, endDateTime);
                    break;
                case "year":
                    rows = orderRepository.countOrdersByYearBetween(startDateTime, endDateTime);
                    break;
                case "day":
                default:
                    rows = orderRepository.countOrdersByDayBetween(startDateTime, endDateTime);
                    break;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("groupBy", groupBy);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("data", rows);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy số đơn hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

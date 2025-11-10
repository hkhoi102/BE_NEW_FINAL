package com.smartretail.orderservice.controller;

import com.smartretail.orderservice.dto.OrderDto;
import com.smartretail.orderservice.dto.OrderDetailDto;
import com.smartretail.orderservice.model.Order;
import com.smartretail.orderservice.service.OrderService;
import com.smartretail.orderservice.service.OrderDetailService;
import com.smartretail.orderservice.service.CustomerInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private CustomerInfoService customerInfoService;

    // POST /api/orders - Tạo đơn hàng mới
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderDto.CreateOrderRequest request,
                                       @RequestHeader("Authorization") String authHeader) {
        try {
            OrderDto.OrderResponse createdOrder = orderService.createOrder(request, authHeader);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đơn hàng đã được tạo thành công");
            response.put("data", createdOrder);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi tạo đơn hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // POST /api/orders/preview - Tính tổng tiền và khuyến mãi cho giỏ hàng (không lưu DB)
    @PostMapping("/preview")
    public ResponseEntity<?> previewOrder(@RequestBody OrderDto.PreviewRequest request,
                                        @RequestHeader("Authorization") String authHeader) {
        try {
            OrderDto.PreviewResponse preview = orderService.previewTotals(request, authHeader);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Preview calculated successfully");
            response.put("data", preview);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi tính preview: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/orders/me - Lấy danh sách đơn hàng của user hiện tại (từ JWT token)
    @GetMapping("/me")
    public ResponseEntity<?> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Lấy customerId từ JWT token
            Long customerId = customerInfoService.getCustomerIdFromToken(authHeader);

            Pageable pageable = Pageable.ofSize(size).withPage(page);
            Page<OrderDto.OrderSummary> orders;

            if (status != null) {
                try {
                    Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
                    // Lấy đơn hàng theo customerId và status
                    orders = orderService.getOrdersByCustomerIdAndStatus(customerId, orderStatus, pageable);
                } catch (IllegalArgumentException e) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Trạng thái đơn hàng không hợp lệ");
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                // Lấy tất cả đơn hàng của customer
                orders = orderService.getOrdersByCustomerId(customerId, pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orders.getContent());
            response.put("totalElements", orders.getTotalElements());
            response.put("totalPages", orders.getTotalPages());
            response.put("currentPage", orders.getNumber());
            response.put("size", orders.getSize());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy danh sách đơn hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/orders - Lấy danh sách đơn hàng
    @GetMapping
    public ResponseEntity<?> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status) {
        try {
            Pageable pageable = Pageable.ofSize(size).withPage(page);
            Page<OrderDto.OrderSummary> orders;

            if (customerId != null) {
                orders = orderService.getOrdersByCustomerId(customerId, pageable);
            } else if (status != null) {
                try {
                    Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
                    orders = orderService.getOrdersByStatus(orderStatus, pageable);
                } catch (IllegalArgumentException e) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Trạng thái đơn hàng không hợp lệ");
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                orders = orderService.getAllOrders(pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orders.getContent());
            response.put("totalElements", orders.getTotalElements());
            response.put("totalPages", orders.getTotalPages());
            response.put("currentPage", orders.getNumber());
            response.put("size", orders.getSize());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy danh sách đơn hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/orders/by-code/{orderCode} - Tìm đơn hàng theo mã
    @GetMapping("/by-code/{orderCode}")
    public ResponseEntity<?> getOrderByCode(@PathVariable String orderCode) {
        try {
            Optional<OrderDto.OrderResponse> order = orderService.getOrderByCode(orderCode);
            if (order.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", order.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy đơn hàng với mã: " + orderCode);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi tìm đơn hàng theo mã: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/orders/{id} - Xem chi tiết đơn hàng
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        try {
            Optional<OrderDto.OrderResponse> order = orderService.getOrderById(id);
            if (order.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", order.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy đơn hàng với ID: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy thông tin đơn hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PATCH /api/orders/{id}/status - Cập nhật trạng thái đơn hàng
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody OrderDto.UpdateStatusRequest request,
                                             @RequestHeader("Authorization") String authHeader) {
        try {
            Optional<OrderDto.OrderResponse> updatedOrder = orderService.updateOrderStatus(id, request, authHeader);
            if (updatedOrder.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Trạng thái đơn hàng đã được cập nhật thành công");
                response.put("data", updatedOrder.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy đơn hàng với ID: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật trạng thái đơn hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // DELETE /api/orders/{id} - Hủy đơn hàng
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        try {
            boolean cancelled = orderService.cancelOrder(id);
            if (cancelled) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Đơn hàng đã được hủy thành công");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không thể hủy đơn hàng. Đơn hàng có thể đã được xử lý hoặc không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi hủy đơn hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== ORDER DETAILS ====================

    // GET /api/orders/{id}/details - Lấy danh sách sản phẩm trong đơn hàng
    @GetMapping("/{id}/details")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long id) {
        try {
            List<OrderDetailDto.OrderDetailResponse> orderDetails = orderService.getOrderDetails(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", orderDetails);
            response.put("total", orderDetails.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy danh sách sản phẩm trong đơn hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // POST /api/orders/{id}/details - Thêm sản phẩm vào đơn hàng
    @PostMapping("/{id}/details")
    public ResponseEntity<?> addProductToOrder(@PathVariable Long id, @RequestBody OrderDetailDto.AddProductRequest request,
                                             @RequestHeader("Authorization") String authHeader) {
        try {
            Optional<OrderDetailDto.OrderDetailResponse> orderDetail = orderService.addProductToOrder(id, request, authHeader);
            if (orderDetail.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Sản phẩm đã được thêm vào đơn hàng thành công");
                response.put("data", orderDetail.get());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không thể thêm sản phẩm vào đơn hàng. Đơn hàng có thể đã được xử lý");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi thêm sản phẩm vào đơn hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PATCH /api/orders/{id}/details/{detailId} - Cập nhật số lượng sản phẩm
    @PatchMapping("/{id}/details/{detailId}")
    public ResponseEntity<?> updateOrderDetailQuantity(@PathVariable Long id, @PathVariable Long detailId, @RequestBody OrderDetailDto.UpdateQuantityRequest request,
                                                     @RequestHeader("Authorization") String authHeader) {
        try {
            Optional<OrderDetailDto.OrderDetailResponse> orderDetail = orderService.updateOrderDetailQuantity(id, detailId, request, authHeader);
            if (orderDetail.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Số lượng sản phẩm đã được cập nhật thành công");
                response.put("data", orderDetail.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không thể cập nhật số lượng sản phẩm. Đơn hàng có thể đã được xử lý hoặc sản phẩm không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật số lượng sản phẩm: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // DELETE /api/orders/{id}/details/{detailId} - Xóa sản phẩm khỏi đơn hàng
    @DeleteMapping("/{id}/details/{detailId}")
    public ResponseEntity<?> removeProductFromOrder(@PathVariable Long id, @PathVariable Long detailId,
                                                  @RequestHeader("Authorization") String authHeader) {
        try {
            boolean removed = orderService.removeProductFromOrder(id, detailId, authHeader);
            if (removed) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Sản phẩm đã được xóa khỏi đơn hàng thành công");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không thể xóa sản phẩm khỏi đơn hàng. Đơn hàng có thể đã được xử lý hoặc sản phẩm không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi xóa sản phẩm khỏi đơn hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== HEALTH CHECK ====================

    // Endpoint để payment-service gọi cập nhật trạng thái thanh toán
    @PatchMapping("/{id}/payment-status")
    public ResponseEntity<?> updatePaymentStatus(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            String paymentStatus = (String) request.get("paymentStatus");
            if ("PAID".equals(paymentStatus)) {
                // Kiểm tra đơn hàng có tồn tại không
                Optional<OrderDto.OrderResponse> orderOpt = orderService.getOrderById(id);
                if (orderOpt.isPresent()) {
                    // Cập nhật paymentStatus trong OrderService
                    boolean updated = orderService.updatePaymentStatus(id, Order.PaymentStatus.PAID);

                    if (updated) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "Payment status updated successfully");
                        return ResponseEntity.ok(response);
                    } else {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Failed to update payment status");
                        return ResponseEntity.internalServerError().body(response);
                    }
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Order not found");
                    return ResponseEntity.notFound().build();
                }
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid payment status");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating payment status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Endpoint để FE kiểm tra trạng thái thanh toán
    @GetMapping("/{id}/payment-status")
    public ResponseEntity<?> getPaymentStatus(@PathVariable Long id) {
        try {
            Optional<OrderDto.OrderResponse> orderOpt = orderService.getOrderById(id);
            if (orderOpt.isPresent()) {
                OrderDto.OrderResponse order = orderOpt.get();
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("orderId", order.getId());
                response.put("paymentMethod", order.getPaymentMethod());
                response.put("paymentStatus", order.getPaymentStatus());
                response.put("status", order.getStatus());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Order not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error getting payment status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Service is running!");
    }
}

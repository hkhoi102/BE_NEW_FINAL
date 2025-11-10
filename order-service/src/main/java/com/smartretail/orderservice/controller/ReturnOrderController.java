package com.smartretail.orderservice.controller;

import com.smartretail.orderservice.dto.ReturnOrderDto;
import com.smartretail.orderservice.dto.ReturnDetailDto;
import com.smartretail.orderservice.model.ReturnOrder;
import com.smartretail.orderservice.service.ReturnOrderService;
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
@RequestMapping("/api/returns")
@CrossOrigin(origins = "*")
public class ReturnOrderController {

    @Autowired
    private ReturnOrderService returnOrderService;

    // POST /api/returns - Khách yêu cầu trả hàng
    @PostMapping
    public ResponseEntity<?> createReturnOrder(@RequestBody ReturnOrderDto.CreateReturnRequest request,
                                             @RequestHeader("Authorization") String authHeader) {
        try {
            ReturnOrderDto.ReturnOrderResponse createdReturn = returnOrderService.createReturnOrder(request, authHeader);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Yêu cầu trả hàng đã được tạo thành công");
            response.put("data", createdReturn);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi tạo yêu cầu trả hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/returns - Danh sách yêu cầu trả hàng
    @GetMapping
    public ResponseEntity<?> getAllReturnOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status) {
        try {
            Pageable pageable = Pageable.ofSize(size).withPage(page);
            Page<ReturnOrderDto.ReturnOrderSummary> returnOrders;

            if (customerId != null) {
                returnOrders = returnOrderService.getReturnOrdersByCustomerId(customerId, pageable);
            } else if (status != null) {
                try {
                    ReturnOrder.ReturnStatus returnStatus = ReturnOrder.ReturnStatus.valueOf(status.toUpperCase());
                    returnOrders = returnOrderService.getReturnOrdersByStatus(returnStatus, pageable);
                } catch (IllegalArgumentException e) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Trạng thái yêu cầu trả hàng không hợp lệ");
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                returnOrders = returnOrderService.getAllReturnOrders(pageable);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", returnOrders.getContent());
            response.put("totalElements", returnOrders.getTotalElements());
            response.put("totalPages", returnOrders.getTotalPages());
            response.put("currentPage", returnOrders.getNumber());
            response.put("size", returnOrders.getSize());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy danh sách yêu cầu trả hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/returns/{id} - Xem chi tiết yêu cầu trả hàng theo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getReturnOrderById(@PathVariable Long id) {
        try {
            Optional<ReturnOrderDto.ReturnOrderResponse> returnOrder = returnOrderService.getReturnOrderById(id);
            if (returnOrder.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", returnOrder.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy yêu cầu trả hàng với ID: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy thông tin yêu cầu trả hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/returns/code/{returnCode} - Xem chi tiết yêu cầu trả hàng theo mã đơn trả hàng
    @GetMapping("/code/{returnCode}")
    public ResponseEntity<?> getReturnOrderByCode(@PathVariable String returnCode) {
        try {
            Optional<ReturnOrderDto.ReturnOrderResponse> returnOrder = returnOrderService.getReturnOrderByCode(returnCode);
            if (returnOrder.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", returnOrder.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy yêu cầu trả hàng với mã: " + returnCode);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy thông tin yêu cầu trả hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PATCH /api/returns/{id}/status - Cập nhật trạng thái yêu cầu trả hàng
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateReturnOrderStatus(@PathVariable Long id, @RequestBody ReturnOrderDto.UpdateStatusRequest request) {
        try {
            Optional<ReturnOrderDto.ReturnOrderResponse> updatedReturn = returnOrderService.updateReturnOrderStatus(id, request);
            if (updatedReturn.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Trạng thái yêu cầu trả hàng đã được cập nhật thành công");
                response.put("data", updatedReturn.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy yêu cầu trả hàng với ID: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật trạng thái yêu cầu trả hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PUT /api/returns/{id}/approve - Duyệt yêu cầu trả hàng
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveReturnOrder(@PathVariable Long id) {
        try {
            boolean approved = returnOrderService.approveReturnOrder(id);
            if (approved) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Yêu cầu trả hàng đã được duyệt thành công");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không thể duyệt yêu cầu trả hàng. Yêu cầu có thể đã được xử lý hoặc không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi duyệt yêu cầu trả hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PUT /api/returns/{id}/reject - Từ chối yêu cầu trả hàng
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectReturnOrder(@PathVariable Long id) {
        try {
            boolean rejected = returnOrderService.rejectReturnOrder(id);
            if (rejected) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Yêu cầu trả hàng đã được từ chối");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không thể từ chối yêu cầu trả hàng. Yêu cầu có thể đã được xử lý hoặc không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi từ chối yêu cầu trả hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // PUT /api/returns/{id}/complete - Hoàn thành trả hàng
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeReturnOrder(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        try {
            boolean completed = returnOrderService.completeReturnOrder(id, authHeader);
            if (completed) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Trả hàng đã được hoàn thành thành công");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không thể hoàn thành trả hàng. Yêu cầu có thể chưa được duyệt hoặc không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi hoàn thành trả hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // GET /api/returns/{id}/details - Lấy danh sách sản phẩm trong yêu cầu trả hàng
    @GetMapping("/{id}/details")
    public ResponseEntity<?> getReturnDetails(@PathVariable Long id) {
        try {
            List<ReturnDetailDto.ReturnDetailResponse> returnDetails = returnOrderService.getReturnDetails(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", returnDetails);
            response.put("total", returnDetails.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi khi lấy danh sách sản phẩm trong yêu cầu trả hàng: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== HEALTH CHECK ====================

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Return Order Service is running!");
    }
}

package com.smartretail.orderservice.service;

import com.smartretail.orderservice.client.InventoryServiceClient;
import com.smartretail.orderservice.dto.ReturnOrderDto;
import com.smartretail.orderservice.dto.ReturnDetailDto;
import com.smartretail.orderservice.model.OrderDetail;
import com.smartretail.orderservice.model.ReturnOrder;
import com.smartretail.orderservice.model.ReturnDetail;
import com.smartretail.orderservice.model.Order;
import com.smartretail.orderservice.repository.ReturnOrderRepository;
import com.smartretail.orderservice.repository.ReturnDetailRepository;
import com.smartretail.orderservice.repository.OrderRepository;
import com.smartretail.orderservice.repository.OrderDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReturnOrderService {

    @Autowired
    private ReturnOrderRepository returnOrderRepository;

    @Autowired
    private ReturnDetailRepository returnDetailRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private InventoryServiceClient inventoryServiceClient;

    @Autowired
    private CustomerInfoService customerInfoService;

    @Autowired
    private OrderService orderService;

    // Tạo yêu cầu trả hàng với token
    public ReturnOrderDto.ReturnOrderResponse createReturnOrder(ReturnOrderDto.CreateReturnRequest request, String authHeader) {
        // Lấy customer ID từ token
        Long customerId = customerInfoService.getCustomerIdFromToken(authHeader);

        // Kiểm tra đơn hàng có tồn tại và đã hoàn thành không
        Optional<Order> orderOpt = orderRepository.findById(request.getOrderId());
        if (!orderOpt.isPresent() || orderOpt.get().getStatus() != Order.OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Đơn hàng không tồn tại hoặc chưa hoàn thành");
        }

        // Kiểm tra đã có yêu cầu trả hàng chưa
        if (returnOrderRepository.existsActiveReturnByOrderId(request.getOrderId())) {
            throw new IllegalArgumentException("Đơn hàng đã có yêu cầu trả hàng đang xử lý");
        }

        // Tạo return order
        Order order = orderOpt.get();
        ReturnOrder returnOrder = new ReturnOrder(order, customerId, request.getReason());
        // Sinh mã đơn trả hàng tự động
        returnOrder.setReturnCode(generateReturnCode());

        ReturnOrder savedReturnOrder = returnOrderRepository.save(returnOrder);

        // Tạo return details
        BigDecimal totalRefundAmount = BigDecimal.ZERO;
        for (ReturnDetailDto.CreateReturnDetailRequest detailRequest : request.getReturnDetails()) {
            // Kiểm tra order detail có tồn tại không
            Optional<com.smartretail.orderservice.model.OrderDetail> orderDetailOpt =
                orderDetailRepository.findById(detailRequest.getOrderDetailId());

            if (orderDetailOpt.isPresent()) {
                com.smartretail.orderservice.model.OrderDetail orderDetail = orderDetailOpt.get();

                // Kiểm tra số lượng trả có hợp lệ không
                if (detailRequest.getQuantity() > orderDetail.getQuantity()) {
                    throw new IllegalArgumentException("Số lượng trả không được vượt quá số lượng đã mua");
                }

                ReturnDetail returnDetail = new ReturnDetail(
                    savedReturnOrder.getId(),
                    detailRequest.getOrderDetailId(),
                    detailRequest.getQuantity(),
                    orderDetail.getUnitPrice()
                );
                returnDetailRepository.save(returnDetail);
                totalRefundAmount = totalRefundAmount.add(returnDetail.getRefundAmount());
            }
        }

        return convertToReturnOrderResponse(savedReturnOrder);
    }

    // Lấy danh sách yêu cầu trả hàng
    public Page<ReturnOrderDto.ReturnOrderSummary> getAllReturnOrders(Pageable pageable) {
        Page<ReturnOrder> returnOrders = returnOrderRepository.findAll(pageable);
        return returnOrders.map(this::convertToReturnOrderSummary);
    }

    // Lấy yêu cầu trả hàng theo ID
    public Optional<ReturnOrderDto.ReturnOrderResponse> getReturnOrderById(Long id) {
        return returnOrderRepository.findById(id)
                .map(this::convertToReturnOrderResponse);
    }

    // Lấy yêu cầu trả hàng theo mã đơn trả hàng
    public Optional<ReturnOrderDto.ReturnOrderResponse> getReturnOrderByCode(String returnCode) {
        if (returnCode == null || returnCode.trim().isEmpty()) return Optional.empty();
        return returnOrderRepository.findByReturnCode(returnCode.trim())
                .map(this::convertToReturnOrderResponse);
    }

    // Lấy yêu cầu trả hàng theo customer ID
    public Page<ReturnOrderDto.ReturnOrderSummary> getReturnOrdersByCustomerId(Long customerId, Pageable pageable) {
        Page<ReturnOrder> returnOrders = returnOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        return returnOrders.map(this::convertToReturnOrderSummary);
    }

    // Lấy yêu cầu trả hàng theo trạng thái
    public Page<ReturnOrderDto.ReturnOrderSummary> getReturnOrdersByStatus(ReturnOrder.ReturnStatus status, Pageable pageable) {
        Page<ReturnOrder> returnOrders = returnOrderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return returnOrders.map(this::convertToReturnOrderSummary);
    }

    // Cập nhật trạng thái yêu cầu trả hàng
    public Optional<ReturnOrderDto.ReturnOrderResponse> updateReturnOrderStatus(Long id, ReturnOrderDto.UpdateStatusRequest request) {
        return returnOrderRepository.findById(id)
                .map(returnOrder -> {
                    returnOrder.setStatus(request.getStatus());
                    returnOrder.setProcessedAt(LocalDateTime.now());
                    ReturnOrder savedReturnOrder = returnOrderRepository.save(returnOrder);
                    return convertToReturnOrderResponse(savedReturnOrder);
                });
    }

    // Duyệt yêu cầu trả hàng
    public boolean approveReturnOrder(Long id) {
        Optional<ReturnOrder> returnOrderOpt = returnOrderRepository.findById(id);
        if (returnOrderOpt.isPresent() && returnOrderOpt.get().getStatus() == ReturnOrder.ReturnStatus.REQUESTED) {
            ReturnOrder returnOrder = returnOrderOpt.get();
            returnOrder.setStatus(ReturnOrder.ReturnStatus.APPROVED);
            returnOrder.setProcessedAt(LocalDateTime.now());
            returnOrderRepository.save(returnOrder);
            return true;
        }
        return false;
    }

    // Từ chối yêu cầu trả hàng
    public boolean rejectReturnOrder(Long id) {
        Optional<ReturnOrder> returnOrderOpt = returnOrderRepository.findById(id);
        if (returnOrderOpt.isPresent() && returnOrderOpt.get().getStatus() == ReturnOrder.ReturnStatus.REQUESTED) {
            ReturnOrder returnOrder = returnOrderOpt.get();
            returnOrder.setStatus(ReturnOrder.ReturnStatus.REJECTED);
            returnOrder.setProcessedAt(LocalDateTime.now());
            returnOrderRepository.save(returnOrder);
            return true;
        }
        return false;
    }

    // Hoàn thành trả hàng
    public boolean completeReturnOrder(Long id, String authHeader) {
        Optional<ReturnOrder> returnOrderOpt = returnOrderRepository.findById(id);
        if (returnOrderOpt.isPresent() && returnOrderOpt.get().getStatus() == ReturnOrder.ReturnStatus.APPROVED) {
            ReturnOrder returnOrder = returnOrderOpt.get();
            // Capture pre-return total (after promotions) on the original order
            Order originalOrder = orderRepository.findById(returnOrder.getOrderId()).orElse(null);
            BigDecimal preTotal = originalOrder != null && originalOrder.getTotalAmount() != null
                    ? originalOrder.getTotalAmount()
                    : BigDecimal.ZERO;

            returnOrder.setStatus(ReturnOrder.ReturnStatus.COMPLETED);
            returnOrder.setProcessedAt(LocalDateTime.now());
            returnOrderRepository.save(returnOrder);

            // Tạo phiếu nhập và auto-approve khi hoàn thành trả hàng
            createAndApproveInboundDocument(returnOrder, authHeader);

            // Giảm số lượng OrderDetail theo ReturnDetail
            adjustOrderDetailsForReturn(returnOrder);

            // Tính lại khuyến mãi cho đơn hàng gốc
            orderService.recalcPromotionsForOrder(returnOrder.getOrderId(), authHeader);

            // Reload order to get post-return total
            Order updatedOrder = orderRepository.findById(returnOrder.getOrderId()).orElse(null);
            BigDecimal postTotal = updatedOrder != null && updatedOrder.getTotalAmount() != null
                    ? updatedOrder.getTotalAmount()
                    : BigDecimal.ZERO;

            // Refund amount is the positive difference between pre and post totals
            BigDecimal diff = preTotal.subtract(postTotal);
            if (diff.compareTo(BigDecimal.ZERO) < 0) {
                diff = BigDecimal.ZERO;
            }

            // Persist refund amount; fallback to sum of detail refunds if needed
            try {
                BigDecimal sumDetails = returnDetailRepository.sumRefundAmountByReturnOrderId(returnOrder.getId());
                if (sumDetails != null && sumDetails.compareTo(diff) > 0) {
                    // If line-by-line sum is greater (e.g., due to promo rounding), choose the lesser to avoid over-refund
                    // Keep business rule simple: use min(pre-post, sumDetails)
                    diff = diff.min(sumDetails);
                }
            } catch (Exception ignore) {}

            returnOrder.setRefundAmount(diff);
            returnOrderRepository.save(returnOrder);
            return true;
        }
        return false;
    }

    // Sinh mã đơn trả hàng tự động
    private String generateReturnCode() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime startOfDay = today.atStartOfDay();
        java.time.LocalDateTime endOfDay = today.atTime(23, 59, 59, 999_000_000);
        long countToday = returnOrderRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        long sequence = countToday + 1;

        String datePart = today.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        String code;
        int attempt = 0;
        do {
            String seqPart = String.format("%05d", sequence + attempt);
            code = "RET-" + datePart + "-" + seqPart;
            attempt++;
        } while (returnOrderRepository.existsByReturnCode(code));
        return code;
    }

    private void createAndApproveInboundDocument(ReturnOrder returnOrder, String authHeader) {
        try {
            List<ReturnDetail> returnDetails = returnDetailRepository.findByReturnOrderIdOrderById(returnOrder.getId());
            if (returnDetails.isEmpty()) return;

            // Tạm chọn kho/vị trí cố định (có thể cải tiến để pick theo tồn hoặc theo cấu hình)
            Long warehouseId = 1L;
            Long stockLocationId = 1L;

            Map<String, Object> createDocReq = new java.util.HashMap<>();
            createDocReq.put("type", "INBOUND");
            createDocReq.put("warehouseId", warehouseId);
            createDocReq.put("stockLocationId", stockLocationId);
            createDocReq.put("referenceNumber", "RETURN-" + returnOrder.getId());
            createDocReq.put("note", "Nhập kho do trả hàng - Return Order #" + returnOrder.getId());

            Map<String, Object> createdDoc = inventoryServiceClient.createStockDocument(createDocReq, authHeader);
            Object dataObj = createdDoc.get("data");
            if (!(dataObj instanceof Map)) throw new RuntimeException("Invalid response creating return document");
            Long documentId = ((Number) ((Map<?, ?>) dataObj).get("id")).longValue();

            java.util.List<java.util.Map<String, Object>> lines = new java.util.ArrayList<>();
            for (ReturnDetail detail : returnDetails) {
                OrderDetail orderDetail = orderDetailRepository.findById(detail.getOrderDetailId()).orElse(null);
                if (orderDetail != null && detail.getQuantity() != null && detail.getQuantity() > 0) {
                    Map<String, Object> line = new java.util.HashMap<>();
                    line.put("productUnitId", orderDetail.getProductUnitId());
                    line.put("quantity", detail.getQuantity());
                    lines.add(line);
                }
            }
            Map<String, Object> bulkPayload = new java.util.HashMap<>();
            bulkPayload.put("lines", lines);
            inventoryServiceClient.addDocumentLinesBulk(documentId, bulkPayload, authHeader);
            inventoryServiceClient.approveStockDocument(documentId, authHeader);

        } catch (Exception e) {
            System.err.println("Failed to create/approve inbound document for return: " + e.getMessage());
        }
    }

    private void adjustOrderDetailsForReturn(ReturnOrder returnOrder) {
        List<ReturnDetail> returnDetails = returnDetailRepository.findByReturnOrderIdOrderById(returnOrder.getId());
        java.util.Map<Long, Integer> orderDetailIdToReturnQty = new java.util.HashMap<>();
        for (ReturnDetail rd : returnDetails) {
            orderDetailIdToReturnQty.merge(rd.getOrderDetailId(), rd.getQuantity(), Integer::sum);
        }

        for (java.util.Map.Entry<Long, Integer> entry : orderDetailIdToReturnQty.entrySet()) {
            Long orderDetailId = entry.getKey();
            Integer qtyReturned = entry.getValue();
            orderDetailRepository.findById(orderDetailId).ifPresent(od -> {
                int newQty = Math.max(0, od.getQuantity() - qtyReturned);
                od.setQuantity(newQty);
                od.setSubtotal(od.getUnitPrice().multiply(java.math.BigDecimal.valueOf(newQty)));
                // Keep order detail even when quantity becomes 0 to preserve return history
                orderDetailRepository.save(od);
            });
        }
    }

    // Lấy danh sách return details theo return order ID
    public List<ReturnDetailDto.ReturnDetailResponse> getReturnDetails(Long returnOrderId) {
        List<ReturnDetail> returnDetails = returnDetailRepository.findByReturnOrderIdOrderById(returnOrderId);
        return returnDetails.stream()
                .map(this::convertToReturnDetailResponse)
                .collect(Collectors.toList());
    }

    // Kiểm tra có thể duyệt không
    public boolean canApproveReturnOrder(Long id) {
        Optional<ReturnOrder> returnOrderOpt = returnOrderRepository.findById(id);
        return returnOrderOpt.isPresent() && returnOrderOpt.get().getStatus() == ReturnOrder.ReturnStatus.REQUESTED;
    }

    // Kiểm tra có thể từ chối không
    public boolean canRejectReturnOrder(Long id) {
        return canApproveReturnOrder(id);
    }

    // Kiểm tra có thể hoàn thành không
    public boolean canCompleteReturnOrder(Long id) {
        Optional<ReturnOrder> returnOrderOpt = returnOrderRepository.findById(id);
        return returnOrderOpt.isPresent() && returnOrderOpt.get().getStatus() == ReturnOrder.ReturnStatus.APPROVED;
    }

    // Convert ReturnOrder to ReturnOrderResponse
    private ReturnOrderDto.ReturnOrderResponse convertToReturnOrderResponse(ReturnOrder returnOrder) {
        ReturnOrderDto.ReturnOrderResponse response = new ReturnOrderDto.ReturnOrderResponse();
        response.setId(returnOrder.getId());
        response.setOrderId(returnOrder.getOrderId());
        response.setCustomerId(returnOrder.getCustomerId());
        response.setReturnCode(returnOrder.getReturnCode());
        response.setStatus(returnOrder.getStatus());
        response.setReason(returnOrder.getReason());
        response.setCreatedAt(returnOrder.getCreatedAt());
        response.setProcessedAt(returnOrder.getProcessedAt());
        response.setCanApprove(canApproveReturnOrder(returnOrder.getId()));
        response.setCanReject(canRejectReturnOrder(returnOrder.getId()));
        response.setCanComplete(canCompleteReturnOrder(returnOrder.getId()));

        // Lấy return details
        List<ReturnDetailDto.ReturnDetailResponse> returnDetails = getReturnDetails(returnOrder.getId());
        response.setReturnDetails(returnDetails);

        // Tổng tiền hoàn trả: ưu tiên cột refundAmount đã chốt; fallback về sum chi tiết
        BigDecimal totalRefundAmount = returnOrder.getRefundAmount();
        if (totalRefundAmount == null) {
            totalRefundAmount = returnDetailRepository.sumRefundAmountByReturnOrderId(returnOrder.getId());
        }
        response.setTotalRefundAmount(totalRefundAmount != null ? totalRefundAmount : BigDecimal.ZERO);

        // TODO: Lấy thông tin khách hàng từ Customer Service
        response.setCustomerName("Customer Name");
        response.setCustomerPhone("Customer Phone");
        response.setCustomerEmail("Customer Email");

        return response;
    }

    // Convert ReturnOrder to ReturnOrderSummary
    private ReturnOrderDto.ReturnOrderSummary convertToReturnOrderSummary(ReturnOrder returnOrder) {
        ReturnOrderDto.ReturnOrderSummary summary = new ReturnOrderDto.ReturnOrderSummary();
        summary.setId(returnOrder.getId());
        summary.setOrderId(returnOrder.getOrderId());
        summary.setCustomerId(returnOrder.getCustomerId());
        summary.setReturnCode(returnOrder.getReturnCode());
        summary.setStatus(returnOrder.getStatus());
        summary.setReason(returnOrder.getReason());
        summary.setCreatedAt(returnOrder.getCreatedAt());

        // Tổng tiền hoàn trả: ưu tiên refundAmount đã chốt; fallback về sum chi tiết
        BigDecimal totalRefundAmount = returnOrder.getRefundAmount();
        if (totalRefundAmount == null) {
            totalRefundAmount = returnDetailRepository.sumRefundAmountByReturnOrderId(returnOrder.getId());
        }
        summary.setTotalRefundAmount(totalRefundAmount != null ? totalRefundAmount : BigDecimal.ZERO);

        // Đếm số lượng sản phẩm
        Long itemCount = returnDetailRepository.countByReturnOrderId(returnOrder.getId());
        summary.setItemCount(itemCount.intValue());

        // TODO: Lấy tên khách hàng từ Customer Service
        summary.setCustomerName("Customer Name");

        return summary;
    }

    // Convert ReturnDetail to ReturnDetailResponse
    private ReturnDetailDto.ReturnDetailResponse convertToReturnDetailResponse(ReturnDetail returnDetail) {
        ReturnDetailDto.ReturnDetailResponse response = new ReturnDetailDto.ReturnDetailResponse();
        response.setId(returnDetail.getId());
        response.setReturnOrderId(returnDetail.getReturnOrderId());
        response.setOrderDetailId(returnDetail.getOrderDetailId());
        response.setQuantity(returnDetail.getQuantity());
        response.setRefundAmount(returnDetail.getRefundAmount());

        // TODO: Lấy thông tin sản phẩm từ OrderDetail và Product Service
        response.setProductUnitId(1L); // Cần lấy từ OrderDetail
        response.setProductName("Product Name");
        response.setUnitName("Unit Name");
        response.setProductImageUrl("");
        response.setUnitPrice(returnDetail.getRefundAmount().divide(BigDecimal.valueOf(returnDetail.getQuantity())));
        response.setOriginalQuantity(returnDetail.getQuantity());
        response.setMaxReturnQuantity(returnDetail.getQuantity());

        return response;
    }
}


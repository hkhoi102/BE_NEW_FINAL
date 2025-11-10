package com.smartretail.orderservice.service;

import com.smartretail.orderservice.dto.OrderDetailDto;
import com.smartretail.orderservice.model.Order;
import com.smartretail.orderservice.model.OrderDetail;
import com.smartretail.orderservice.repository.OrderRepository;
import com.smartretail.orderservice.repository.OrderDetailRepository;
import com.smartretail.orderservice.client.ProductServiceClient;
import com.smartretail.orderservice.client.InventoryServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderDetailService {

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductServiceClient productServiceClient;

    @Autowired
    private InventoryServiceClient inventoryServiceClient;

    // Lấy danh sách order details theo order ID
    public List<OrderDetailDto.OrderDetailResponse> getOrderDetails(Long orderId) {
        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderIdOrderById(orderId);
        return orderDetails.stream()
                .map(this::convertToOrderDetailResponse)
                .collect(Collectors.toList());
    }

    // Thêm sản phẩm vào đơn hàng
    public Optional<OrderDetailDto.OrderDetailResponse> addProductToOrder(Long orderId, OrderDetailDto.AddProductRequest request, String authHeader) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent() && orderOpt.get().getStatus() == Order.OrderStatus.PENDING) {
            // Lấy giá từ Product Service
            BigDecimal unitPrice = getProductPrice(request.getProductUnitId(), authHeader);

            // Kiểm tra tồn kho - TẠM THỜI DISABLE
            // checkStockAvailability(request.getProductUnitId(), request.getQuantity(), authHeader);

            // Kiểm tra sản phẩm đã có trong đơn hàng chưa
            Optional<OrderDetail> existingDetail = orderDetailRepository.findByOrderIdAndProductUnitId(orderId, request.getProductUnitId());

            if (existingDetail.isPresent()) {
                // Cập nhật số lượng nếu sản phẩm đã có
                OrderDetail detail = existingDetail.get();
                int newQuantity = detail.getQuantity() + request.getQuantity();

                // Kiểm tra tồn kho cho tổng số lượng mới - TẠM THỜI DISABLE
                // checkStockAvailability(request.getProductUnitId(), newQuantity, authHeader);

                detail.setQuantity(newQuantity);
                detail.setSubtotal(detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity())));
                OrderDetail savedDetail = orderDetailRepository.save(detail);

                // Cập nhật total_amount của order
                updateOrderTotalAmount(orderId);

                return Optional.of(convertToOrderDetailResponse(savedDetail));
            } else {
                // Tạo order detail mới
                OrderDetail orderDetail = new OrderDetail(orderId, request.getProductUnitId(), request.getQuantity(), unitPrice);
                OrderDetail savedDetail = orderDetailRepository.save(orderDetail);

                // Cập nhật total_amount của order
                updateOrderTotalAmount(orderId);

                return Optional.of(convertToOrderDetailResponse(savedDetail));
            }
        }
        return Optional.empty();
    }

    // Cập nhật số lượng sản phẩm
    public Optional<OrderDetailDto.OrderDetailResponse> updateQuantity(Long orderId, Long detailId, OrderDetailDto.UpdateQuantityRequest request) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent() && orderOpt.get().getStatus() == Order.OrderStatus.PENDING) {
            Optional<OrderDetail> detailOpt = orderDetailRepository.findById(detailId);
            if (detailOpt.isPresent()) {
                OrderDetail detail = detailOpt.get();
                if (detail.getOrderId().equals(orderId)) {
                    detail.setQuantity(request.getQuantity());
                    detail.setSubtotal(detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity())));
                    OrderDetail savedDetail = orderDetailRepository.save(detail);

                    // Cập nhật total_amount của order
                    updateOrderTotalAmount(orderId);

                    return Optional.of(convertToOrderDetailResponse(savedDetail));
                }
            }
        }
        return Optional.empty();
    }

    // Xóa sản phẩm khỏi đơn hàng
    public boolean removeProductFromOrder(Long orderId, Long detailId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent() && orderOpt.get().getStatus() == Order.OrderStatus.PENDING) {
            Optional<OrderDetail> detailOpt = orderDetailRepository.findById(detailId);
            if (detailOpt.isPresent()) {
                OrderDetail detail = detailOpt.get();
                if (detail.getOrderId().equals(orderId)) {
                    orderDetailRepository.delete(detail);

                    // Cập nhật total_amount của order
                    updateOrderTotalAmount(orderId);

                    return true;
                }
            }
        }
        return false;
    }

    // Lấy order detail theo ID
    public Optional<OrderDetailDto.OrderDetailResponse> getOrderDetailById(Long orderId, Long detailId) {
        Optional<OrderDetail> detailOpt = orderDetailRepository.findById(detailId);
        if (detailOpt.isPresent()) {
            OrderDetail detail = detailOpt.get();
            if (detail.getOrderId().equals(orderId)) {
                return Optional.of(convertToOrderDetailResponse(detail));
            }
        }
        return Optional.empty();
    }

    // Kiểm tra có thể trả hàng không
    public boolean canReturnOrderDetail(Long orderId, Long detailId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            return order.getStatus() == Order.OrderStatus.COMPLETED;
        }
        return false;
    }

    // Lấy số lượng tối đa có thể trả
    public Integer getMaxReturnQuantity(Long orderId, Long detailId) {
        Optional<OrderDetail> detailOpt = orderDetailRepository.findById(detailId);
        if (detailOpt.isPresent()) {
            OrderDetail detail = detailOpt.get();
            if (detail.getOrderId().equals(orderId)) {
                return detail.getQuantity();
            }
        }
        return 0;
    }

    // Convert OrderDetail to OrderDetailResponse
    private OrderDetailDto.OrderDetailResponse convertToOrderDetailResponse(OrderDetail orderDetail) {
        OrderDetailDto.OrderDetailResponse response = new OrderDetailDto.OrderDetailResponse();
        response.setId(orderDetail.getId());
        response.setOrderId(orderDetail.getOrderId());
        response.setProductUnitId(orderDetail.getProductUnitId());
        response.setQuantity(orderDetail.getQuantity());
        response.setUnitPrice(orderDetail.getUnitPrice());
        response.setSubtotal(orderDetail.getSubtotal());
        response.setCanReturn(canReturnOrderDetail(orderDetail.getOrderId(), orderDetail.getId()));
        response.setMaxReturnQuantity(getMaxReturnQuantity(orderDetail.getOrderId(), orderDetail.getId()));

        // TODO: Lấy thông tin sản phẩm từ Product Service
        response.setProductName("Product Name"); // Cần tích hợp với Product Service
        response.setUnitName("Unit Name"); // Cần tích hợp với Product Service
        response.setProductImageUrl(""); // Cần tích hợp với Product Service

        return response;
    }

    // Helper method để lấy giá từ Product Service
    private BigDecimal getProductPrice(Long productUnitId, String authHeader) {
        try {
            // Bước 1: Lấy thông tin product unit để có productId
            Map<String, Object> unitResponse = productServiceClient.getProductUnitById(1L, productUnitId, authHeader);

            if (unitResponse != null && unitResponse.containsKey("data")) {
                Map<String, Object> unitData = (Map<String, Object>) unitResponse.get("data");
                Long productId = ((Number) unitData.get("productId")).longValue();

                // Bước 2: Lấy giá hiện tại
                Map<String, Object> priceResponse = productServiceClient.getCurrentPrice(productId, productUnitId, authHeader);

                if (priceResponse != null && priceResponse.containsKey("data")) {
                    Object priceObj = priceResponse.get("data");
                    if (priceObj != null) {
                        return new BigDecimal(priceObj.toString());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to get product price for productUnitId: " + productUnitId + ", error: " + e.getMessage());
        }

        // Trả về giá mặc định nếu không lấy được giá
        return BigDecimal.valueOf(100000);
    }

    // Helper method để cập nhật total_amount của order
    private void updateOrderTotalAmount(Long orderId) {
        try {
            // Tính tổng tiền từ tất cả order details
            List<OrderDetail> orderDetails = orderDetailRepository.findByOrderIdOrderById(orderId);
            BigDecimal totalAmount = orderDetails.stream()
                    .map(OrderDetail::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Cập nhật total_amount của order
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                order.setTotalAmount(totalAmount);
                orderRepository.save(order);
            }
        } catch (Exception e) {
            System.err.println("Failed to update order total amount for orderId: " + orderId + ", error: " + e.getMessage());
        }
    }

    // Helper method để kiểm tra tồn kho
    private void checkStockAvailability(Long productUnitId, Integer quantity, String authHeader) {
        try {
            Map<String, Object> response = inventoryServiceClient.getStockByProductUnit(productUnitId, authHeader);
            if (response != null && response.containsKey("data")) {
                Map<String, Object> stockData = (Map<String, Object>) response.get("data");
                Integer availableStock = (Integer) stockData.get("quantity");
                if (availableStock == null || availableStock < quantity) {
                    throw new RuntimeException("Insufficient stock for product unit: " + productUnitId +
                        ". Available: " + availableStock + ", Required: " + quantity);
                }
            } else {
                throw new RuntimeException("Stock information not available for product unit: " + productUnitId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to check stock availability: " + e.getMessage());
        }
    }
}

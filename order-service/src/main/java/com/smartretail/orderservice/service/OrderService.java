package com.smartretail.orderservice.service;

import com.smartretail.orderservice.dto.OrderDto;
import com.smartretail.orderservice.dto.OrderDetailDto;
import com.smartretail.orderservice.model.Order;
import com.smartretail.orderservice.model.OrderDetail;
import com.smartretail.orderservice.repository.OrderRepository;
import com.smartretail.orderservice.repository.OrderDetailRepository;
import com.smartretail.orderservice.client.ProductServiceClient;
import com.smartretail.orderservice.client.PromotionServiceClient;
import com.smartretail.orderservice.client.InventoryServiceClient;
import com.smartretail.orderservice.client.PaymentServiceClient;
import com.smartretail.orderservice.client.CustomerServiceClient;
import com.smartretail.orderservice.client.UserServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private ProductServiceClient productServiceClient;

    @Autowired
    private PromotionServiceClient promotionServiceClient;

    @Autowired
    private InventoryServiceClient inventoryServiceClient;

    @Autowired
    private CustomerInfoService customerInfoService;

    @Autowired
    private PaymentServiceClient paymentServiceClient;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private NotificationService notificationService;

    // Tạo đơn hàng mới với token
    public OrderDto.OrderResponse createOrder(OrderDto.CreateOrderRequest request, String authHeader) {
        // Lấy/khởi tạo customer ID từ token (auto-provision nếu cần, dùng phone trong request)
        Long customerId = customerInfoService.ensureCustomer(authHeader, request.getPhoneNumber());

        // Lấy warehouseId và stockLocationId từ user profile
        Long warehouseId = null;
        Long stockLocationId = null;
        try {
            Map<String, Object> userInfo = userServiceClient.getCurrentUser(authHeader);
            if (userInfo != null && userInfo.containsKey("defaultWarehouseId")) {
                Object warehouseIdObj = userInfo.get("defaultWarehouseId");
                if (warehouseIdObj != null) {
                    warehouseId = ((Number) warehouseIdObj).longValue();
                }
            }
            if (userInfo != null && userInfo.containsKey("defaultStockLocationId")) {
                Object stockLocationIdObj = userInfo.get("defaultStockLocationId");
                if (stockLocationIdObj != null) {
                    stockLocationId = ((Number) stockLocationIdObj).longValue();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to get user warehouse/stock location info: " + e.getMessage());
        }

        // Sử dụng giá trị mặc định nếu không lấy được từ user
        if (warehouseId == null) warehouseId = 1L;
        if (stockLocationId == null) stockLocationId = 1L;

        System.out.println("Using warehouseId: " + warehouseId + ", stockLocationId: " + stockLocationId);

        // Tạo phiếu xuất kho TRƯỚC khi tạo đơn hàng
        Long outboundDocumentId = null;
        try {
            outboundDocumentId = createOutboundDocumentBeforeOrder(request, warehouseId, stockLocationId, authHeader);
            System.out.println("Created outbound document with ID: " + outboundDocumentId);
        } catch (Exception e) {
            System.err.println("Failed to create outbound document: " + e.getMessage());
            throw new RuntimeException("Failed to create outbound document: " + e.getMessage());
        }

        // Tạo order
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setStatus(Order.OrderStatus.PENDING);
        // Sinh mã đơn hàng tự động
        order.setOrderCode(generateOrderCode());
        // Payment: default COD if not provided
        Order.PaymentMethod pm = request.getPaymentMethod() != null ? request.getPaymentMethod() : Order.PaymentMethod.COD;
        order.setPaymentMethod(pm);
        order.setPaymentStatus(Order.PaymentStatus.UNPAID);
        order.setPromotionAppliedId(request.getPromotionAppliedId());

        // Set delivery method: default PICKUP_AT_STORE if not provided
        Order.DeliveryMethod dm = request.getDeliveryMethod() != null ? request.getDeliveryMethod() : Order.DeliveryMethod.PICKUP_AT_STORE;
        order.setDeliveryMethod(dm);

        // Set phone number
        order.setPhoneNumber(request.getPhoneNumber());

        // Set warehouse và stock location từ user
        order.setWarehouseId(warehouseId);
        order.setStockLocationId(stockLocationId);
        order.setOutboundDocumentId(outboundDocumentId);

        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Tính tổng tiền và chuẩn bị data tính khuyến mãi
        BigDecimal totalAmount = BigDecimal.ZERO;
        java.util.List<Map<String, Object>> promoProducts = new java.util.ArrayList<>();
        for (OrderDetailDto.CreateOrderDetailRequest detailRequest : request.getOrderDetails()) {
            Map<String, Object> productInfo = getProductInfo(detailRequest.getProductUnitId(), authHeader);

            Object priceObj = productInfo.get("price");
            BigDecimal unitPrice = priceObj == null ? BigDecimal.ZERO : new BigDecimal(priceObj.toString());
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(detailRequest.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            // Lấy thêm productId và categoryId để tính khuyến mãi
            Long productId = ((Number) productInfo.get("productId")).longValue();
            Long categoryId = null;
            try {
                Map<String, Object> product = productServiceClient.getProductById(productId, authHeader);
                if (product != null && product.containsKey("data")) {
                    Map<String, Object> pd = (Map<String, Object>) product.get("data");
                    if (pd.get("categoryId") != null) {
                        categoryId = ((Number) pd.get("categoryId")).longValue();
                    }
                }
            } catch (Exception ignore) {}

            Map<String, Object> promoItem = new java.util.HashMap<>();
            promoItem.put("productId", productId);
            promoItem.put("productUnitId", detailRequest.getProductUnitId());
            promoItem.put("categoryId", categoryId);
            promoItem.put("quantity", detailRequest.getQuantity());
            promoItem.put("unitPrice", unitPrice);
            promoProducts.add(promoItem);
        }

        // Lưu order tạm với tổng trước khuyến mãi
        order.setTotalAmount(totalAmount);

        // Lấy địa chỉ giao hàng từ request (giao diện nhập)
        if (request.getShippingAddress() != null && !request.getShippingAddress().trim().isEmpty()) {
            order.setShippingAddress(request.getShippingAddress().trim());
        } else {
            // Fallback: lấy từ Customer Service nếu giao diện không nhập
            try {
                Map<String, Object> customerResp = customerInfoService.getCustomerProfile(authHeader);
                if (customerResp != null) {
                    Object addr = customerResp.get("address");
                    if (addr == null && customerResp.containsKey("data")) {
                        Object data = customerResp.get("data");
                        if (data instanceof Map) addr = ((Map<?, ?>) data).get("address");
                    }
                    if (addr != null) {
                        order.setShippingAddress(String.valueOf(addr));
                    }
                }
            } catch (Exception e) {
                System.err.println("WARN: cannot fetch customer address: " + e.getMessage());
            }
        }

        Order savedOrder = orderRepository.save(order);

        // Tạo order details
        for (OrderDetailDto.CreateOrderDetailRequest detailRequest : request.getOrderDetails()) {
            Map<String, Object> productInfo = getProductInfo(detailRequest.getProductUnitId(), authHeader);

            // Kiểm tra và lấy giá an toàn
            Object priceObj = productInfo.get("price");
            BigDecimal unitPrice;
            if (priceObj == null) {
                System.out.println("WARNING: Price not found for product unit: " + detailRequest.getProductUnitId() + ", using default price 0");
                unitPrice = BigDecimal.ZERO; // Giá mặc định
            } else {
                unitPrice = new BigDecimal(priceObj.toString());
                System.out.println("DEBUG: Found price = " + unitPrice + " for productUnitId = " + detailRequest.getProductUnitId());
            }

            OrderDetail orderDetail = new OrderDetail(
                savedOrder.getId(),
                detailRequest.getProductUnitId(),
                detailRequest.getQuantity(),
                unitPrice
            );
            orderDetailRepository.save(orderDetail);
        }

        // Sau khi có danh sách chi tiết, tính khuyến mãi theo sản phẩm/đơn và cập nhật tổng
        try {
            recalculateOrderPromotion(savedOrder.getId(), authHeader);
        } catch (Exception e) {
            System.err.println("Promotion recalculation after create failed: " + e.getMessage());
        }

        // Cập nhật reference number của phiếu xuất với order ID thực tế
        try {
            updateOutboundDocumentReference(outboundDocumentId, savedOrder.getId(), authHeader);
        } catch (Exception e) {
            System.err.println("Failed to update outbound document reference: " + e.getMessage());
            // Không throw exception để không làm fail việc tạo đơn hàng
        }

        // Nếu phương thức thanh toán là BANK_TRANSFER, tạo intent qua payment-service
        if (order.getPaymentMethod() == Order.PaymentMethod.BANK_TRANSFER) {
            try {
                Map<String, Object> intentReq = new HashMap<>();
                intentReq.put("orderId", savedOrder.getId());
                intentReq.put("amount", savedOrder.getTotalAmount());
                intentReq.put("description", "Thanh toan don hang #" + savedOrder.getId());
                intentReq.put("bankCode", "ACB");
                Map<String, Object> intentResp = paymentServiceClient.createSepayIntent(intentReq);

                // Đính kèm paymentInfo vào response
                OrderDto.OrderResponse resp = orderRepository.findById(savedOrder.getId()).map(this::convertToOrderResponse).orElseGet(() -> convertToOrderResponse(savedOrder));
                OrderDto.PaymentInfo pi = new OrderDto.PaymentInfo();
                Map<String, Object> data = intentResp;
                Object qr = data.get("qrContent");
                Object acc = data.get("accountNumber");
                Object accName = data.get("accountName");
                Object bank = data.get("bankCode");
                Object content = data.get("transferContent");
                Object ref = data.get("referenceId");
                pi.setQrContent(qr != null ? qr.toString() : null);
                pi.setAccountNumber(acc != null ? acc.toString() : null);
                pi.setAccountName(accName != null ? accName.toString() : null);
                pi.setBankCode(bank != null ? bank.toString() : null);
                pi.setTransferContent(content != null ? content.toString() : null);
                pi.setReferenceId(ref != null ? ref.toString() : null);
                resp.setPaymentInfo(pi);
                return resp;
            } catch (Exception e) {
                System.err.println("Failed to create bank transfer intent: " + e.getMessage());
            }
        }

        // Trả về bản cập nhật mới nhất
        return orderRepository.findById(savedOrder.getId()).map(this::convertToOrderResponse).orElseGet(() -> convertToOrderResponse(savedOrder));
    }

    private String generateOrderCode() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime startOfDay = today.atStartOfDay();
        java.time.LocalDateTime endOfDay = today.atTime(23, 59, 59, 999_000_000);
        long countToday = orderRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        long sequence = countToday + 1;

        String datePart = today.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        String code;
        int attempt = 0;
        do {
            String seqPart = String.format("%05d", sequence + attempt);
            code = "ORD-" + datePart + "-" + seqPart;
            attempt++;
        } while (orderRepository.existsByOrderCode(code));
        return code;
    }

    // Preview totals for a cart without persisting
    public OrderDto.PreviewResponse previewTotals(OrderDto.PreviewRequest request, String authHeader) {
        if (request == null || request.getOrderDetails() == null || request.getOrderDetails().isEmpty()) {
            return new OrderDto.PreviewResponse(java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO, java.util.Collections.emptyList(), java.util.Collections.emptyList());
        }

        java.math.BigDecimal totalOriginal = java.math.BigDecimal.ZERO;
        java.util.List<java.util.Map<String, Object>> promoProducts = new java.util.ArrayList<>();

        for (com.smartretail.orderservice.dto.OrderDetailDto.CreateOrderDetailRequest d : request.getOrderDetails()) {
            java.util.Map<String, Object> productInfo = getProductInfo(d.getProductUnitId(), authHeader);
            java.math.BigDecimal unitPrice = new java.math.BigDecimal(productInfo.get("price").toString());
            totalOriginal = totalOriginal.add(unitPrice.multiply(java.math.BigDecimal.valueOf(d.getQuantity())));

            Long productId = ((Number) productInfo.get("productId")).longValue();
            Long categoryId = null;
            try {
                java.util.Map<String, Object> product = productServiceClient.getProductById(productId, authHeader);
                if (product != null && product.containsKey("data")) {
                    java.util.Map<String, Object> pd = (java.util.Map<String, Object>) product.get("data");
                    if (pd.get("categoryId") != null) {
                        categoryId = ((Number) pd.get("categoryId")).longValue();
                    }
                }
            } catch (Exception ignore) {}

            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("productId", productId);
            item.put("productUnitId", d.getProductUnitId());
            item.put("categoryId", categoryId);
            item.put("quantity", d.getQuantity());
            item.put("unitPrice", unitPrice);
            promoProducts.add(item);
        }

        java.util.Map<String, Object> orderCalcReq = new java.util.HashMap<>();
        orderCalcReq.put("products", promoProducts);
        Long customerId = customerInfoService.tryGetCustomerIdOrNull(authHeader);
        if (customerId != null) {
            orderCalcReq.put("customerId", customerId);
        }

        java.util.Map<String, Object> calcResp = promotionServiceClient.calculateOrderPromotions(orderCalcReq, authHeader);
        java.math.BigDecimal discount = java.math.BigDecimal.ZERO;
        java.util.List<String> applied = new java.util.ArrayList<>();
        java.util.List<OrderDto.GiftItem> giftItems = new java.util.ArrayList<>();

        if (calcResp != null) {
            // Lấy discount amount
            Object td = calcResp.get("totalDiscountAmount");
            if (td != null) {
                try { discount = new java.math.BigDecimal(td.toString()); } catch (Exception ignore) {}
            }

            // Lấy applied promotions
            Object ap = calcResp.get("appliedPromotions");
            if (ap instanceof java.util.List<?>) {
                for (Object o : (java.util.List<?>) ap) applied.add(String.valueOf(o));
            }

            // Lấy gift items
            Object giftItemsObj = calcResp.get("giftItems");
            if (giftItemsObj instanceof java.util.List<?>) {
                for (Object gi : (java.util.List<?>) giftItemsObj) {
                    if (!(gi instanceof java.util.Map)) continue;
                    Map<?, ?> g = (Map<?, ?>) gi;

                    // Lấy thông tin cơ bản từ promotion-service
                    Object puIdObj = g.get("productUnitId");
                    Object qtyObj = g.get("quantity");
                    Object promoNameObj = g.get("promotionName");

                    if (puIdObj == null || qtyObj == null) continue;

                    Long giftProductUnitId = ((Number) puIdObj).longValue();
                    Integer giftQty = ((Number) qtyObj).intValue();
                    String promotionName = promoNameObj != null ? promoNameObj.toString() : "Khuyến mãi";

                    if (giftQty == null || giftQty <= 0) continue;

                    // Lấy thông tin chi tiết sản phẩm từ Product Service
                    try {
                        Map<String, Object> productInfo = getProductInfo(giftProductUnitId, authHeader);
                        String productName = productInfo.get("productName") != null ?
                            productInfo.get("productName").toString() : "Sản phẩm #" + giftProductUnitId;
                        String unitName = productInfo.get("unitName") != null ?
                            productInfo.get("unitName").toString() : "Cái";

                        // Tạo GiftItem object
                        OrderDto.GiftItem giftItem = new OrderDto.GiftItem();
                        giftItem.setProductUnitId(giftProductUnitId);
                        giftItem.setProductName(productName);
                        giftItem.setUnitName(unitName);
                        giftItem.setQuantity(giftQty);
                        giftItem.setUnitPrice(java.math.BigDecimal.ZERO);  // Quà tặng = 0
                        giftItem.setSubtotal(java.math.BigDecimal.ZERO);   // Quà tặng = 0
                        giftItem.setPromotionName(promotionName);

                        giftItems.add(giftItem);
                    } catch (Exception e) {
                        // Fallback: tạo gift item cơ bản nếu không lấy được thông tin
                        OrderDto.GiftItem giftItem = new OrderDto.GiftItem();
                        giftItem.setProductUnitId(giftProductUnitId);
                        giftItem.setProductName("Sản phẩm #" + giftProductUnitId);
                        giftItem.setUnitName("Cái");
                        giftItem.setQuantity(giftQty);
                        giftItem.setUnitPrice(java.math.BigDecimal.ZERO);
                        giftItem.setSubtotal(java.math.BigDecimal.ZERO);
                        giftItem.setPromotionName(promotionName);

                        giftItems.add(giftItem);
                    }
                }
            }
        }

        java.math.BigDecimal finalAmount = totalOriginal.subtract(discount);
        return new OrderDto.PreviewResponse(totalOriginal, discount, finalAmount, applied, giftItems);
    }

    // Lấy danh sách đơn hàng
    public Page<OrderDto.OrderSummary> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(this::convertToOrderSummary);
    }

    // Lấy đơn hàng theo ID
    public Optional<OrderDto.OrderResponse> getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(this::convertToOrderResponse);
    }

    // Lấy đơn hàng theo mã đơn hàng
    public Optional<OrderDto.OrderResponse> getOrderByCode(String orderCode) {
        if (orderCode == null || orderCode.trim().isEmpty()) return Optional.empty();
        return orderRepository.findByOrderCode(orderCode.trim())
                .map(this::convertToOrderResponse);
    }

    // Lấy đơn hàng theo customer ID
    public Page<OrderDto.OrderSummary> getOrdersByCustomerId(Long customerId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        return orders.map(this::convertToOrderSummary);
    }

    // Lấy đơn hàng theo trạng thái
    public Page<OrderDto.OrderSummary> getOrdersByStatus(Order.OrderStatus status, Pageable pageable) {
        Page<Order> orders = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return orders.map(this::convertToOrderSummary);
    }

    // Lấy đơn hàng theo customer ID và trạng thái
    public Page<OrderDto.OrderSummary> getOrdersByCustomerIdAndStatus(Long customerId, Order.OrderStatus status, Pageable pageable) {
        Page<Order> orders = orderRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(customerId, status, pageable);
        return orders.map(this::convertToOrderSummary);
    }

    // Cập nhật trạng thái đơn hàng
    public Optional<OrderDto.OrderResponse> updateOrderStatus(Long id, OrderDto.UpdateStatusRequest request) {
        return orderRepository.findById(id)
                .map(order -> {
                    order.setStatus(request.getStatus());
                    order.setUpdatedAt(LocalDateTime.now());
                    Order savedOrder = orderRepository.save(order);
                    return convertToOrderResponse(savedOrder);
                });
    }

    // Hủy đơn hàng
    public boolean cancelOrder(Long id) {
        Optional<Order> orderOpt = orderRepository.findCancellableOrder(id);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(Order.OrderStatus.CANCELLED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            return true;
        }
        return false;
    }

    // Xóa đơn hàng (soft delete)
    public boolean deleteOrder(Long id) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            // Chỉ cho phép xóa đơn hàng đã hủy
            if (order.getStatus() == Order.OrderStatus.CANCELLED) {
                orderRepository.delete(order);
                return true;
            }
        }
        return false;
    }

    // Thêm sản phẩm vào đơn hàng
    public Optional<OrderDetailDto.OrderDetailResponse> addProductToOrder(Long orderId, OrderDetailDto.AddProductRequest request, String authHeader) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent() && orderOpt.get().getStatus() == Order.OrderStatus.PENDING) {
            Optional<OrderDetailDto.OrderDetailResponse> resp = orderDetailService.addProductToOrder(orderId, request, authHeader);
            if (resp.isPresent()) {
                // Recalculate bill-level promotion after adding item
                recalculateOrderPromotion(orderId, authHeader);
            }
            return resp;
        }
        return Optional.empty();
    }

    // Cập nhật số lượng sản phẩm trong đơn hàng
    public Optional<OrderDetailDto.OrderDetailResponse> updateOrderDetailQuantity(Long orderId, Long detailId, OrderDetailDto.UpdateQuantityRequest request, String authHeader) {
        Optional<OrderDetailDto.OrderDetailResponse> resp = orderDetailService.updateQuantity(orderId, detailId, request);
        // Recalculate promotion for the order total
        recalculateOrderPromotion(orderId, authHeader);
        return resp;
    }

    // Xóa sản phẩm khỏi đơn hàng
    public boolean removeProductFromOrder(Long orderId, Long detailId, String authHeader) {
        boolean removed = orderDetailService.removeProductFromOrder(orderId, detailId);
        if (removed) {
            recalculateOrderPromotion(orderId, authHeader);
        }
        return removed;
    }

    // Lấy danh sách sản phẩm trong đơn hàng
    public List<OrderDetailDto.OrderDetailResponse> getOrderDetails(Long orderId) {
        return orderDetailService.getOrderDetails(orderId);
    }

    // Kiểm tra có thể hủy đơn hàng không
    public boolean canCancelOrder(Long id) {
        return orderRepository.findCancellableOrder(id).isPresent();
    }

    // Kiểm tra có thể trả hàng không
    public boolean canReturnOrder(Long id) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            return order.getStatus() == Order.OrderStatus.COMPLETED;
        }
        return false;
    }

    // Convert Order to OrderResponse
    private OrderDto.OrderResponse convertToOrderResponse(Order order) {
        OrderDto.OrderResponse response = new OrderDto.OrderResponse();
        response.setOrderCode(order.getOrderCode());
        response.setId(order.getId());
        response.setCustomerId(order.getCustomerId());
        response.setShippingAddress(order.getShippingAddress());
        response.setDeliveryMethod(order.getDeliveryMethod());
        response.setPhoneNumber(order.getPhoneNumber());
        response.setTotalAmount(order.getTotalAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setStatus(order.getStatus());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPromotionAppliedId(order.getPromotionAppliedId());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setCanCancel(canCancelOrder(order.getId()));
        response.setCanReturn(canReturnOrder(order.getId()));

        // Lấy order details
        List<OrderDetailDto.OrderDetailResponse> orderDetails = getOrderDetails(order.getId());
        response.setOrderDetails(orderDetails);

        return response;
    }

    // Recalculate promotion for an existing order based on current items
    private void recalculateOrderPromotion(Long orderId, String authHeader) {
        try {
            List<OrderDetail> orderDetails = orderDetailRepository.findByOrderIdOrderById(orderId);
            if (orderDetails.isEmpty()) {
                // If no items, set total to 0
                orderRepository.findById(orderId).ifPresent(o -> { o.setTotalAmount(BigDecimal.ZERO); orderRepository.save(o); });
                return;
            }

            // Tính tổng bill từ subtotal của order_details, không phụ thuộc Product Service
            BigDecimal total = BigDecimal.ZERO;
            for (OrderDetail d : orderDetails) {
                BigDecimal lineSubtotal = d.getSubtotal() != null
                        ? d.getSubtotal()
                        : d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity()));
                total = total.add(lineSubtotal);
            }

            // Tính khuyến mãi theo sản phẩm/đơn (bao gồm cả bill-level trong promotion-service)
            java.util.List<Map<String, Object>> promoProducts = new java.util.ArrayList<>();
            for (OrderDetail d : orderDetails) {
                // Lấy productId và categoryId từ Product Service, không gọi API giá
                Map<String, Object> unitResp = productServiceClient.getProductUnitById(1L, d.getProductUnitId(), authHeader);
                Long productId = null;
                Long categoryId = null;
                if (unitResp != null && unitResp.containsKey("data")) {
                    Map<String, Object> unitData = (Map<String, Object>) unitResp.get("data");
                    Object pid = unitData.get("productId");
                    if (pid != null) {
                        productId = ((Number) pid).longValue();
                        try {
                            Map<String, Object> product = productServiceClient.getProductById(productId, authHeader);
                            if (product != null && product.containsKey("data")) {
                                Map<String, Object> pd = (Map<String, Object>) product.get("data");
                                if (pd.get("categoryId") != null) {
                                    categoryId = ((Number) pd.get("categoryId")).longValue();
                                }
                            }
                        } catch (Exception ignore) {}
                    }
                }

                Map<String, Object> item = new java.util.HashMap<>();
                item.put("productId", productId);
                item.put("productUnitId", d.getProductUnitId());
                item.put("categoryId", categoryId);
                item.put("quantity", d.getQuantity());
                item.put("unitPrice", d.getUnitPrice());
                promoProducts.add(item);
            }

            Map<String, Object> orderCalcReq = new java.util.HashMap<>();
            orderCalcReq.put("products", promoProducts);
            // Use the customerId from the order in DB to avoid dependence on caller's JWT (admin vs user)
            Long customerId = orderRepository.findById(orderId).map(Order::getCustomerId).orElse(null);
            if (customerId != null) {
                orderCalcReq.put("customerId", customerId);
            }

            Map<String, Object> orderCalcResp = promotionServiceClient.calculateOrderPromotions(orderCalcReq, authHeader);
            if (orderCalcResp != null) {
                // 1) Đồng bộ quà tặng (giftItems) từ promotion-service vào order_details với đơn giá 0
                Object giftItemsObj = orderCalcResp.get("giftItems");
                if (giftItemsObj instanceof java.util.List<?>) {
                    for (Object gi : (java.util.List<?>) giftItemsObj) {
                        if (!(gi instanceof java.util.Map)) continue;
                        Map<?, ?> g = (Map<?, ?>) gi;
                        Object puIdObj = g.get("productUnitId");
                        Object qtyObj = g.get("quantity");
                        if (puIdObj == null || qtyObj == null) continue;
                        Long giftProductUnitId = ((Number) puIdObj).longValue();
                        Integer giftQty = ((Number) qtyObj).intValue();
                        if (giftQty == null || giftQty <= 0) continue;

                        // Tìm xem dòng sản phẩm đã tồn tại trong order chưa
                        java.util.Optional<OrderDetail> existingOpt = orderDetailRepository.findByOrderIdAndProductUnitId(orderId, giftProductUnitId);
                        if (existingOpt.isPresent()) {
                            OrderDetail existing = existingOpt.get();
                            // Nếu là dòng quà (giá 0) thì cập nhật số lượng theo khuyến nghị
                            if (existing.getUnitPrice() != null && java.math.BigDecimal.ZERO.compareTo(existing.getUnitPrice()) == 0) {
                                existing.setQuantity(giftQty);
                                existing.setSubtotal(java.math.BigDecimal.ZERO);
                                orderDetailRepository.save(existing);
                            }
                            // Nếu không phải dòng quà (khách đã mua B), không tự động thay đổi dòng đó
                        } else {
                            // Thêm dòng quà mới với đơn giá 0
                            OrderDetail gift = new OrderDetail(orderId, giftProductUnitId, giftQty, java.math.BigDecimal.ZERO);
                            // subtotal sẽ là 0 theo constructor
                            orderDetailRepository.save(gift);
                        }
                    }
                }

                // 2) Cập nhật tổng tiền/giảm giá từ kết quả tính khuyến mãi
                Object finalAmountObj = orderCalcResp.get("totalFinalAmount");
                Object discountObj = orderCalcResp.get("totalDiscountAmount");
                final BigDecimal finalAmountLocal = finalAmountObj != null ? new BigDecimal(finalAmountObj.toString()) : total;
                final BigDecimal discountLocal = discountObj != null ? new BigDecimal(discountObj.toString()) : total.subtract(finalAmountLocal);
                orderRepository.findById(orderId).ifPresent(o -> { o.setDiscountAmount(discountLocal); o.setTotalAmount(finalAmountLocal); orderRepository.save(o); });
            } else {
                final BigDecimal finalTotalLocal = total;
                orderRepository.findById(orderId).ifPresent(o -> { o.setDiscountAmount(BigDecimal.ZERO); o.setTotalAmount(finalTotalLocal); orderRepository.save(o); });
            }
        } catch (Exception e) {
            System.err.println("Failed to recalculate promotion for order " + orderId + ": " + e.getMessage());
        }
    }

    // Public wrapper to allow other services to trigger promotion recalculation
    public void recalcPromotionsForOrder(Long orderId, String authHeader) {
        recalculateOrderPromotion(orderId, authHeader);
    }

    // Cập nhật trạng thái thanh toán (được gọi từ payment-service)
    public boolean updatePaymentStatus(Long orderId, Order.PaymentStatus paymentStatus) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setPaymentStatus(paymentStatus);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            return true;
        }
        return false;
    }

    // Convert Order to OrderSummary
    private OrderDto.OrderSummary convertToOrderSummary(Order order) {
        OrderDto.OrderSummary summary = new OrderDto.OrderSummary();
        summary.setOrderCode(order.getOrderCode());
        summary.setId(order.getId());
        summary.setCustomerId(order.getCustomerId());
        summary.setTotalAmount(order.getTotalAmount());
        summary.setStatus(order.getStatus());
        summary.setPaymentMethod(order.getPaymentMethod());
        summary.setPaymentStatus(order.getPaymentStatus());
        summary.setShippingAddress(order.getShippingAddress());
        summary.setCreatedAt(order.getCreatedAt());
        summary.setCanCancel(canCancelOrder(order.getId()));

        // Đếm số lượng sản phẩm
        Long itemCount = orderDetailRepository.countByOrderId(order.getId());
        summary.setItemCount(itemCount.intValue());

        return summary;
    }

    // Helper methods for service integration
    private Map<String, Object> getProductInfo(Long productUnitId, String authHeader) {
        try {
            // Bước 1: Lấy thông tin product unit để có productId
            System.out.println("DEBUG: Getting product unit info for unitId=" + productUnitId);
            Map<String, Object> unitResponse;
            try {
                unitResponse = productServiceClient.getProductUnitPublic(productUnitId, authHeader);
            } catch (Exception ex) {
                unitResponse = productServiceClient.getProductUnitById(1L, productUnitId, authHeader);
            }
            System.out.println("DEBUG: Product Unit response = " + unitResponse);

            if (unitResponse != null) {
                Map<String, Object> unitData;
                if (unitResponse.containsKey("data")) {
                    unitData = (Map<String, Object>) unitResponse.get("data");
                } else {
                    // Public controller returns DTO directly (no wrapper)
                    unitData = unitResponse;
                }
                System.out.println("DEBUG: Product Unit data = " + unitData);

                // Lấy productId từ unitData
                Object productIdObj = unitData.get("productId");
                if (productIdObj == null) {
                    throw new RuntimeException("Missing productId for productUnitId: " + productUnitId);
                }
                Long productId = ((Number) productIdObj).longValue();
                System.out.println("DEBUG: Found productId = " + productId);

                // Bước 2: Lấy giá hiện tại
                System.out.println("DEBUG: Getting current price for productId=" + productId + ", productUnitId=" + productUnitId);
                Map<String, Object> priceResponse = productServiceClient.getCurrentPrice(productId, productUnitId, authHeader);
                System.out.println("DEBUG: Price response = " + priceResponse);

                if (priceResponse != null && priceResponse.containsKey("data")) {
                    // Tạo response data với thông tin đầy đủ
                    Map<String, Object> result = new HashMap<>();
                    result.put("price", priceResponse.get("data"));
                    result.put("productId", productId);
                    result.put("productUnitId", productUnitId);
                    result.put("unitName", unitData.get("unitName"));
                    result.put("conversionRate", unitData.get("conversionRate"));

                    // Thêm thông tin sản phẩm
                    try {
                        Map<String, Object> product = productServiceClient.getProductById(productId, authHeader);
                        if (product != null && product.containsKey("data")) {
                            Map<String, Object> pd = (Map<String, Object>) product.get("data");
                            result.put("productName", pd.get("name"));
                            result.put("categoryId", pd.get("categoryId"));
                        }
                    } catch (Exception ignore) {}

                    System.out.println("DEBUG: Final result = " + result);
                    return result;
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Exception calling Product Service: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get product information: " + e.getMessage());
        }
        throw new RuntimeException("Product unit not found: " + productUnitId);
    }

    private void checkStockAvailability(Long productUnitId, Integer quantity, String authHeader) {
        try {
            Map<String, Object> response = inventoryServiceClient.getStockByProductUnit(productUnitId, authHeader);
            if (response != null && response.containsKey("data")) {
                Map<String, Object> stockData = (Map<String, Object>) response.get("data");
                Integer availableStock = (Integer) stockData.get("quantity");
                if (availableStock == null || availableStock < quantity) {
                    throw new RuntimeException("Insufficient stock for product unit: " + productUnitId);
                }
            } else {
                throw new RuntimeException("Stock information not available for product unit: " + productUnitId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to check stock availability: " + e.getMessage());
        }
    }

    private BigDecimal applyPromotion(BigDecimal totalAmount, Long promotionId,
                                    List<OrderDetailDto.CreateOrderDetailRequest> orderDetails, String authHeader) {
        try {
            Map<String, Object> promotionRequest = Map.of(
                "promotionId", promotionId,
                "totalAmount", totalAmount,
                "orderDetails", orderDetails
            );

            Map<String, Object> response = promotionServiceClient.calculatePromotion(promotionRequest, authHeader);
            if (response != null && response.containsKey("data")) {
                Map<String, Object> promotionData = (Map<String, Object>) response.get("data");
                BigDecimal discountAmount = new BigDecimal(promotionData.get("discountAmount").toString());
                return totalAmount.subtract(discountAmount);
            }
        } catch (Exception e) {
            // Nếu không áp dụng được khuyến mãi, trả về giá gốc
            System.err.println("Failed to apply promotion: " + e.getMessage());
        }
        return totalAmount;
    }

    // Cập nhật trạng thái đơn hàng và xuất kho
    public Optional<OrderDto.OrderResponse> updateOrderStatus(Long id, OrderDto.UpdateStatusRequest request, String authHeader) {
        return orderRepository.findById(id)
                .map(order -> {
                    Order.OrderStatus oldStatus = order.getStatus();
                    order.setStatus(request.getStatus());
                    order.setUpdatedAt(LocalDateTime.now());
                    Order savedOrder = orderRepository.save(order);

                    // Nếu chuyển từ PENDING sang CONFIRMED và delivery_method là PICKUP_AT_STORE, gửi notification
                    if (oldStatus == Order.OrderStatus.PENDING
                        && request.getStatus() == Order.OrderStatus.CONFIRMED
                        && savedOrder.getDeliveryMethod() == Order.DeliveryMethod.PICKUP_AT_STORE) {
                        try {
                            System.out.println("=== SENDING ORDER READY NOTIFICATION FOR ORDER " + savedOrder.getId() + " ===");
                            notificationService.sendOrderReadyForPickupNotification(
                                savedOrder.getId(),
                                savedOrder.getOrderCode(),
                                savedOrder.getCustomerId(),
                                authHeader
                            );
                        } catch (Exception ex) {
                            System.err.println("=== NOTIFICATION FAILED ===");
                            System.err.println("Error: " + ex.getMessage());
                            ex.printStackTrace();
                            // Không throw exception để không ảnh hưởng đến việc cập nhật trạng thái đơn hàng
                        }
                    }

                    // Nếu chuyển từ PENDING/CONFIRMED sang DELIVERING, duyệt phiếu xuất kho đã có
                    if ((oldStatus == Order.OrderStatus.PENDING || oldStatus == Order.OrderStatus.CONFIRMED)
                        && request.getStatus() == Order.OrderStatus.DELIVERING) {
                        try {
                            System.out.println("=== STARTING OUTBOUND DOCUMENT APPROVAL FOR ORDER " + savedOrder.getId() + " ===");

                            // Lấy warehouseId/stockLocationId từ request hoặc mặc định (nếu chưa có)
                            Long warehouseId = request.getWarehouseId() != null ? request.getWarehouseId() : savedOrder.getWarehouseId();
                            Long stockLocationId = request.getStockLocationId() != null ? request.getStockLocationId() : savedOrder.getStockLocationId();

                            // Cập nhật warehouse/stockLocation nếu có trong request
                            if (request.getWarehouseId() != null || request.getStockLocationId() != null) {
                                savedOrder.setWarehouseId(warehouseId);
                                savedOrder.setStockLocationId(stockLocationId);
                                orderRepository.save(savedOrder);
                            }

                            // Duyệt phiếu xuất kho đã có
                            createAndApproveOutboundDocument(savedOrder, authHeader);

                            System.out.println("=== OUTBOUND DOCUMENT APPROVAL COMPLETED ===");
                        } catch (Exception ex) {
                            System.err.println("=== OUTBOUND DOCUMENT APPROVAL FAILED ===");
                            System.err.println("Error: " + ex.getMessage());
                            ex.printStackTrace();
                            throw new RuntimeException("Failed to approve outbound document for order: " + savedOrder.getId() + ", reason: " + ex.getMessage());
                        }
                    }

                    return convertToOrderResponse(savedOrder);
                });
    }

    // Tạo phiếu xuất kho TRƯỚC khi tạo đơn hàng
    private Long createOutboundDocumentBeforeOrder(OrderDto.CreateOrderRequest request, Long warehouseId, Long stockLocationId, String authHeader) {
        Long documentId = null;
        try {
            System.out.println("=== CREATING OUTBOUND DOCUMENT BEFORE ORDER ===");

            // Tạo phiếu xuất kho với reference tạm thời
            Map<String, Object> createDocReq = new HashMap<>();
            createDocReq.put("type", "OUTBOUND");
            createDocReq.put("warehouseId", warehouseId);
            createDocReq.put("stockLocationId", stockLocationId);
            createDocReq.put("referenceNumber", "TEMP-" + System.currentTimeMillis());
            createDocReq.put("note", "Phiếu xuất tạm thời - sẽ cập nhật sau khi tạo đơn hàng");

            Map<String, Object> createdDocResp = inventoryServiceClient.createStockDocument(createDocReq, authHeader);
            Object dataObj = createdDocResp.get("data");
            if (!(dataObj instanceof Map)) throw new RuntimeException("Invalid response creating document");
            documentId = ((Number) ((Map<?, ?>) dataObj).get("id")).longValue();

            System.out.println("Created document with ID: " + documentId);

            // Chuẩn bị lines cho bulk từ request
            java.util.List<Map<String, Object>> lines = new java.util.ArrayList<>();

            for (OrderDetailDto.CreateOrderDetailRequest detailRequest : request.getOrderDetails()) {
                System.out.println("Preparing order detail: ProductUnitId=" + detailRequest.getProductUnitId() +
                    ", Quantity=" + detailRequest.getQuantity());

                Map<String, Object> line = new HashMap<>();
                line.put("productUnitId", detailRequest.getProductUnitId());
                line.put("quantity", detailRequest.getQuantity());
                lines.add(line);
            }

            // Thêm sản phẩm vào phiếu xuất - nếu thất bại sẽ rollback
            Map<String, Object> bulkPayload = new HashMap<>();
            bulkPayload.put("lines", lines);
            Map<String, Object> addLinesResp = inventoryServiceClient.addDocumentLinesBulk(documentId, bulkPayload, authHeader);
            System.out.println("Added lines to document: " + addLinesResp);

            System.out.println("=== OUTBOUND DOCUMENT CREATION COMPLETED (NOT APPROVED) ===");
            return documentId;
        } catch (Exception e) {
            System.err.println("=== OUTBOUND DOCUMENT CREATION FAILED ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();

            // Nếu đã tạo phiếu xuất nhưng thêm sản phẩm thất bại, hủy phiếu xuất
            if (documentId != null) {
                try {
                    System.out.println("Rolling back document ID: " + documentId);
                    inventoryServiceClient.cancelStockDocument(documentId, authHeader);
                    System.out.println("Successfully cancelled document ID: " + documentId);
                } catch (Exception rollbackException) {
                    System.err.println("Failed to rollback document ID: " + documentId + ", Error: " + rollbackException.getMessage());
                }
            }

            System.err.println("=== END ERROR DETAILS ===");
            throw e;
        }
    }

    // Cập nhật reference number của phiếu xuất sau khi tạo đơn hàng thành công
    private void updateOutboundDocumentReference(Long documentId, Long orderId, String authHeader) {
        try {
            System.out.println("=== UPDATING OUTBOUND DOCUMENT REFERENCE ===");
            System.out.println("Document ID: " + documentId + ", Order ID: " + orderId);

            // Gọi API cập nhật reference number (cần implement trong inventory service)
            Map<String, Object> updateReq = new HashMap<>();
            updateReq.put("referenceNumber", "ORDER-" + orderId);
            updateReq.put("note", "Xuất kho cho đơn hàng #" + orderId);

            // Note: Cần thêm endpoint updateStockDocument trong InventoryServiceClient
            // inventoryServiceClient.updateStockDocument(documentId, updateReq, authHeader);

            System.out.println("=== OUTBOUND DOCUMENT REFERENCE UPDATED ===");
        } catch (Exception e) {
            System.err.println("=== OUTBOUND DOCUMENT REFERENCE UPDATE FAILED ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Tạo phiếu xuất kho khi tạo đơn hàng (chưa duyệt) - DEPRECATED
    private void createOutboundDocumentForOrder(Order order, String authHeader) {
        try {
            System.out.println("=== CREATING OUTBOUND DOCUMENT FOR ORDER " + order.getId() + " ===");

            List<OrderDetail> orderDetails = orderDetailRepository.findByOrderIdOrderById(order.getId());
            System.out.println("Found " + orderDetails.size() + " order details");

            if (orderDetails.isEmpty()) {
                System.out.println("No order details found, skipping document creation");
                return;
            }

            // Sử dụng warehouse/stockLocation mặc định hoặc từ request
            Long warehouseIdDoc = order.getWarehouseId() != null ? order.getWarehouseId() : 1L;
            Long stockLocationIdDoc = order.getStockLocationId() != null ? order.getStockLocationId() : 1L;

            System.out.println("Using warehouseId: " + warehouseIdDoc + ", stockLocationId: " + stockLocationIdDoc);

            Map<String, Object> createDocReq = new HashMap<>();
            createDocReq.put("type", "OUTBOUND");
            createDocReq.put("warehouseId", warehouseIdDoc);
            createDocReq.put("stockLocationId", stockLocationIdDoc);
            createDocReq.put("referenceNumber", "ORDER-" + order.getId());
            createDocReq.put("note", "Xuất kho cho đơn hàng #" + order.getId());

            Map<String, Object> createdDocResp = inventoryServiceClient.createStockDocument(createDocReq, authHeader);
            Object dataObj = createdDocResp.get("data");
            if (!(dataObj instanceof Map)) throw new RuntimeException("Invalid response creating document");
            Long documentId = ((Number) ((Map<?, ?>) dataObj).get("id")).longValue();

            System.out.println("Created document with ID: " + documentId);

            // Chuẩn bị lines cho bulk
            java.util.List<Map<String, Object>> lines = new java.util.ArrayList<>();

            for (OrderDetail detail : orderDetails) {
                System.out.println("Preparing order detail: ID=" + detail.getId() +
                    ", ProductUnitId=" + detail.getProductUnitId() +
                    ", Quantity=" + detail.getQuantity());

                Map<String, Object> line = new HashMap<>();
                line.put("productUnitId", detail.getProductUnitId());
                line.put("quantity", detail.getQuantity());
                lines.add(line);
            }

            Map<String, Object> bulkPayload = new HashMap<>();
            bulkPayload.put("lines", lines);
            Map<String, Object> addLinesResp = inventoryServiceClient.addDocumentLinesBulk(documentId, bulkPayload, authHeader);
            System.out.println("Added lines to document: " + addLinesResp);

            // Lưu documentId vào order
            order.setOutboundDocumentId(documentId);
            order.setWarehouseId(warehouseIdDoc);
            order.setStockLocationId(stockLocationIdDoc);
            orderRepository.save(order);

            System.out.println("=== OUTBOUND DOCUMENT CREATION COMPLETED (NOT APPROVED) ===");
        } catch (Exception e) {
            System.err.println("=== OUTBOUND DOCUMENT CREATION FAILED ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END ERROR DETAILS ===");
            // Không throw exception để không làm fail việc tạo đơn hàng
        }
    }

    private void createAndApproveOutboundDocument(Order order, String authHeader) {
        try {
            System.out.println("=== APPROVING EXISTING OUTBOUND DOCUMENT FOR ORDER " + order.getId() + " ===");
            System.out.println("Order ID: " + order.getId());
            System.out.println("Auth Header: " + (authHeader != null ? "Present" : "NULL"));

            // Kiểm tra xem order đã có outboundDocumentId chưa
            Long documentId = order.getOutboundDocumentId();
            if (documentId == null) {
                throw new RuntimeException("No outbound document found for order: " + order.getId() + ". Document should be created when order is created.");
            }

            System.out.println("Found existing document ID: " + documentId);

            // Chỉ duyệt phiếu xuất đã có
            Map<String, Object> approveResp = inventoryServiceClient.approveStockDocument(documentId, authHeader);
            System.out.println("Approved document: " + approveResp);

            System.out.println("=== OUTBOUND DOCUMENT APPROVAL COMPLETED ===");
        } catch (Exception e) {
            System.err.println("=== OUTBOUND DOCUMENT APPROVAL FAILED ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END ERROR DETAILS ===");
            throw e; // Re-throw để updateOrderStatus có thể handle
        }
    }
}

package com.smartretail.promotionservice.service;

import com.smartretail.promotionservice.dto.PromotionHeaderDto;
import com.smartretail.promotionservice.dto.PromotionLineDto;
import com.smartretail.promotionservice.dto.PromotionDetailDto;
import com.smartretail.promotionservice.model.PromotionHeader;
import com.smartretail.promotionservice.model.PromotionLine;
import com.smartretail.promotionservice.model.PromotionDetail;
import com.smartretail.promotionservice.repository.PromotionHeaderRepository;
import com.smartretail.promotionservice.repository.PromotionLineRepository;
import com.smartretail.promotionservice.repository.PromotionDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class PromotionCalculationService {

    @Autowired
    private PromotionHeaderRepository promotionHeaderRepository;

    @Autowired
    private PromotionLineRepository promotionLineRepository;

    @Autowired
    private PromotionDetailRepository promotionDetailRepository;

    @Autowired
    private RestTemplate restTemplate;

    // API Gateway URL và các path
    @Value("${app.api-gateway.url}")
    private String apiGatewayUrl;

    @Value("${app.product-service.path}")
    private String productServicePath;

    @Value("${app.user-service.path}")
    private String userServicePath;

    @Value("${app.inventory-service.path}")
    private String inventoryServicePath;

    /**
     * DTO để tính toán khuyến mãi cho một sản phẩm
     */
    public static class ProductPromotionInfo {
        private Long productId;         // ID sản phẩm (không phải đơn vị)
        private Long productUnitId;     // ID ProductUnit (đơn vị bán) - dùng để match điều kiện detail
        private Long categoryId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private BigDecimal discountAmount;
        private BigDecimal finalPrice;
        private List<String> appliedPromotions;

        public ProductPromotionInfo(Long productId, Long productUnitId, Long categoryId, Integer quantity, BigDecimal unitPrice) {
            this.productId = productId;
            this.productUnitId = productUnitId;
            this.categoryId = categoryId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.subtotal = unitPrice.multiply(new BigDecimal(quantity));
            this.discountAmount = BigDecimal.ZERO;
            this.finalPrice = this.subtotal;
            this.appliedPromotions = new ArrayList<>();
        }

        // Getters and Setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Long getProductUnitId() { return productUnitId; }
        public void setProductUnitId(Long productUnitId) { this.productUnitId = productUnitId; }

        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

        public BigDecimal getFinalPrice() { return finalPrice; }
        public void setFinalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; }

        public List<String> getAppliedPromotions() { return appliedPromotions; }
        public void setAppliedPromotions(List<String> appliedPromotions) { this.appliedPromotions = appliedPromotions; }
    }

    /**
     * DTO để tính toán khuyến mãi cho toàn bộ đơn hàng
     */
    public static class OrderPromotionResult {
        private BigDecimal totalOriginalAmount;
        private BigDecimal totalDiscountAmount;
        private BigDecimal totalFinalAmount;
        private List<ProductPromotionInfo> productPromotions;
        private List<String> appliedPromotions;
        private List<GiftItem> giftItems;

        public OrderPromotionResult() {
            this.totalOriginalAmount = BigDecimal.ZERO;
            this.totalDiscountAmount = BigDecimal.ZERO;
            this.totalFinalAmount = BigDecimal.ZERO;
            this.productPromotions = new ArrayList<>();
            this.appliedPromotions = new ArrayList<>();
            this.giftItems = new ArrayList<>();
        }

        // Getters and Setters
        public BigDecimal getTotalOriginalAmount() { return totalOriginalAmount; }
        public void setTotalOriginalAmount(BigDecimal totalOriginalAmount) { this.totalOriginalAmount = totalOriginalAmount; }

        public BigDecimal getTotalDiscountAmount() { return totalDiscountAmount; }
        public void setTotalDiscountAmount(BigDecimal totalDiscountAmount) { this.totalDiscountAmount = totalDiscountAmount; }

        public BigDecimal getTotalFinalAmount() { return totalFinalAmount; }
        public void setTotalFinalAmount(BigDecimal totalFinalAmount) { this.totalFinalAmount = totalFinalAmount; }

        public List<ProductPromotionInfo> getProductPromotions() { return productPromotions; }
        public void setProductPromotions(List<ProductPromotionInfo> productPromotions) { this.productPromotions = productPromotions; }

        public List<String> getAppliedPromotions() { return appliedPromotions; }
        public void setAppliedPromotions(List<String> appliedPromotions) { this.appliedPromotions = appliedPromotions; }

        public List<GiftItem> getGiftItems() { return giftItems; }
        public void setGiftItems(List<GiftItem> giftItems) { this.giftItems = giftItems; }
    }

    public static class GiftItem {
        private Long productUnitId;
        private Integer quantity;
        private String promotionName;

        public GiftItem(Long productUnitId, Integer quantity, String promotionName) {
            this.productUnitId = productUnitId;
            this.quantity = quantity;
            this.promotionName = promotionName;
        }

        public Long getProductUnitId() { return productUnitId; }
        public Integer getQuantity() { return quantity; }
        public String getPromotionName() { return promotionName; }
    }

    /**
     * DTO trả về giảm giá ở cấp bill
     */
    public static class BillDiscountResult {
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;
        private List<String> appliedPromotions;

        public BillDiscountResult() {
            this.totalAmount = BigDecimal.ZERO;
            this.discountAmount = BigDecimal.ZERO;
            this.finalAmount = BigDecimal.ZERO;
            this.appliedPromotions = new ArrayList<>();
        }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
        public BigDecimal getFinalAmount() { return finalAmount; }
        public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
        public List<String> getAppliedPromotions() { return appliedPromotions; }
        public void setAppliedPromotions(List<String> appliedPromotions) { this.appliedPromotions = appliedPromotions; }
    }

    /**
     * Tính toán khuyến mãi cho một đơn hàng
     */
    public OrderPromotionResult calculateOrderPromotions(List<ProductPromotionInfo> products, Long customerId) {
        OrderPromotionResult result = new OrderPromotionResult();
        result.setProductPromotions(products);

        // Lấy tất cả khuyến mãi đang hiệu lực
        List<PromotionHeader> activePromotions = promotionHeaderRepository.findActivePromotionsByDate(LocalDate.now());

        // Tính toán khuyến mãi cho từng sản phẩm
        for (ProductPromotionInfo product : products) {
            calculateProductPromotions(result, product, activePromotions, customerId);
            result.setTotalOriginalAmount(result.getTotalOriginalAmount().add(product.getSubtotal()));
            result.setTotalDiscountAmount(result.getTotalDiscountAmount().add(product.getDiscountAmount()));
            result.setTotalFinalAmount(result.getTotalFinalAmount().add(product.getFinalPrice()));
        }

        // Tính toán khuyến mãi cho toàn bộ đơn hàng
        calculateOrderLevelPromotions(result, activePromotions, customerId);

        return result;
    }

    /**
     * Tính toán khuyến mãi cho một sản phẩm
     */
    private void calculateProductPromotions(OrderPromotionResult result, ProductPromotionInfo product, List<PromotionHeader> activePromotions, Long customerId) {
        // Duyệt tất cả promotion headers đang active, sau đó duyệt lines của từng header
        for (PromotionHeader promotion : activePromotions) {
            List<PromotionLine> lines = promotionLineRepository.findByPromotionHeaderIdAndActiveTrue(promotion.getId());

            for (PromotionLine line : lines) {
                // Kiểm tra line có hiệu lực theo ngày
                if (!line.isActiveOnDate(LocalDate.now())) {
                    continue;
                }

                // Nếu line target theo customer thì kiểm tra customerId
                if (line.getTargetType() == PromotionLine.TargetType.CUSTOMER &&
                        (customerId == null || !line.getTargetId().equals(customerId))) {
                    continue;
                }

                // Lấy promotion details của line
                List<PromotionDetail> details = promotionDetailRepository.findByPromotionLineIdAndActiveTrue(line.getId());

                for (PromotionDetail detail : details) {
                    // Ưu tiên khớp theo ProductUnit điều kiện nếu có cấu hình trong detail
                    if (detail.getConditionProductUnitId() != null) {
                        if (product.getProductUnitId() == null ||
                                !detail.getConditionProductUnitId().equals(product.getProductUnitId())) {
                            continue; // không khớp đơn vị mua X
                        }
                    } else {
                        // Không có điều kiện ở detail → có thể là giảm giá theo product/category.
                        // Có thể giữ logic cũ theo targetType của line nếu cần (optional).
                        if (line.getTargetType() == PromotionLine.TargetType.PRODUCT && line.getTargetId() != null) {
                            if (!line.getTargetId().equals(product.getProductId())) {
                                continue;
                            }
                        }
                        if (line.getTargetType() == PromotionLine.TargetType.CATEGORY && line.getTargetId() != null) {
                            if (product.getCategoryId() == null || !line.getTargetId().equals(product.getCategoryId())) {
                                continue;
                            }
                        }

                        // Nếu line không có target (áp dụng toàn bộ) và detail có minAmount,
                        // coi đây là khuyến mãi cấp hóa đơn (bill-level) → không áp ở cấp sản phẩm
                        // để tránh việc khi xóa target_id/target_type (để null) thì bị cộng dồn sai.
                        if (line.getTargetType() == null && detail.getMinAmount() != null) {
                            continue;
                        }
                    }

                    applyPromotionToProduct(result, product, line, detail);
                }
            }
        }
    }

    /**
     * Áp dụng khuyến mãi cho sản phẩm
     */
    private void applyPromotionToProduct(OrderPromotionResult result, ProductPromotionInfo product, PromotionLine line, PromotionDetail detail) {
        PromotionHeader header = line.getPromotionHeader();
        BigDecimal discount = BigDecimal.ZERO;

        // Ưu tiên type ở line; header không còn type => nếu line null thì bỏ qua
        PromotionHeader.PromotionType effectiveType = line.getType();
        if (effectiveType == null) {
            return; // không áp dụng nếu line không chỉ định loại
        }

        switch (effectiveType) {
            case DISCOUNT_PERCENT:
                discount = calculatePercentageDiscount(product, detail);
                break;
            case DISCOUNT_AMOUNT:
                discount = calculateFixedAmountDiscount(product, detail);
                break;
            case BUY_X_GET_Y:
                discount = calculateBuyXGetYDiscount(result, product, header.getName(), detail);
                break;
        }

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            product.setDiscountAmount(product.getDiscountAmount().add(discount));
            product.setFinalPrice(product.getSubtotal().subtract(product.getDiscountAmount()));
            product.getAppliedPromotions().add(header.getName());
        }
    }

    /**
     * Tính giảm giá theo phần trăm
     */
    private BigDecimal calculatePercentageDiscount(ProductPromotionInfo product, PromotionDetail detail) {
        if (detail.getDiscountPercent() == null || detail.getDiscountPercent() <= 0) {
            return BigDecimal.ZERO;
        }

        // Kiểm tra điều kiện số lượng
        if (detail.getConditionQuantity() != null && product.getQuantity() < detail.getConditionQuantity()) {
            return BigDecimal.ZERO;
        }

        // Kiểm tra điều kiện tổng tiền
        if (detail.getMinAmount() != null && product.getSubtotal().compareTo(detail.getMinAmount()) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = product.getSubtotal()
            .multiply(new BigDecimal(detail.getDiscountPercent()))
            .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);

        // Áp dụng giới hạn tối đa
        if (detail.getMaxDiscount() != null && discount.compareTo(detail.getMaxDiscount()) > 0) {
            discount = detail.getMaxDiscount();
        }

        return discount;
    }

    /**
     * Tính giảm giá theo số tiền cố định
     */
    private BigDecimal calculateFixedAmountDiscount(ProductPromotionInfo product, PromotionDetail detail) {
        if (detail.getDiscountAmount() == null || detail.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Kiểm tra điều kiện số lượng
        if (detail.getConditionQuantity() != null && product.getQuantity() < detail.getConditionQuantity()) {
            return BigDecimal.ZERO;
        }

        // Kiểm tra điều kiện tổng tiền
        if (detail.getMinAmount() != null && product.getSubtotal().compareTo(detail.getMinAmount()) < 0) {
            return BigDecimal.ZERO;
        }

        return detail.getDiscountAmount();
    }

    /**
     * Tính khuyến mãi mua X tặng Y.
     *
     * YÊU CẦU MỚI:
     * - totalDiscountAmount KHÔNG được tính giá trị phần quà Y.
     * - Khuyến mãi này chỉ thêm quà vào danh sách giftItems, khách vẫn trả đủ tiền cho
     *   phần hàng mua (X). Giá trị quà chỉ hiển thị ở UI, không trừ vào tiền phải trả.
     */
    private BigDecimal calculateBuyXGetYDiscount(OrderPromotionResult result, ProductPromotionInfo product, String promotionName, PromotionDetail detail) {
        if (detail.getConditionQuantity() == null || detail.getConditionQuantity() <= 0) {
            return BigDecimal.ZERO;
        }

        // Nếu mua đủ điều kiện X thì tặng Y (chỉ 1 lần) nhưng KHÔNG trừ tiền quà vào discount.
        if (product.getQuantity() >= detail.getConditionQuantity()) {
            int free = (detail.getFreeQuantity() != null && detail.getFreeQuantity() > 0)
                    ? detail.getFreeQuantity()
                    : 1;

            // Nếu cấu hình có sản phẩm quà tặng (ProductUnit khác), ghi nhận vào giftItems
            if (detail.getGiftProductUnitId() != null) {
                result.getGiftItems().add(new GiftItem(detail.getGiftProductUnitId(), free, promotionName));
            } else {
                // Trường hợp tặng thêm chính sản phẩm đang mua (mua 2 tặng 1 cùng mã),
                // vẫn chỉ thêm quà logic ở UI, không trừ tiền.
                result.getGiftItems().add(new GiftItem(product.getProductUnitId(), free, promotionName));
            }

            // Không cộng gì vào discount -> trả về 0 để totalDiscountAmount không bao gồm giá trị quà.
            return BigDecimal.ZERO;
        }

        return BigDecimal.ZERO;
    }

    // Gọi product-service qua API Gateway để lấy giá hiện tại theo productUnitId
    private BigDecimal fetchCurrentUnitPrice(Long productUnitId) {
        try {
            String url = apiGatewayUrl + productServicePath + "/0/prices/current?productUnitId=" + productUnitId;
            Map response = restTemplate.getForObject(url, Map.class);
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                Object data = response.get("data");
                if (data != null) {
                    return new BigDecimal(data.toString());
                }
            }
        } catch (Exception e) {
            // ignore pricing fetch errors; fallback to using unit price of A
        }
        return null;
    }

    /**
     * Tính toán khuyến mãi cấp đơn hàng (bill). Không xét target, chỉ cần header/line/detail hợp lệ.
     * Hỗ trợ DISCOUNT_AMOUNT và DISCOUNT_PERCENT.
     */
    private void calculateOrderLevelPromotions(OrderPromotionResult result, List<PromotionHeader> activePromotions, Long customerId) {
        BigDecimal bestDiscount = BigDecimal.ZERO;
        String bestPromotionName = null;

        for (PromotionHeader promotion : activePromotions) {
            // Duyệt các lines của header và dùng type ở line
            List<PromotionLine> lines = promotionLineRepository
                    .findByPromotionHeaderIdAndActiveTrue(promotion.getId());

            for (PromotionLine line : lines) {
                if (!line.isActiveOnDate(LocalDate.now())) {
                    continue;
                }

                List<PromotionDetail> details = promotionDetailRepository
                        .findByPromotionLineIdAndActiveTrue(line.getId());

                for (PromotionDetail detail : details) {
                    if (detail.getMinAmount() != null &&
                            result.getTotalOriginalAmount().compareTo(detail.getMinAmount()) < 0) {
                        continue;
                    }

                    // Chỉ áp dụng cho line-type percent/amount ở bill-level
                    if (line.getType() == null) continue;

                    BigDecimal orderDiscount = BigDecimal.ZERO;
                    if (line.getType() == PromotionHeader.PromotionType.DISCOUNT_AMOUNT) {
                        orderDiscount = detail.getDiscountAmount() != null ? detail.getDiscountAmount() : BigDecimal.ZERO;
                    } else if (line.getType() == PromotionHeader.PromotionType.DISCOUNT_PERCENT
                            && detail.getDiscountPercent() != null && detail.getDiscountPercent() > 0) {
                        orderDiscount = result.getTotalOriginalAmount()
                                .multiply(new BigDecimal(detail.getDiscountPercent()))
                                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                    }

                    if (detail.getMaxDiscount() != null &&
                            orderDiscount.compareTo(detail.getMaxDiscount()) > 0) {
                        orderDiscount = detail.getMaxDiscount();
                    }

                    if (orderDiscount.compareTo(bestDiscount) > 0) {
                        bestDiscount = orderDiscount;
                        bestPromotionName = promotion.getName();
                    }
                }
            }
        }

        if (bestDiscount.compareTo(BigDecimal.ZERO) > 0) {
            result.setTotalDiscountAmount(result.getTotalDiscountAmount().add(bestDiscount));
            result.setTotalFinalAmount(result.getTotalFinalAmount().subtract(bestDiscount));
            if (bestPromotionName != null) {
                result.getAppliedPromotions().add(bestPromotionName);
            }
        }
    }

    /**
     * Tính giảm giá theo bill (không cần liệt kê sản phẩm). Chỉ cần tổng tiền.
     */
    public BillDiscountResult calculateBillDiscount(BigDecimal totalAmount) {
        BillDiscountResult result = new BillDiscountResult();
        result.setTotalAmount(totalAmount);
        result.setFinalAmount(totalAmount);

        BigDecimal bestDiscount = BigDecimal.ZERO;
        String bestPromotionName = null;

        List<PromotionHeader> activePromotions = promotionHeaderRepository.findActivePromotionsByDate(LocalDate.now());

        for (PromotionHeader promotion : activePromotions) {
            List<PromotionLine> lines = promotionLineRepository.findByPromotionHeaderIdAndActiveTrue(promotion.getId());
            for (PromotionLine line : lines) {
                if (!line.isActiveOnDate(LocalDate.now())) {
                    continue;
                }

                List<PromotionDetail> details = promotionDetailRepository.findByPromotionLineIdAndActiveTrue(line.getId());
                for (PromotionDetail detail : details) {
                    if (detail.getMinAmount() != null && totalAmount.compareTo(detail.getMinAmount()) < 0) {
                        continue;
                    }

                    if (line.getType() == null) continue;

                    BigDecimal discount = BigDecimal.ZERO;
                    if (line.getType() == PromotionHeader.PromotionType.DISCOUNT_AMOUNT) {
                        discount = detail.getDiscountAmount() != null ? detail.getDiscountAmount() : BigDecimal.ZERO;
                    } else if (line.getType() == PromotionHeader.PromotionType.DISCOUNT_PERCENT
                            && detail.getDiscountPercent() != null && detail.getDiscountPercent() > 0) {
                        discount = totalAmount
                                .multiply(new BigDecimal(detail.getDiscountPercent()))
                                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
                    }

                    if (detail.getMaxDiscount() != null && discount.compareTo(detail.getMaxDiscount()) > 0) {
                        discount = detail.getMaxDiscount();
                    }

                    if (discount.compareTo(bestDiscount) > 0) {
                        bestDiscount = discount;
                        bestPromotionName = promotion.getName();
                    }
                }
            }
        }

        if (bestDiscount.compareTo(BigDecimal.ZERO) > 0) {
            result.setDiscountAmount(bestDiscount);
            result.setFinalAmount(totalAmount.subtract(bestDiscount));
            if (bestPromotionName != null) {
                result.getAppliedPromotions().add(bestPromotionName);
            }
        }

        return result;
    }

    /**
     * Lấy thông tin sản phẩm từ Product Service thông qua API Gateway
     */
    public Map<String, Object> getProductInfo(Long productId) {
        try {
            String url = apiGatewayUrl + productServicePath + "/" + productId;
            System.out.println("🔗 Gọi Product Service qua API Gateway: " + url);
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            // Log error và trả về null nếu không thể kết nối
            System.err.println("❌ Không thể kết nối đến Product Service qua API Gateway: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy thông tin user từ User Service thông qua API Gateway
     */
    public Map<String, Object> getUserInfo(Long userId) {
        try {
            String url = apiGatewayUrl + userServicePath + "/" + userId;
            System.out.println("🔗 Gọi User Service qua API Gateway: " + url);
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            // Log error và trả về null nếu không thể kết nối
            System.err.println("❌ Không thể kết nối đến User Service qua API Gateway: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy thông tin inventory từ Inventory Service thông qua API Gateway
     */
    public Map<String, Object> getInventoryInfo(Long productId) {
        try {
            String url = apiGatewayUrl + inventoryServicePath + "/stock/" + productId;
            System.out.println("🔗 Gọi Inventory Service qua API Gateway: " + url);
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            // Log error và trả về null nếu không thể kết nối
            System.err.println("❌ Không thể kết nối đến Inventory Service qua API Gateway: " + e.getMessage());
            return null;
        }
    }
}

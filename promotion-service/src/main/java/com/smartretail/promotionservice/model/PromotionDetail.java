package com.smartretail.promotionservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "promotion_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_line_id", nullable = false)
    private PromotionLine promotionLine;

    @Column(name = "discount_percent")
    private Float discountPercent;  // % giảm giá (ví dụ 20%)

    @Column(name = "discount_amount", precision = 19, scale = 2)
    private BigDecimal discountAmount;  // Số tiền giảm cố định (ví dụ 50,000đ)

    @Column(name = "condition_quantity")
    private Integer conditionQuantity;  // Mua tối thiểu bao nhiêu sp (ví dụ: mua từ 2 sản phẩm trở lên)

    @Column(name = "free_quantity")
    private Integer freeQuantity;  // Số lượng tặng Y cho mỗi lần đạt điều kiện X (mặc định 1)

    // Sản phẩm điều kiện để xét tặng (mua X -> tặng Y). Lưu ProductUnit ID của X
    @Column(name = "condition_product_unit_id")
    private Long conditionProductUnitId;

    // Nếu là chương trình Mua A tặng B: giftProductUnitId là ID ProductUnit của sản phẩm tặng
    @Column(name = "gift_product_unit_id")
    private Long giftProductUnitId;

    @Column(name = "min_amount", precision = 19, scale = 2)
    private BigDecimal minAmount;  // Tổng tiền tối thiểu của đơn hàng để được giảm (ví dụ >= 500,000đ)

    @Column(name = "max_discount", precision = 19, scale = 2)
    private BigDecimal maxDiscount;  // Mức giảm tối đa (ví dụ không quá 200,000đ)

    @Column(nullable = false)
    private Boolean active = true;
}

package com.smartretail.promotionservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "promotion_lines")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_header_id", nullable = false)
    private PromotionHeader promotionHeader;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = true)
    private TargetType targetType;

    @Column(name = "target_id", nullable = true)
    private Long targetId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private Boolean active = true;

    // Loại khuyến mãi ở cấp line (ưu tiên nếu được set). Nếu null, dùng loại ở header
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private PromotionHeader.PromotionType type;

    public enum TargetType {
        PRODUCT, CATEGORY, CUSTOMER
    }

    /**
     * Kiểm tra xem promotion line có đang hiệu lực trong ngày cụ thể không
     */
    public boolean isActiveOnDate(LocalDate date) {
        if (!active) {
            return false;
        }

        // Nếu không có startDate và endDate, sử dụng thời gian của PromotionHeader
        if (startDate == null && endDate == null) {
            return promotionHeader != null && promotionHeader.isActiveOnDate(date);
        }

        // Kiểm tra thời gian của PromotionLine
        boolean lineDateValid = (startDate == null || !date.isBefore(startDate)) &&
                               (endDate == null || !date.isAfter(endDate));

        // Đồng thời kiểm tra thời gian của PromotionHeader
        boolean headerDateValid = promotionHeader == null || promotionHeader.isActiveOnDate(date);

        return lineDateValid && headerDateValid;
    }

    /**
     * Kiểm tra xem promotion line có hiệu lực trong khoảng thời gian cụ thể không
     */
    public boolean isActiveInDateRange(LocalDate fromDate, LocalDate toDate) {
        if (!active) {
            return false;
        }

        // Nếu không có startDate và endDate, sử dụng thời gian của PromotionHeader
        if (startDate == null && endDate == null) {
            return promotionHeader != null && promotionHeader.isActiveInDateRange(fromDate, toDate);
        }

        // Kiểm tra overlap giữa thời gian của PromotionLine và khoảng thời gian cần kiểm tra
        LocalDate lineStart = startDate != null ? startDate : fromDate;
        LocalDate lineEnd = endDate != null ? endDate : toDate;

        boolean lineDateOverlap = !lineStart.isAfter(toDate) && !lineEnd.isBefore(fromDate);

        // Đồng thời kiểm tra thời gian của PromotionHeader
        boolean headerDateValid = promotionHeader == null || promotionHeader.isActiveInDateRange(fromDate, toDate);

        return lineDateOverlap && headerDateValid;
    }
}

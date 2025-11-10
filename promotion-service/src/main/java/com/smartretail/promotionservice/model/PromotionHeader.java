package com.smartretail.promotionservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotion_headers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String name;


    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean active = true;

    public enum PromotionType {
        DISCOUNT_PERCENT, DISCOUNT_AMOUNT, BUY_X_GET_Y
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
    }

    /**
     * Kiểm tra xem promotion có đang hiệu lực trong ngày cụ thể không
     */
    public boolean isActiveOnDate(LocalDate date) {
        if (!active) {
            return false;
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Kiểm tra xem promotion có hiệu lực trong khoảng thời gian cụ thể không
     */
    public boolean isActiveInDateRange(LocalDate fromDate, LocalDate toDate) {
        if (!active) {
            return false;
        }
        // Kiểm tra overlap giữa thời gian của promotion và khoảng thời gian cần kiểm tra
        return !startDate.isAfter(toDate) && !endDate.isBefore(fromDate);
    }

    /**
     * Kiểm tra xem promotion có đang hiệu lực hôm nay không
     */
    public boolean isCurrentlyActive() {
        return isActiveOnDate(LocalDate.now());
    }
}

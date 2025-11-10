package com.smartretail.orderservice.dto;

import com.smartretail.orderservice.model.ReturnOrder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnOrderDto {

    private Long id;
    private Long orderId;
    private Long customerId;
    private String returnCode;
    private ReturnOrder.ReturnStatus status;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private List<ReturnDetailDto> returnDetails;

    // DTO cho tạo yêu cầu trả hàng (customerId sẽ được lấy từ JWT token)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateReturnRequest {
        private Long orderId;
        private String reason;
        private List<ReturnDetailDto.CreateReturnDetailRequest> returnDetails;
    }

    // DTO cho cập nhật trạng thái trả hàng
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        private ReturnOrder.ReturnStatus status;
        private String adminNote;
    }

    // DTO cho response với thông tin chi tiết
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnOrderResponse {
        private Long id;
        private Long orderId;
        private Long customerId;
        private String returnCode;
        private String customerName;
        private String customerPhone;
        private String customerEmail;
        private ReturnOrder.ReturnStatus status;
        private String reason;
        private String adminNote;
        private BigDecimal totalRefundAmount;
        private LocalDateTime createdAt;
        private LocalDateTime processedAt;
        private List<ReturnDetailDto.ReturnDetailResponse> returnDetails;
        private boolean canApprove;
        private boolean canReject;
        private boolean canComplete;
    }

    // DTO cho danh sách yêu cầu trả hàng
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnOrderSummary {
        private Long id;
        private Long orderId;
        private Long customerId;
        private String returnCode;
        private String customerName;
        private ReturnOrder.ReturnStatus status;
        private String reason;
        private BigDecimal totalRefundAmount;
        private LocalDateTime createdAt;
        private int itemCount;
    }
}

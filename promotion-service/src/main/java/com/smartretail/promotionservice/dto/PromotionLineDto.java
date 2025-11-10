package com.smartretail.promotionservice.dto;

import com.smartretail.promotionservice.model.PromotionLine;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionLineDto {

    private Long id;
    private Long promotionHeaderId;
    private PromotionLine.TargetType targetType;
    private Long targetId;
    private com.smartretail.promotionservice.model.PromotionHeader.PromotionType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
    private List<PromotionDetailDto> promotionDetails;

        public static PromotionLineDto fromEntity(PromotionLine entity) {
        PromotionLineDto dto = new PromotionLineDto();
        dto.setId(entity.getId());
        dto.setPromotionHeaderId(entity.getPromotionHeader().getId());
        dto.setTargetType(entity.getTargetType());
        dto.setTargetId(entity.getTargetId());
        dto.setType(entity.getType());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setActive(entity.getActive());

        // Note: promotionDetails sẽ được populate bởi PromotionService.convertToLineDto()
        // vì entity không có relationship trực tiếp với PromotionDetail

        return dto;
    }

    public PromotionLine toEntity() {
        PromotionLine entity = new PromotionLine();
        entity.setId(this.id);
        entity.setTargetType(this.targetType);
        entity.setTargetId(this.targetId);
        entity.setStartDate(this.startDate);
        entity.setEndDate(this.endDate);
        entity.setActive(this.active != null ? this.active : true);
        entity.setType(this.type);
        return entity;
    }
}

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PromotionService {

    @Autowired
    private PromotionHeaderRepository promotionHeaderRepository;

    @Autowired
    private PromotionLineRepository promotionLineRepository;

    @Autowired
    private PromotionDetailRepository promotionDetailRepository;

    // ==================== PROMOTION HEADER ====================

    /**
     * Tạo mới chương trình khuyến mãi
     */
    public PromotionHeaderDto createPromotionHeader(PromotionHeaderDto dto) {
        // Kiểm tra tên không được trùng
        if (promotionHeaderRepository.existsByNameAndActiveTrue(dto.getName())) {
            throw new RuntimeException("Tên chương trình khuyến mãi đã tồn tại: " + dto.getName());
        }

        // Kiểm tra thời gian hợp lệ
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new RuntimeException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        PromotionHeader header = new PromotionHeader();
        header.setName(dto.getName());
        header.setStartDate(dto.getStartDate());
        header.setEndDate(dto.getEndDate());
        header.setActive(dto.getActive() != null ? dto.getActive() : false); // Mặc định là false

        PromotionHeader savedHeader = promotionHeaderRepository.save(header);
        return convertToHeaderDto(savedHeader);
    }

    /**
     * Cập nhật chương trình khuyến mãi
     */
    public Optional<PromotionHeaderDto> updatePromotionHeader(Long id, PromotionHeaderDto dto) {
        Optional<PromotionHeader> existingHeader = promotionHeaderRepository.findById(id);
        if (existingHeader.isPresent()) {
            PromotionHeader header = existingHeader.get();

            // Kiểm tra tên không được trùng (trừ chính nó)
            if (!header.getName().equals(dto.getName()) &&
                promotionHeaderRepository.existsByNameAndActiveTrue(dto.getName())) {
                throw new RuntimeException("Tên chương trình khuyến mãi đã tồn tại: " + dto.getName());
            }

            // Kiểm tra thời gian hợp lệ
            if (dto.getStartDate().isAfter(dto.getEndDate())) {
                throw new RuntimeException("Ngày bắt đầu phải trước ngày kết thúc");
            }

            header.setName(dto.getName());
            header.setStartDate(dto.getStartDate());
            header.setEndDate(dto.getEndDate());
            if (dto.getActive() != null) {
                header.setActive(dto.getActive());
            }

            PromotionHeader updatedHeader = promotionHeaderRepository.save(header);
            return Optional.of(convertToHeaderDto(updatedHeader));
        }
        return Optional.empty();
    }

    /**
     * Kích hoạt khuyến mãi (set active = true)
     */
    public boolean activatePromotionHeader(Long id) {
        Optional<PromotionHeader> header = promotionHeaderRepository.findById(id);
        if (header.isPresent()) {
            PromotionHeader h = header.get();
            h.setActive(true);
            promotionHeaderRepository.save(h);
            return true;
        }
        return false;
    }

    /**
     * Tắt khuyến mãi (set active = false)
     */
    public boolean deactivatePromotionHeader(Long id) {
        Optional<PromotionHeader> header = promotionHeaderRepository.findById(id);
        if (header.isPresent()) {
            PromotionHeader h = header.get();
            h.setActive(false);
            promotionHeaderRepository.save(h);
            return true;
        }
        return false;
    }

    /**
     * Xóa chương trình khuyến mãi (soft delete)
     */
    public boolean deletePromotionHeader(Long id) {
        Optional<PromotionHeader> header = promotionHeaderRepository.findById(id);
        if (header.isPresent()) {
            PromotionHeader h = header.get();
            h.setActive(false);
            promotionHeaderRepository.save(h);
            return true;
        }
        return false;
    }

    /**
     * Lấy chương trình khuyến mãi theo ID
     */
    public Optional<PromotionHeaderDto> getPromotionHeaderById(Long id) {
        Optional<PromotionHeader> header = promotionHeaderRepository.findById(id);
        return header.map(this::convertToHeaderDto);
    }

    /**
     * Lấy tất cả chương trình khuyến mãi đang hoạt động
     */
    public List<PromotionHeaderDto> getAllActivePromotionHeaders() {
        List<PromotionHeader> headers = promotionHeaderRepository.findByActiveTrue();
        return headers.stream()
                .map(this::convertToHeaderDto)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả chương trình khuyến mãi (cả active và inactive)
     */
    public List<PromotionHeaderDto> getAllPromotionHeaders() {
        List<PromotionHeader> headers = promotionHeaderRepository.findAll();
        return headers.stream()
                .map(this::convertToHeaderDto)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chương trình khuyến mãi đang hiệu lực theo ngày
     */
    public List<PromotionHeaderDto> getActivePromotionsByDate(LocalDate date) {
        List<PromotionHeader> headers = promotionHeaderRepository.findActivePromotionsByDate(date);
        return headers.stream()
                .map(this::convertToHeaderDto)
                .collect(Collectors.toList());
    }

    /**
     * Tìm kiếm chương trình khuyến mãi theo tên
     */
    public List<PromotionHeaderDto> searchPromotionHeadersByName(String name) {
        List<PromotionHeader> headers = promotionHeaderRepository.findByNameContainingIgnoreCase(name);
        return headers.stream()
                .map(this::convertToHeaderDto)
                .collect(Collectors.toList());
    }

    // ==================== PROMOTION LINE ====================

    /**
     * Tạo promotion line mới
     */
    public PromotionLineDto createPromotionLine(PromotionLineDto dto) {
        // Kiểm tra promotion header có tồn tại không
        PromotionHeader header = promotionHeaderRepository.findById(dto.getPromotionHeaderId())
            .orElseThrow(() -> new RuntimeException("Promotion header không tồn tại"));

        // Kiểm tra promotion header có đang active không
        if (!header.getActive()) {
            throw new RuntimeException("Không thể tạo promotion line cho promotion header đã bị tắt");
        }

        // Validate thời gian của promotion line
        if (dto.getStartDate() != null || dto.getEndDate() != null) {
            if (dto.getStartDate() != null && dto.getEndDate() != null) {
                // Nếu có cả startDate và endDate, kiểm tra startDate < endDate
                if (dto.getStartDate().isAfter(dto.getEndDate())) {
                    throw new RuntimeException("Start date phải trước end date");
                }
            }

            // Kiểm tra thời gian của promotion line phải nằm trong khoảng thời gian của promotion header
            if (dto.getStartDate() != null && dto.getStartDate().isBefore(header.getStartDate())) {
                throw new RuntimeException("Start date của promotion line không thể trước start date của promotion header");
            }
            if (dto.getEndDate() != null && dto.getEndDate().isAfter(header.getEndDate())) {
                throw new RuntimeException("End date của promotion line không thể sau end date của promotion header");
            }
        }

        PromotionLine line = dto.toEntity();
        line.setPromotionHeader(header);
        line.setActive(dto.getActive() != null ? dto.getActive() : true); // Mặc định là true

        PromotionLine savedLine = promotionLineRepository.save(line);
        return PromotionLineDto.fromEntity(savedLine);
    }

    /**
     * Kích hoạt promotion line
     */
    public boolean activatePromotionLine(Long id) {
        Optional<PromotionLine> line = promotionLineRepository.findById(id);
        if (line.isPresent()) {
            PromotionLine l = line.get();
            // Kiểm tra promotion header có đang active không
            if (!l.getPromotionHeader().getActive()) {
                throw new RuntimeException("Không thể kích hoạt promotion line khi promotion header đã bị tắt");
            }
            l.setActive(true);
            promotionLineRepository.save(l);
            return true;
        }
        return false;
    }

    /**
     * Tắt promotion line
     */
    public boolean deactivatePromotionLine(Long id) {
        Optional<PromotionLine> line = promotionLineRepository.findById(id);
        if (line.isPresent()) {
            PromotionLine l = line.get();
            l.setActive(false);
            promotionLineRepository.save(l);
            return true;
        }
        return false;
    }

    /**
     * Lấy tất cả promotion lines của một promotion header
     */
    public List<PromotionLineDto> getPromotionLinesByHeaderId(Long headerId) {
        List<PromotionLine> lines = promotionLineRepository.findByPromotionHeaderIdAndActiveTrue(headerId);
        return lines.stream()
                .map(this::convertToLineDto)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả promotion lines của một promotion header (cả active và inactive)
     */
    public List<PromotionLineDto> getAllPromotionLinesByHeaderId(Long headerId) {
        List<PromotionLine> lines = promotionLineRepository.findByPromotionHeaderId(headerId);
        return lines.stream()
                .map(this::convertToLineDto)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật promotion line
     */
    public Optional<PromotionLineDto> updatePromotionLine(Long id, PromotionLineDto dto) {
        Optional<PromotionLine> lineOpt = promotionLineRepository.findById(id);
        if (lineOpt.isEmpty()) {
            return Optional.empty();
        }

        PromotionLine line = lineOpt.get();
        PromotionHeader header = line.getPromotionHeader();

        // Validate time range
        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new RuntimeException("Start date phải trước end date");
        }
        if (dto.getStartDate() != null && dto.getStartDate().isBefore(header.getStartDate())) {
            throw new RuntimeException("Start date của promotion line không thể trước start date của promotion header");
        }
        if (dto.getEndDate() != null && dto.getEndDate().isAfter(header.getEndDate())) {
            throw new RuntimeException("End date của promotion line không thể sau end date của promotion header");
        }

        if (dto.getTargetType() != null) line.setTargetType(dto.getTargetType());
        if (dto.getTargetId() != null) line.setTargetId(dto.getTargetId());
        if (dto.getType() != null) line.setType(dto.getType());
        if (dto.getStartDate() != null) line.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) line.setEndDate(dto.getEndDate());
        if (dto.getActive() != null) line.setActive(dto.getActive());

        PromotionLine saved = promotionLineRepository.save(line);
        return Optional.of(convertToLineDto(saved));
    }

    // ==================== PROMOTION DETAIL ====================

    /**
     * Tạo mới promotion detail
     */
    public PromotionDetailDto createPromotionDetail(PromotionDetailDto dto) {
        // Kiểm tra promotion line tồn tại
        PromotionLine line = promotionLineRepository.findById(dto.getPromotionLineId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy promotion line với ID: " + dto.getPromotionLineId()));

        // Kiểm tra promotion line có đang active không
        if (!line.getActive()) {
            throw new RuntimeException("Không thể tạo promotion detail cho promotion line đã bị tắt");
        }

        // Kiểm tra promotion header có đang active không
        if (!line.getPromotionHeader().getActive()) {
            throw new RuntimeException("Không thể tạo promotion detail cho promotion header đã bị tắt");
        }

        PromotionDetail detail = new PromotionDetail();
        detail.setPromotionLine(line);
        detail.setDiscountPercent(dto.getDiscountPercent());
        detail.setDiscountAmount(dto.getDiscountAmount());
        detail.setConditionQuantity(dto.getConditionQuantity());
        detail.setFreeQuantity(dto.getFreeQuantity());
        detail.setConditionProductUnitId(dto.getConditionProductUnitId());
        detail.setGiftProductUnitId(dto.getGiftProductUnitId());
        detail.setMinAmount(dto.getMinAmount());
        detail.setMaxDiscount(dto.getMaxDiscount());
        detail.setActive(dto.getActive() != null ? dto.getActive() : true); // Mặc định là true

        PromotionDetail savedDetail = promotionDetailRepository.save(detail);
        return convertToDetailDto(savedDetail);
    }

    /**
     * Kích hoạt promotion detail
     */
    public boolean activatePromotionDetail(Long id) {
        Optional<PromotionDetail> detail = promotionDetailRepository.findById(id);
        if (detail.isPresent()) {
            PromotionDetail d = detail.get();
            // Kiểm tra promotion line và header có đang active không
            if (!d.getPromotionLine().getActive()) {
                throw new RuntimeException("Không thể kích hoạt promotion detail khi promotion line đã bị tắt");
            }
            if (!d.getPromotionLine().getPromotionHeader().getActive()) {
                throw new RuntimeException("Không thể kích hoạt promotion detail khi promotion header đã bị tắt");
            }
            d.setActive(true);
            promotionDetailRepository.save(d);
            return true;
        }
        return false;
    }

    /**
     * Tắt promotion detail
     */
    public boolean deactivatePromotionDetail(Long id) {
        Optional<PromotionDetail> detail = promotionDetailRepository.findById(id);
        if (detail.isPresent()) {
            PromotionDetail d = detail.get();
            d.setActive(false);
            promotionDetailRepository.save(d);
            return true;
        }
        return false;
    }

    /**
     * Lấy tất cả promotion details của một promotion line
     */
    public List<PromotionDetailDto> getPromotionDetailsByLineId(Long lineId) {
        List<PromotionDetail> details = promotionDetailRepository.findByPromotionLineIdAndActiveTrue(lineId);
        return details.stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả promotion details của một promotion line (cả active và inactive)
     */
    public List<PromotionDetailDto> getAllPromotionDetailsByLineId(Long lineId) {
        List<PromotionDetail> details = promotionDetailRepository.findByPromotionLineId(lineId);
        return details.stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật promotion detail
     */
    public Optional<PromotionDetailDto> updatePromotionDetail(Long id, PromotionDetailDto dto) {
        Optional<PromotionDetail> opt = promotionDetailRepository.findById(id);
        if (opt.isEmpty()) {
            return Optional.empty();
        }

        PromotionDetail d = opt.get();

        // Basic validations
        if (dto.getDiscountPercent() != null && dto.getDiscountPercent() < 0f) {
            throw new RuntimeException("discountPercent không hợp lệ");
        }
        if (dto.getDiscountAmount() != null && dto.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new RuntimeException("discountAmount không hợp lệ");
        }
        if (dto.getConditionQuantity() != null && dto.getConditionQuantity() < 0) {
            throw new RuntimeException("conditionQuantity không hợp lệ");
        }
        if (dto.getFreeQuantity() != null && dto.getFreeQuantity() < 0) {
            throw new RuntimeException("freeQuantity không hợp lệ");
        }

        if (dto.getDiscountPercent() != null) d.setDiscountPercent(dto.getDiscountPercent());
        if (dto.getDiscountAmount() != null) d.setDiscountAmount(dto.getDiscountAmount());
        if (dto.getConditionQuantity() != null) d.setConditionQuantity(dto.getConditionQuantity());
        if (dto.getFreeQuantity() != null) d.setFreeQuantity(dto.getFreeQuantity());
        if (dto.getConditionProductUnitId() != null) d.setConditionProductUnitId(dto.getConditionProductUnitId());
        if (dto.getGiftProductUnitId() != null) d.setGiftProductUnitId(dto.getGiftProductUnitId());
        if (dto.getMinAmount() != null) d.setMinAmount(dto.getMinAmount());
        if (dto.getMaxDiscount() != null) d.setMaxDiscount(dto.getMaxDiscount());
        if (dto.getActive() != null) d.setActive(dto.getActive());

        PromotionDetail saved = promotionDetailRepository.save(d);
        return Optional.of(convertToDetailDto(saved));
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Chuyển đổi PromotionHeader thành DTO
     */
    private PromotionHeaderDto convertToHeaderDto(PromotionHeader header) {
        PromotionHeaderDto dto = new PromotionHeaderDto();
        dto.setId(header.getId());
        dto.setName(header.getName());
        // type đã chuyển xuống line, header không còn type
        dto.setStartDate(header.getStartDate());
        dto.setEndDate(header.getEndDate());
        dto.setCreatedAt(header.getCreatedAt());
        dto.setActive(header.getActive());

        // Lấy promotion lines (cả active và inactive)
        List<PromotionLine> lines = promotionLineRepository.findByPromotionHeaderId(header.getId());
        List<PromotionLineDto> lineDtos = lines.stream()
                .map(this::convertToLineDto)
                .collect(Collectors.toList());
        dto.setPromotionLines(lineDtos);

        return dto;
    }

    /**
     * Chuyển đổi PromotionLine thành DTO
     */
    private PromotionLineDto convertToLineDto(PromotionLine line) {
        PromotionLineDto dto = new PromotionLineDto();
        dto.setId(line.getId());
        dto.setPromotionHeaderId(line.getPromotionHeader().getId());
        dto.setTargetType(line.getTargetType());
        dto.setTargetId(line.getTargetId());
        dto.setType(line.getType());
        dto.setStartDate(line.getStartDate());
        dto.setEndDate(line.getEndDate());
        dto.setActive(line.getActive());

        // Lấy promotion details (cả active và inactive)
        List<PromotionDetail> details = promotionDetailRepository.findByPromotionLineId(line.getId());
        List<PromotionDetailDto> detailDtos = details.stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
        dto.setPromotionDetails(detailDtos);

        return dto;
    }

    /**
     * Chuyển đổi PromotionDetail thành DTO
     */
    private PromotionDetailDto convertToDetailDto(PromotionDetail detail) {
        PromotionDetailDto dto = new PromotionDetailDto();
        dto.setId(detail.getId());
        dto.setPromotionLineId(detail.getPromotionLine().getId());
        dto.setDiscountPercent(detail.getDiscountPercent());
        dto.setDiscountAmount(detail.getDiscountAmount());
        dto.setConditionQuantity(detail.getConditionQuantity());
        dto.setFreeQuantity(detail.getFreeQuantity());
        dto.setConditionProductUnitId(detail.getConditionProductUnitId());
        dto.setGiftProductUnitId(detail.getGiftProductUnitId());
        dto.setMinAmount(detail.getMinAmount());
        dto.setMaxDiscount(detail.getMaxDiscount());
        dto.setActive(detail.getActive());
        return dto;
    }
}

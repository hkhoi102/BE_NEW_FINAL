package com.smartretail.serviceproduct.service;

import com.smartretail.serviceproduct.dto.PriceListDto;
import com.smartretail.serviceproduct.model.PriceList;
import com.smartretail.serviceproduct.model.PriceHeader;
import com.smartretail.serviceproduct.model.Product;
import com.smartretail.serviceproduct.model.ProductUnit;
import com.smartretail.serviceproduct.repository.PriceListRepository;
import com.smartretail.serviceproduct.repository.PriceHeaderRepository;
import com.smartretail.serviceproduct.repository.ProductUnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class PriceListService {

    @Autowired
    private PriceListRepository priceListRepository;

    @Autowired
    private ProductUnitRepository productUnitRepository;

    @Autowired
    private PriceHeaderRepository priceHeaderRepository;

    // Kiểm tra xung đột thời gian giữa 2 header
    private boolean hasTimeConflict(PriceHeader existing, PriceHeader newHeader) {
        LocalDateTime existingStart = existing.getTimeStart();
        LocalDateTime existingEnd = existing.getTimeEnd();
        LocalDateTime newStart = newHeader.getTimeStart();
        LocalDateTime newEnd = newHeader.getTimeEnd();

        // Nếu cả 2 đều không có thời gian → không xung đột
        if (existingStart == null && existingEnd == null && newStart == null && newEnd == null) {
            return false;
        }

        // Nếu một trong hai không có thời gian → xung đột
        if ((existingStart == null && existingEnd == null) || (newStart == null && newEnd == null)) {
            return true;
        }

        // Kiểm tra overlap: [existingStart, existingEnd] và [newStart, newEnd]
        // Xung đột nếu: existingStart < newEnd && newStart < existingEnd
        if (existingStart != null && newEnd != null && existingStart.isBefore(newEnd) &&
            newStart != null && existingEnd != null && newStart.isBefore(existingEnd)) {
            return true;
        }

        return false;
    }

    // Format thời gian để hiển thị lỗi
    private String formatTimePeriod(PriceHeader header) {
        if (header.getTimeStart() == null && header.getTimeEnd() == null) {
            return "header '" + header.getName() + "' (không giới hạn thời gian)";
        }

        String startStr = header.getTimeStart() != null ? formatDateTime(header.getTimeStart()) : "không giới hạn";
        String endStr = header.getTimeEnd() != null ? formatDateTime(header.getTimeEnd()) : "không giới hạn";

        return "header '" + header.getName() + "' (" + startStr + " - " + endStr + ")";
    }

    // Format LocalDateTime thành dd/mm/yyyy
    private String formatDateTime(LocalDateTime dateTime) {
        return String.format("%02d/%02d/%04d",
            dateTime.getDayOfMonth(),
            dateTime.getMonthValue(),
            dateTime.getYear());
    }

    // API kiểm tra xung đột thời gian (chỉ kiểm tra, không tạo giá)
    public Map<String, Object> checkTimeConflict(Long productUnitId, Long priceHeaderId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Lấy PriceHeader
            PriceHeader newHeader = priceHeaderRepository.findById(priceHeaderId)
                    .orElseThrow(() -> new RuntimeException("PriceHeader not found with id: " + priceHeaderId));

            // Lấy tất cả price headers có chứa sản phẩm này
            List<PriceList> existingPrices = priceListRepository.findByProductUnitIdAndActiveTrue(productUnitId);

            for (PriceList existingPrice : existingPrices) {
                PriceHeader existingHeader = existingPrice.getPriceHeader();
                if (existingHeader == null || existingHeader.getId().equals(newHeader.getId())) {
                    continue; // Bỏ qua nếu cùng header hoặc không có header
                }

                // Kiểm tra xung đột thời gian
                if (hasTimeConflict(existingHeader, newHeader)) {
                    String productName = existingPrice.getProductUnit().getProduct().getName();
                    String unitName = existingPrice.getProductUnit().getUnit().getName();
                    String existingPeriod = formatTimePeriod(existingHeader);
                    String newPeriod = formatTimePeriod(newHeader);

                    result.put("hasConflict", true);
                    result.put("message", String.format("Xung đột thời gian: Sản phẩm '%s (%s)' đã có giá trong %s, không thể thêm vào %s",
                        productName, unitName, existingPeriod, newPeriod));
                    return result;
                }
            }

            // Không có xung đột
            result.put("hasConflict", false);
            result.put("message", "Không có xung đột thời gian, có thể thêm giá cho sản phẩm này");

        } catch (Exception e) {
            result.put("hasConflict", true);
            result.put("message", "Lỗi khi kiểm tra: " + e.getMessage());
        }

        return result;
    }

    // Create new price
    public PriceListDto createPrice(PriceListDto priceDto) {
        try {
            System.out.println("🔍 Creating price with data: " + priceDto);

            ProductUnit productUnit = productUnitRepository.findById(priceDto.getProductUnitId())
                    .orElseThrow(() -> new RuntimeException("ProductUnit not found with id: " + priceDto.getProductUnitId()));

            // Validate product has code
            Product product = productUnit.getProduct();
            if (product.getCode() == null || product.getCode().trim().isEmpty()) {
                throw new RuntimeException("Không thể tạo giá cho sản phẩm không có mã sản phẩm: " + product.getName());
            }

            // No time range validation anymore

            // Đảm bảo có PriceHeader cho sản phẩm
            PriceHeader header = ensurePriceHeaderExists(
                    productUnit,
                    priceDto.getPriceHeaderId(),
                    null
            );

            // No overlapping/time-window checks anymore

            PriceList priceList = new PriceList();
            priceList.setProductUnit(productUnit);
            priceList.setPrice(priceDto.getPrice());
            priceList.setPriceHeader(header);
            priceList.setActive(true);

            PriceList savedPrice = priceListRepository.save(priceList);
            System.out.println("✅ Price created successfully: " + savedPrice.getId());
            return convertToDto(savedPrice);
        } catch (Exception e) {
            System.err.println("❌ Error creating price: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Create multiple prices under a specific header
    public List<PriceListDto> createPricesBulk(Long headerId, List<PriceListDto> items) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Danh sách giá không được rỗng");
        }

        // Ensure header exists if provided
        if (headerId != null) {
            priceHeaderRepository.findById(headerId)
                    .orElseThrow(() -> new RuntimeException("PriceHeader not found with id: " + headerId));
        }

        // Check for duplicate product units in the same header
        List<Long> productUnitIds = items.stream()
                .map(PriceListDto::getProductUnitId)
                .collect(Collectors.toList());

        if (headerId != null) {
            // Check if any of these product units already have prices in this header
            List<PriceList> existingPrices = priceListRepository.findByPriceHeaderIdAndProductUnitIdIn(headerId, productUnitIds);
            if (!existingPrices.isEmpty()) {
                List<String> duplicateProducts = existingPrices.stream()
                        .map(pl -> pl.getProductUnit().getProduct().getName() + " (" + pl.getProductUnit().getUnit().getName() + ")")
                        .collect(Collectors.toList());
                throw new RuntimeException("Các sản phẩm sau đã có giá trong header này: " + String.join(", ", duplicateProducts));
            }
        }

        return items.stream()
                .map(dto -> {
                    dto.setPriceHeaderId(headerId);
                    return createPrice(dto);
                })
                .collect(Collectors.toList());
    }

    private PriceHeader ensurePriceHeaderExists(ProductUnit productUnit, Long maybeHeaderId, LocalDateTime ignored) {
        if (maybeHeaderId != null) {
            return priceHeaderRepository.findById(maybeHeaderId)
                    .orElseThrow(() -> new RuntimeException("PriceHeader not found with id: " + maybeHeaderId));
        }

        // Tìm header hiện hành theo thời điểm (global), nếu chưa có thì tạo mới
        List<PriceHeader> headers = priceHeaderRepository.findCurrentHeaders(null);
        if (!headers.isEmpty()) {
            return headers.get(0);
        }

        PriceHeader header = new PriceHeader();

        header.setName("DEFAULT");
        header.setDescription("Auto-created header");
        header.setTimeStart(null);
        header.setTimeEnd(null);
        header.setActive(true);
        return priceHeaderRepository.save(header);
    }


    // Get price history by product
    public List<PriceListDto> getPriceHistoryByProduct(Long productId) {
        List<PriceList> prices = priceListRepository.findPriceHistoryByProduct(productId);
        return prices.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Get current price by product unit
    public Optional<BigDecimal> getCurrentPrice(Long productUnitId) {
        return priceListRepository.findTopByProductUnitIdAndActiveTrueOrderByCreatedAtDesc(productUnitId)
                .map(PriceList::getPrice);
    }

    // Update price
    public Optional<PriceListDto> updatePrice(Long id, PriceListDto priceDto) {
        Optional<PriceList> existingPrice = priceListRepository.findById(id);
        if (existingPrice.isPresent()) {
            PriceList price = existingPrice.get();

            price.setPrice(priceDto.getPrice());

            PriceList updatedPrice = priceListRepository.save(price);
            return Optional.of(convertToDto(updatedPrice));
        }
        return Optional.empty();
    }

    // Delete price
    public boolean deletePrice(Long id) {
        Optional<PriceList> price = priceListRepository.findById(id);
        if (price.isPresent()) {
            PriceList p = price.get();
            p.setActive(false);
            priceListRepository.save(p);
            return true;
        }
        return false;
    }

    // Get prices by product unit
    public List<PriceListDto> getPricesByProductUnit(Long productUnitId) {
        List<PriceList> prices = priceListRepository.findByProductUnitIdAndActiveTrue(productUnitId);
        return prices.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Convert entity to DTO
    private PriceListDto convertToDto(PriceList priceList) {
        return new PriceListDto(
            priceList.getId(),
            priceList.getProductUnit().getId(),
            priceList.getProductUnit().getProduct().getId(),
            priceList.getProductUnit().getProduct().getName(),
            priceList.getProductUnit().getUnit().getId(),
            priceList.getProductUnit().getUnit().getName(),
            priceList.getProductUnit().getProduct().getCode(),
            priceList.getPriceHeader() != null ? priceList.getPriceHeader().getId() : null,
            priceList.getPrice(),
            null,
            null,
            priceList.getActive(),
            priceList.getCreatedAt()
        );
    }
}

package com.smartretail.serviceproduct.service;

import com.smartretail.serviceproduct.dto.PriceHeaderDto;
import com.smartretail.serviceproduct.model.PriceHeader;
import com.smartretail.serviceproduct.repository.PriceHeaderRepository;
import com.smartretail.serviceproduct.repository.ProductUnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PriceHeaderService {

    @Autowired
    private PriceHeaderRepository priceHeaderRepository;

    @Autowired
    private ProductUnitRepository productUnitRepository; // kept for future use

    @Autowired
    private com.smartretail.serviceproduct.repository.PriceListRepository priceListRepository;
    @Autowired
    private com.smartretail.serviceproduct.service.PriceListService priceListService;

    // Create a global price header (not tied to a specific product unit)
    public PriceHeaderDto createGlobal(PriceHeaderDto dto) {
        validateTimeRange(dto.getTimeStart(), dto.getTimeEnd());

        PriceHeader header = new PriceHeader();
        header.setName(dto.getName());
        header.setDescription(dto.getDescription());
        header.setTimeStart(dto.getTimeStart());
        header.setTimeEnd(dto.getTimeEnd());
        header.setActive(dto.getActive() == null ? true : dto.getActive());

        PriceHeader saved = priceHeaderRepository.save(header);
        return toDto(saved);
    }

    // New: list all active headers (global)
    public List<PriceHeaderDto> listAllActive() {
        return priceHeaderRepository.findByActiveTrue()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // List all headers (both active and inactive)
    public List<PriceHeaderDto> listAllHeaders() {
        return priceHeaderRepository.findAll()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public java.util.Map<String, Object> getHeaderWithItems(Long headerId) {
        PriceHeader header = priceHeaderRepository.findById(headerId)
                .orElseThrow(() -> new RuntimeException("PriceHeader not found: " + headerId));
        var headerDto = toDto(header);
        var items = priceListRepository.findByHeaderId(headerId)
                .stream().map(pl -> new com.smartretail.serviceproduct.dto.PriceListDto(
                        pl.getId(),
                        pl.getProductUnit().getId(),
                        pl.getProductUnit().getProduct().getId(),
                        pl.getProductUnit().getProduct().getName(),
                        pl.getProductUnit().getUnit().getId(),
                        pl.getProductUnit().getUnit().getName(),
                        pl.getProductUnit().getProduct().getCode(),
                        pl.getPriceHeader() != null ? pl.getPriceHeader().getId() : null,
                        pl.getPrice(),
                        null,
                        null,
                        pl.getActive(),
                        pl.getCreatedAt()
                )).collect(java.util.stream.Collectors.toList());
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("header", headerDto);
        result.put("items", items);
        result.put("total", items.size());
        return result;
    }

    // Check which products already have prices in this header
    public java.util.Map<String, Object> checkProductsInHeader(Long headerId) {
        PriceHeader header = priceHeaderRepository.findById(headerId)
                .orElseThrow(() -> new RuntimeException("PriceHeader not found: " + headerId));

        var existingPrices = priceListRepository.findByHeaderId(headerId);

        // Group by product
        var productsInHeader = existingPrices.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    pl -> pl.getProductUnit().getProduct().getId(),
                    java.util.stream.Collectors.mapping(
                        pl -> new java.util.HashMap<String, Object>() {{
                            put("productUnitId", pl.getProductUnit().getId());
                            put("unitName", pl.getProductUnit().getUnit().getName());
                            put("price", pl.getPrice());
                            put("createdAt", pl.getCreatedAt());
                        }},
                        java.util.stream.Collectors.toList()
                    )
                ));

        // Convert to response format
        var result = new java.util.HashMap<String, Object>();
        result.put("headerId", headerId);
        result.put("headerName", header.getName());
        result.put("productsInHeader", productsInHeader.entrySet().stream()
                .map(entry -> {
                    var productId = entry.getKey();
                    var firstPrice = existingPrices.stream()
                            .filter(pl -> pl.getProductUnit().getProduct().getId().equals(productId))
                            .findFirst()
                            .orElse(null);

                    return new java.util.HashMap<String, Object>() {{
                        put("productId", productId);
                        put("productName", firstPrice.getProductUnit().getProduct().getName());
                        put("productCode", firstPrice.getProductUnit().getProduct().getCode());
                        put("units", entry.getValue());
                        put("totalUnits", entry.getValue().size());
                    }};
                })
                .collect(java.util.stream.Collectors.toList()));
        result.put("totalProducts", productsInHeader.size());
        result.put("totalPrices", existingPrices.size());

        return result;
    }

    // Deactivate price header (set active = false)
    public boolean deactivateHeader(Long headerId) {
        return priceHeaderRepository.findById(headerId)
                .map(header -> {
                    header.setActive(false);
                    priceHeaderRepository.save(header);
                    return true;
                })
                .orElse(false);
    }

    // Activate price header (set active = true)
    public boolean activateHeader(Long headerId) {
        return priceHeaderRepository.findById(headerId)
                .map(header -> {
                    header.setActive(true);
                    priceHeaderRepository.save(header);
                    return true;
                })
                .orElse(false);
    }

    // time window validation retained for header creation/updates
    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !start.isBefore(end)) {
            throw new RuntimeException("timeStart phải trước timeEnd");
        }
    }

    private PriceHeaderDto toDto(PriceHeader h) {
        // product/unit fields are null for global headers
        return new PriceHeaderDto(
                h.getId(),
                null,
                null,
                null,
                null,
                h.getName(),
                h.getDescription(),
                h.getTimeStart(),
                h.getTimeEnd(),
                h.getActive(),
                h.getCreatedAt()
        );
    }
}



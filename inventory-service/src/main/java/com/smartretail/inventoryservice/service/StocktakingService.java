package com.smartretail.inventoryservice.service;

import com.smartretail.inventoryservice.model.StockLocation;
import com.smartretail.inventoryservice.model.Stocktaking;
import com.smartretail.inventoryservice.model.StocktakingDetail;
import com.smartretail.inventoryservice.model.Warehouse;
import com.smartretail.inventoryservice.repository.StockLocationRepository;
import com.smartretail.inventoryservice.repository.StocktakingDetailRepository;
import com.smartretail.inventoryservice.repository.StocktakingRepository;
import com.smartretail.inventoryservice.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StocktakingService {

    private final StocktakingRepository stocktakingRepository;
    private final StocktakingDetailRepository stocktakingDetailRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockLocationRepository stockLocationRepository;
    private final StockDocumentService stockDocumentService;
    private final com.smartretail.inventoryservice.client.ProductServiceClient productServiceClient;

    // Tạo phiếu kiểm kê
    public Stocktaking createStocktaking(LocalDateTime stocktakingDate, Long warehouseId, Long stockLocationId, String note, Long createdBy, String createdByUsername) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + warehouseId));
        if (stockLocationId == null) {
            throw new RuntimeException("Stock location is required for stocktaking");
        }
        StockLocation stockLocation = stockLocationRepository.findById(stockLocationId)
                .orElseThrow(() -> new RuntimeException("Stock location not found with id: " + stockLocationId));

        Stocktaking st = new Stocktaking();
        st.setWarehouse(warehouse);
        st.setStockLocation(stockLocation);
        st.setStocktakingNumber("ST-" + System.currentTimeMillis());
        st.setStocktakingDate(stocktakingDate != null ? stocktakingDate : LocalDateTime.now());
        st.setStatus(Stocktaking.StocktakingStatus.IN_PROGRESS);
        st.setNote(note);
        st.setCreatedBy(createdBy);
        st.setCreatedByUsername(createdByUsername);
        return stocktakingRepository.save(st);
    }

    // Thêm/ghi chi tiết kiểm kê (một dòng sản phẩm) - lưu ngay vào DB
    public StocktakingDetail upsertDetail(Long stocktakingId, Long productUnitId, Integer systemQty, Integer actualQty, String note) {
        Stocktaking st = stocktakingRepository.findById(stocktakingId)
                .orElseThrow(() -> new RuntimeException("Stocktaking not found with id: " + stocktakingId));

        StocktakingDetail d = new StocktakingDetail();
        d.setStocktaking(st);
        d.setProductUnitId(productUnitId);
        d.setSystemQuantity(systemQty);
        d.setActualQuantity(actualQty);
        d.setNote(note);
        return stocktakingDetailRepository.save(d);
    }

    // Xác nhận kiểm kê - nhận danh sách chi tiết từ FE (không lưu từng dòng trước đó)
    public void confirmWithPayload(Long stocktakingId, List<com.smartretail.inventoryservice.dto.StocktakingDetailDto> detailsPayload) {
        Stocktaking st = stocktakingRepository.findById(stocktakingId)
                .orElseThrow(() -> new RuntimeException("Stocktaking not found with id: " + stocktakingId));

        // Lưu chi tiết để có lịch sử
        for (com.smartretail.inventoryservice.dto.StocktakingDetailDto p : detailsPayload) {
            StocktakingDetail d = new StocktakingDetail();
            d.setStocktaking(st);
            d.setProductUnitId(p.getProductUnitId());
            d.setSystemQuantity(p.getSystemQuantity());
            d.setActualQuantity(p.getActualQuantity());
            d.setNote(p.getNote());
            stocktakingDetailRepository.save(d);
        }

        // Tạo phiếu nhập/xuất cho các sản phẩm có chênh lệch
        createStockDocumentsForAdjustments(st, detailsPayload);

        st.setStatus(Stocktaking.StocktakingStatus.CONFIRMED);
        st.setCompletedDate(LocalDateTime.now());
        stocktakingRepository.save(st);
    }

    // Xác nhận kiểm kê: tạo phiếu nhập/xuất theo chênh lệch
    public void confirm(Long stocktakingId) {
        Stocktaking st = stocktakingRepository.findById(stocktakingId)
                .orElseThrow(() -> new RuntimeException("Stocktaking not found with id: " + stocktakingId));

        List<StocktakingDetail> details = stocktakingDetailRepository.findByStocktaking(st);
        if (details.isEmpty()) {
            throw new RuntimeException("No stocktaking details to confirm. Send details in payload or add details first.");
        }

        // Convert details to DTOs for processing
        List<com.smartretail.inventoryservice.dto.StocktakingDetailDto> detailDtos = details.stream()
                .map(detail -> {
                    com.smartretail.inventoryservice.dto.StocktakingDetailDto dto = new com.smartretail.inventoryservice.dto.StocktakingDetailDto();
                    dto.setProductUnitId(detail.getProductUnitId());
                    dto.setSystemQuantity(detail.getSystemQuantity());
                    dto.setActualQuantity(detail.getActualQuantity());
                    dto.setNote(detail.getNote());
                    return dto;
                })
                .collect(Collectors.toList());

        // Tạo phiếu nhập/xuất cho các sản phẩm có chênh lệch
        createStockDocumentsForAdjustments(st, detailDtos);

        st.setStatus(Stocktaking.StocktakingStatus.CONFIRMED);
        st.setCompletedDate(LocalDateTime.now());
        stocktakingRepository.save(st);
    }

    // Tạo phiếu nhập/xuất cho các sản phẩm có chênh lệch trong kiểm kê
    private void createStockDocumentsForAdjustments(Stocktaking st, List<com.smartretail.inventoryservice.dto.StocktakingDetailDto> detailsPayload) {
        try {
            log.info("Creating stock documents for stocktaking adjustments: {}", st.getStocktakingNumber());

            // Phân loại sản phẩm theo chênh lệch
            List<com.smartretail.inventoryservice.dto.StocktakingDetailDto> importItems = new java.util.ArrayList<>();
            List<com.smartretail.inventoryservice.dto.StocktakingDetailDto> exportItems = new java.util.ArrayList<>();

            for (com.smartretail.inventoryservice.dto.StocktakingDetailDto p : detailsPayload) {
                int systemQty = p.getSystemQuantity() != null ? p.getSystemQuantity() : 0;
                int actualQty = p.getActualQuantity() != null ? p.getActualQuantity() : 0;
                int diff = actualQty - systemQty;

                if (diff > 0) {
                    // Thừa hàng -> Tạo phiếu NHẬP
                    importItems.add(p);
                } else if (diff < 0) {
                    // Thiếu hàng -> Tạo phiếu XUẤT
                    exportItems.add(p);
                }
                // diff == 0: Không có chênh lệch, bỏ qua
            }

            // Tạo phiếu NHẬP nếu có sản phẩm thừa
            if (!importItems.isEmpty()) {
                createInboundDocumentForAdjustments(st, importItems);
            }

            // Tạo phiếu XUẤT nếu có sản phẩm thiếu
            if (!exportItems.isEmpty()) {
                createOutboundDocumentForAdjustments(st, exportItems);
            }

            log.info("Successfully created stock documents for stocktaking: {}", st.getStocktakingNumber());
        } catch (Exception e) {
            log.error("Failed to create stock documents for stocktaking {}: {}", st.getStocktakingNumber(), e.getMessage());
            throw new RuntimeException("Failed to create stock documents for stocktaking adjustments: " + e.getMessage(), e);
        }
    }

    // Tạo phiếu NHẬP cho các sản phẩm thừa
    private void createInboundDocumentForAdjustments(Stocktaking st, List<com.smartretail.inventoryservice.dto.StocktakingDetailDto> importItems) {
        try {
            log.info("Creating inbound document for {} import items", importItems.size());

            // Tạo phiếu nhập
            com.smartretail.inventoryservice.dto.StockDocumentDto inboundDoc = new com.smartretail.inventoryservice.dto.StockDocumentDto();
            inboundDoc.type = "INBOUND";
            inboundDoc.warehouseId = st.getWarehouse().getId();
            inboundDoc.stockLocationId = st.getStockLocation().getId();
            inboundDoc.referenceNumber = st.getStocktakingNumber() + "-IN";
            inboundDoc.note = "Phiếu nhập điều chỉnh từ kiểm kê: " + st.getStocktakingNumber();

            com.smartretail.inventoryservice.dto.StockDocumentDto createdDoc = stockDocumentService.create(inboundDoc);
            log.info("Created inbound document ID: {}", createdDoc.id);

            // Thêm sản phẩm vào phiếu nhập
            List<com.smartretail.inventoryservice.dto.StockDocumentDto.Line> lines = new java.util.ArrayList<>();
            for (com.smartretail.inventoryservice.dto.StocktakingDetailDto item : importItems) {
                int systemQty = item.getSystemQuantity() != null ? item.getSystemQuantity() : 0;
                int actualQty = item.getActualQuantity() != null ? item.getActualQuantity() : 0;
                int diff = actualQty - systemQty;

                com.smartretail.inventoryservice.dto.StockDocumentDto.Line line = new com.smartretail.inventoryservice.dto.StockDocumentDto.Line();
                line.productUnitId = item.getProductUnitId();
                line.quantity = diff;
                lines.add(line);
            }

            // Thêm tất cả sản phẩm vào phiếu nhập
            stockDocumentService.addLinesBulk(createdDoc.id, lines);

            // Duyệt phiếu nhập để tạo inventory transactions
            try {
                log.info("Attempting to approve inbound document ID: {}", createdDoc.id);
                stockDocumentService.approve(createdDoc.id);
                log.info("Successfully approved inbound document ID: {}", createdDoc.id);
            } catch (Exception e) {
                log.error("Failed to approve inbound document ID {}: {}", createdDoc.id, e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to approve inbound document: " + e.getMessage(), e);
            }

            log.info("Successfully created and approved inbound document ID: {}", createdDoc.id);
        } catch (Exception e) {
            log.error("Failed to create inbound document for stocktaking: {}", e.getMessage());
            throw e;
        }
    }

    // Tạo phiếu XUẤT cho các sản phẩm thiếu
    private void createOutboundDocumentForAdjustments(Stocktaking st, List<com.smartretail.inventoryservice.dto.StocktakingDetailDto> exportItems) {
        try {
            log.info("Creating outbound document for {} export items", exportItems.size());

            // Tạo phiếu xuất
            com.smartretail.inventoryservice.dto.StockDocumentDto outboundDoc = new com.smartretail.inventoryservice.dto.StockDocumentDto();
            outboundDoc.type = "OUTBOUND";
            outboundDoc.warehouseId = st.getWarehouse().getId();
            outboundDoc.stockLocationId = st.getStockLocation().getId();
            outboundDoc.referenceNumber = st.getStocktakingNumber() + "-OUT";
            outboundDoc.note = "Phiếu xuất điều chỉnh từ kiểm kê: " + st.getStocktakingNumber();

            com.smartretail.inventoryservice.dto.StockDocumentDto createdDoc = stockDocumentService.create(outboundDoc);
            log.info("Created outbound document ID: {}", createdDoc.id);

            // Thêm sản phẩm vào phiếu xuất
            List<com.smartretail.inventoryservice.dto.StockDocumentDto.Line> lines = new java.util.ArrayList<>();
            for (com.smartretail.inventoryservice.dto.StocktakingDetailDto item : exportItems) {
                int systemQty = item.getSystemQuantity() != null ? item.getSystemQuantity() : 0;
                int actualQty = item.getActualQuantity() != null ? item.getActualQuantity() : 0;
                int diff = Math.abs(actualQty - systemQty);

                com.smartretail.inventoryservice.dto.StockDocumentDto.Line line = new com.smartretail.inventoryservice.dto.StockDocumentDto.Line();
                line.productUnitId = item.getProductUnitId();
                line.quantity = diff;
                lines.add(line);
            }

            // Thêm tất cả sản phẩm vào phiếu xuất
            stockDocumentService.addLinesBulk(createdDoc.id, lines);

            // Duyệt phiếu xuất để tạo inventory transactions
            try {
                log.info("Attempting to approve outbound document ID: {}", createdDoc.id);
                stockDocumentService.approve(createdDoc.id);
                log.info("Successfully approved outbound document ID: {}", createdDoc.id);
            } catch (Exception e) {
                log.error("Failed to approve outbound document ID {}: {}", createdDoc.id, e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to approve outbound document: " + e.getMessage(), e);
            }

            log.info("Successfully created and approved outbound document ID: {}", createdDoc.id);
        } catch (Exception e) {
            log.error("Failed to create outbound document for stocktaking: {}", e.getMessage());
            throw e;
        }
    }

    // Danh sách phiếu kiểm kê (lọc tùy chọn)
    public List<Stocktaking> listStocktakings(Long warehouseId, Long stockLocationId, String status) {
        List<Stocktaking> all = stocktakingRepository.findAll();
        return all.stream()
                .filter(st -> warehouseId == null || (st.getWarehouse() != null && st.getWarehouse().getId().equals(warehouseId)))
                .filter(st -> stockLocationId == null || (st.getStockLocation() != null && st.getStockLocation().getId().equals(stockLocationId)))
                .filter(st -> {
                    if (status == null) return true;
                    try {
                        return st.getStatus() == Stocktaking.StocktakingStatus.valueOf(status.toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        return true;
                    }
                })
                .collect(Collectors.toList());
    }

    // Lấy phiếu kiểm kê theo ID
    public Stocktaking getStocktakingById(Long id) {
        return stocktakingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stocktaking not found with id: " + id));
    }

    // Lấy danh sách chi tiết của phiếu
    public List<StocktakingDetail> getDetailsByStocktakingId(Long id) {
        Stocktaking st = getStocktakingById(id);
        return stocktakingDetailRepository.findByStocktaking(st);
    }

    // Map Detail Entity -> DTO
    public com.smartretail.inventoryservice.dto.StocktakingDetailDto toDetailDto(StocktakingDetail d) {
        com.smartretail.inventoryservice.dto.StocktakingDetailDto dto = new com.smartretail.inventoryservice.dto.StocktakingDetailDto();
        dto.setId(d.getId());
        dto.setProductUnitId(d.getProductUnitId());
        dto.setSystemQuantity(d.getSystemQuantity());
        dto.setActualQuantity(d.getActualQuantity());
        dto.setDifferenceQuantity(d.getDifferenceQuantity());
        dto.setNote(d.getNote());
        try {
            var pu = productServiceClient.getProductUnitById(d.getProductUnitId());
            if (pu != null) {
                // Ưu tiên lấy trực tiếp nếu response có sẵn tên
                if (pu.getUnitName() != null) {
                    dto.setUnitName(pu.getUnitName());
                } else if (pu.getUnitId() != null) {
                    var unit = productServiceClient.getUnitById(pu.getUnitId());
                    if (unit != null) {
                        dto.setUnitName(unit.getName());
                    }
                }

                if (pu.getProductName() != null) {
                    dto.setProductName(pu.getProductName());
                } else if (pu.getProductId() != null) {
                    var product = productServiceClient.getProductById(pu.getProductId());
                    if (product != null) {
                        dto.setProductName(product.getName());
                    }
                }
            }
        } catch (Exception ignore) { }
        return dto;
    }

    public java.util.List<com.smartretail.inventoryservice.dto.StocktakingDetailDto> toDetailDtoList(java.util.List<StocktakingDetail> items) {
        return items.stream().map(this::toDetailDto).collect(java.util.stream.Collectors.toList());
    }

    // Xóa phiếu kiểm kê (chỉ cho phép khi chưa CONFIRMED)
    public void deleteStocktaking(Long id) {
        Stocktaking st = getStocktakingById(id);
        if (st.getStatus() == Stocktaking.StocktakingStatus.CONFIRMED) {
            throw new RuntimeException("Cannot delete a confirmed stocktaking");
        }
        // Xóa chi tiết trước
        List<StocktakingDetail> details = stocktakingDetailRepository.findByStocktaking(st);
        if (!details.isEmpty()) {
            stocktakingDetailRepository.deleteAll(details);
        }
        stocktakingRepository.delete(st);
    }

    // Mapping Entity -> DTO để tránh lazy proxy khi serialize
    public com.smartretail.inventoryservice.dto.StocktakingDto toDto(Stocktaking st) {
        com.smartretail.inventoryservice.dto.StocktakingDto dto = new com.smartretail.inventoryservice.dto.StocktakingDto();
        dto.setId(st.getId());
        dto.setStocktakingNumber(st.getStocktakingNumber());
        dto.setWarehouseId(st.getWarehouse() != null ? st.getWarehouse().getId() : null);
        dto.setStockLocationId(st.getStockLocation() != null ? st.getStockLocation().getId() : null);
        dto.setStatus(st.getStatus() != null ? st.getStatus().name() : null);
        dto.setStocktakingDate(st.getStocktakingDate());
        dto.setCompletedDate(st.getCompletedDate());
        dto.setNote(st.getNote());
        dto.setCreatedBy(st.getCreatedBy());
        dto.setCreatedByUsername(st.getCreatedByUsername());
        dto.setCreatedAt(st.getCreatedAt());
        dto.setUpdatedAt(st.getUpdatedAt());
        return dto;
    }

    public java.util.List<com.smartretail.inventoryservice.dto.StocktakingDto> toDtoList(java.util.List<Stocktaking> items) {
        return items.stream().map(this::toDto).collect(java.util.stream.Collectors.toList());
    }

    // Hoàn tất phiếu kiểm kê - chuyển từ CONFIRMED sang COMPLETED
    @Transactional
    public com.smartretail.inventoryservice.dto.StocktakingDto complete(Long stocktakingId) {
        Stocktaking st = stocktakingRepository.findById(stocktakingId)
                .orElseThrow(() -> new RuntimeException("Stocktaking not found with id: " + stocktakingId));

        if (st.getStatus() != Stocktaking.StocktakingStatus.CONFIRMED) {
            throw new RuntimeException("Only CONFIRMED stocktaking can be completed");
        }

        st.setStatus(Stocktaking.StocktakingStatus.COMPLETED);
        st.setCompletedDate(LocalDateTime.now());
        Stocktaking saved = stocktakingRepository.save(st);

        return toDto(saved);
    }

    // Hủy phiếu kiểm kê - chuyển sang CANCELLED
    @Transactional
    public com.smartretail.inventoryservice.dto.StocktakingDto cancel(Long stocktakingId, String reason) {
        Stocktaking st = stocktakingRepository.findById(stocktakingId)
                .orElseThrow(() -> new RuntimeException("Stocktaking not found with id: " + stocktakingId));

        if (st.getStatus() == Stocktaking.StocktakingStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel a completed stocktaking");
        }

        if (st.getStatus() == Stocktaking.StocktakingStatus.CANCELLED) {
            throw new RuntimeException("Stocktaking is already cancelled");
        }

        st.setStatus(Stocktaking.StocktakingStatus.CANCELLED);

        // Thêm lý do hủy vào note
        String currentNote = st.getNote();
        String cancelReason = reason != null && !reason.trim().isEmpty() ? reason.trim() : "No reason provided";
        String newNote = (currentNote != null && !currentNote.isEmpty())
            ? currentNote + " | " + cancelReason
            : " " + cancelReason;
        st.setNote(newNote);

        Stocktaking saved = stocktakingRepository.save(st);

        return toDto(saved);
    }
}



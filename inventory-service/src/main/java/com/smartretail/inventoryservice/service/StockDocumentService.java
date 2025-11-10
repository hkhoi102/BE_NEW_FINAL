package com.smartretail.inventoryservice.service;

import com.smartretail.inventoryservice.dto.InventoryDto;
import com.smartretail.inventoryservice.dto.StockDocumentDto;
import com.smartretail.inventoryservice.model.StockDocument;
import com.smartretail.inventoryservice.model.StockDocumentLine;
import com.smartretail.inventoryservice.repository.StockDocumentRepository;
import com.smartretail.inventoryservice.repository.StockDocumentLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockDocumentService {

    private final StockDocumentRepository stockDocumentRepository;
    private final InventoryService inventoryService;
    private final LotManagementService lotManagementService;
    private final StockDocumentLineRepository stockDocumentLineRepository;
    private final StockReservationService stockReservationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public StockDocumentDto create(StockDocumentDto dto) {
        StockDocument doc = new StockDocument();
        doc.setType(StockDocument.DocumentType.valueOf(dto.type));
        doc.setStatus(StockDocument.DocumentStatus.DRAFT);
        doc.setWarehouseId(dto.warehouseId);
        doc.setStockLocationId(dto.stockLocationId);
        doc.setReferenceNumber(dto.referenceNumber);
        doc.setNote(dto.note);

        StockDocument saved = stockDocumentRepository.save(doc);
        return toDto(saved);
    }

    @Transactional
    public StockDocumentDto addLine(Long documentId, StockDocumentDto.Line lineDto) {
        StockDocument doc = stockDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Stock document not found: " + documentId));
        if (doc.getStatus() != StockDocument.DocumentStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT document can be edited");
        }
        // Validate lot number early for inbound documents
        if (doc.getType() == StockDocument.DocumentType.INBOUND && lineDto.lotNumber != null) {
            lotManagementService.validateLotNumberForInboundDraft(
                    lineDto.productUnitId,
                    doc.getWarehouseId(),
                    doc.getStockLocationId(),
                    lineDto.lotNumber);
        }
        // Validate outbound availability early for outbound documents
        if (doc.getType() == StockDocument.DocumentType.OUTBOUND) {
            // Kiểm tra available_quantity trước khi reserve và lấy thông tin chi tiết
            if (!stockReservationService.checkAvailableQuantity(
                    lineDto.productUnitId,
                    doc.getWarehouseId(),
                    doc.getStockLocationId(),
                    lineDto.quantity)) {
                // Lấy thông tin chi tiết về số lượng còn lại
                StockReservationService.AvailableQuantityInfo quantityInfo =
                    stockReservationService.getAvailableQuantityInfo(
                        lineDto.productUnitId,
                        doc.getWarehouseId(),
                        doc.getStockLocationId()
                    );

                throw new RuntimeException("Số sản phẩm yêu cầu vượt quá số lượng trong kho. " +
                    "Số lượng yêu cầu: " + lineDto.quantity +
                    ", Số lượng trong kho còn: " + quantityInfo.getAvailableFromLots() +
                    " (ProductUnitId: " + lineDto.productUnitId + ")");
            }
        }
        StockDocumentLine line = new StockDocumentLine();
        line.setDocument(doc);
        line.setProductUnitId(lineDto.productUnitId);
        line.setQuantity(lineDto.quantity);
        // Lot fields (optional)
        line.setLotNumber(lineDto.lotNumber);
        line.setExpiryDate(lineDto.expiryDate);
        line.setManufacturingDate(lineDto.manufacturingDate);
        line.setSupplierName(lineDto.supplierName);
        line.setSupplierBatchNumber(lineDto.supplierBatchNumber);

        // Reserve stock cho OUTBOUND documents
        if (doc.getType() == StockDocument.DocumentType.OUTBOUND) {
            try {
                StockReservationService.ReserveResult reserveResult = stockReservationService.reserveStock(
                        lineDto.productUnitId,
                        doc.getWarehouseId(),
                        doc.getStockLocationId(),
                        lineDto.quantity);

                // Lưu thông tin reservation vào line
                String reservedLotInfoJson = objectMapper.writeValueAsString(reserveResult.getLotReservations());
                line.setReservedLotInfo(reservedLotInfoJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize reservation info", e);
            }
        }

        stockDocumentLineRepository.save(line);
        return toDto(doc);
    }

    public List<StockDocumentDto.Line> getLines(Long documentId) {
        return stockDocumentLineRepository.findByDocument_Id(documentId).stream().map(l -> {
            StockDocumentDto.Line d = new StockDocumentDto.Line();
            d.id = l.getId();
            d.productUnitId = l.getProductUnitId();
            d.quantity = l.getQuantity();
            return d;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void deleteLine(Long lineId) {
        stockDocumentLineRepository.deleteById(lineId);
    }

    @Transactional
    public StockDocumentDto addLinesBulk(Long documentId, List<StockDocumentDto.Line> lines) {
        StockDocument doc = stockDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Stock document not found: " + documentId));
        if (doc.getStatus() != StockDocument.DocumentStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT document can be edited");
        }
        if (lines != null) {
            // Validate all lines first to fail-fast
            for (StockDocumentDto.Line l : lines) {
                if (doc.getType() == StockDocument.DocumentType.INBOUND && l.lotNumber != null) {
                    lotManagementService.validateLotNumberForInboundDraft(
                            l.productUnitId,
                            doc.getWarehouseId(),
                            doc.getStockLocationId(),
                            l.lotNumber);
                }
                if (doc.getType() == StockDocument.DocumentType.OUTBOUND) {
                    // Kiểm tra available_quantity trước khi reserve và lấy thông tin chi tiết
                    if (!stockReservationService.checkAvailableQuantity(
                            l.productUnitId,
                            doc.getWarehouseId(),
                            doc.getStockLocationId(),
                            l.quantity)) {
                        // Lấy thông tin chi tiết về số lượng còn lại
                        StockReservationService.AvailableQuantityInfo quantityInfo =
                            stockReservationService.getAvailableQuantityInfo(
                                l.productUnitId,
                                doc.getWarehouseId(),
                                doc.getStockLocationId()
                            );

                        throw new RuntimeException("Số sản phẩm yêu cầu vượt quá số lượng trong kho. " +
                            "Số lượng yêu cầu: " + l.quantity +
                            ", Số lượng trong kho còn: " + quantityInfo.getAvailableFromLots() +
                            " (ProductUnitId: " + l.productUnitId + ")");
                    }
                }
            }

            // Process all lines
            for (StockDocumentDto.Line l : lines) {
                StockDocumentLine line = new StockDocumentLine();
                line.setDocument(doc);
                line.setProductUnitId(l.productUnitId);
                line.setQuantity(l.quantity);
                line.setLotNumber(l.lotNumber);
                line.setExpiryDate(l.expiryDate);
                line.setManufacturingDate(l.manufacturingDate);
                line.setSupplierName(l.supplierName);
                line.setSupplierBatchNumber(l.supplierBatchNumber);

                // Reserve stock cho OUTBOUND documents
                if (doc.getType() == StockDocument.DocumentType.OUTBOUND) {
                    try {
                        StockReservationService.ReserveResult reserveResult = stockReservationService.reserveStock(
                                l.productUnitId,
                                doc.getWarehouseId(),
                                doc.getStockLocationId(),
                                l.quantity);

                        // Lưu thông tin reservation vào line
                        String reservedLotInfoJson = objectMapper.writeValueAsString(reserveResult.getLotReservations());
                        line.setReservedLotInfo(reservedLotInfoJson);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to serialize reservation info for product " + l.productUnitId, e);
                    }
                }

                stockDocumentLineRepository.save(line);
            }
        }
        return toDto(doc);
    }

    @Transactional
    public StockDocumentDto approve(Long id) {
        log.info("Starting approval process for document ID: {}", id);

        StockDocument doc = stockDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock document not found: " + id));

        // Force load lines to avoid lazy loading issues
        List<StockDocumentLine> lines = stockDocumentLineRepository.findByDocument_Id(id);

        log.info("Found document: Type={}, Status={}, Lines={}",
                doc.getType(), doc.getStatus(), lines.size());

        if (doc.getStatus() != StockDocument.DocumentStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT document can be approved");
        }

        // Generate inventory transactions for each line
        log.info("Processing {} lines for document {}", lines.size(), id);
        for (StockDocumentLine line : lines) {
            log.info("Processing line: ID={}, ProductUnitId={}, Quantity={}",
                    line.getId(), line.getProductUnitId(), line.getQuantity());
            if (doc.getType() == StockDocument.DocumentType.INBOUND) {
                try {
                    log.info("Processing INBOUND document line: ProductUnitId={}, Quantity={}",
                            line.getProductUnitId(), line.getQuantity());

                    InventoryDto tx = new InventoryDto();
                    tx.setProductUnitId(line.getProductUnitId());
                    tx.setQuantity(line.getQuantity());
                    tx.setWarehouseId(doc.getWarehouseId());
                    tx.setStockLocationId(doc.getStockLocationId());
                    tx.setTransactionDate(LocalDateTime.now());
                    tx.setNote("From document " + doc.getId());
                    tx.setReferenceNumber(doc.getReferenceNumber());

                    // Map lot fields from line so inbound creates/merges lot
                    tx.setLotNumber(line.getLotNumber());
                    tx.setExpiryDate(line.getExpiryDate());
                    tx.setManufacturingDate(line.getManufacturingDate());
                    tx.setSupplierName(line.getSupplierName());
                    tx.setSupplierBatchNumber(line.getSupplierBatchNumber());

                    // For stocktaking adjustments, create lot if not provided
                    if (tx.getLotNumber() == null || tx.getLotNumber().trim().isEmpty()) {
                        tx.setLotNumber("STOCKTAKING-" + doc.getReferenceNumber() + "-" + line.getProductUnitId());
                    }
                    if (tx.getExpiryDate() == null) {
                        // Set expiry date to 1 year from now for stocktaking adjustments
                        tx.setExpiryDate(LocalDateTime.now().plusYears(1).toLocalDate());
                    }
                    if (tx.getManufacturingDate() == null) {
                        tx.setManufacturingDate(LocalDateTime.now().toLocalDate());
                    }
                    if (tx.getSupplierName() == null || tx.getSupplierName().trim().isEmpty()) {
                        tx.setSupplierName("Stocktaking Adjustment");
                    }

                    log.info("Lot fields - LotNumber: {}, ExpiryDate: {}, ManufacturingDate: {}, SupplierName: {}",
                            tx.getLotNumber(), tx.getExpiryDate(), tx.getManufacturingDate(), tx.getSupplierName());

                    log.info("Calling processInboundInventory for product {}", line.getProductUnitId());
                    inventoryService.processInboundInventory(tx);
                    log.info("Successfully processed inbound inventory for product {}", line.getProductUnitId());
                } catch (Exception e) {
                    log.error("Failed to process inbound inventory for line {}: {}", line.getId(), e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            } else if (doc.getType() == StockDocument.DocumentType.OUTBOUND) {
                // Consume reserved stock cho OUTBOUND documents
                try {
                    log.info("Processing OUTBOUND document line: ProductUnitId={}, Quantity={}",
                            line.getProductUnitId(), line.getQuantity());

                    log.info("ReservedLotInfo for line {}: {}", line.getId(), line.getReservedLotInfo());

                    if (line.getReservedLotInfo() == null || line.getReservedLotInfo().trim().isEmpty()) {
                        throw new RuntimeException("ReservedLotInfo is null or empty for line " + line.getId() +
                                ". This means stock was not properly reserved when adding lines to document.");
                    }

                    List<StockReservationService.LotReservation> lotReservations =
                            objectMapper.readValue(line.getReservedLotInfo(),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, StockReservationService.LotReservation.class));

                    log.info("Found {} lot reservations for product {}",
                            lotReservations.size(), line.getProductUnitId());

                    stockReservationService.consumeReservedStock(
                            line.getProductUnitId(),
                            doc.getWarehouseId(),
                            doc.getStockLocationId(),
                            line.getQuantity(),
                            lotReservations);

                    log.info("Successfully consumed {} units for product {}",
                            line.getQuantity(), line.getProductUnitId());
                } catch (JsonProcessingException e) {
                    log.error("Failed to deserialize reservation info for line {}: {}", line.getId(), e.getMessage());
                    throw new RuntimeException("Failed to deserialize reservation info for line " + line.getId(), e);
                } catch (Exception e) {
                    log.error("Failed to consume reserved stock for line {}: {}", line.getId(), e.getMessage());
                    throw e;
                }
            }
        }

        doc.setStatus(StockDocument.DocumentStatus.APPROVED);
        doc.setApprovedAt(LocalDateTime.now());
        StockDocument saved = stockDocumentRepository.save(doc);

        log.info("Successfully approved document ID: {}, Status: {}", saved.getId(), saved.getStatus());

        return toDto(saved);
    }

    @Transactional
    public StockDocumentDto reject(Long id, String reason) {
        StockDocument doc = stockDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock document not found: " + id));
        if (doc.getStatus() != StockDocument.DocumentStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT document can be rejected");
        }
        doc.setStatus(StockDocument.DocumentStatus.CANCELLED);
        String note = doc.getNote();
        if (reason != null && !reason.trim().isEmpty()) {
            doc.setNote((note != null && !note.isEmpty() ? note + " | " : "") + "Rejected: " + reason.trim());
        }
        StockDocument saved = stockDocumentRepository.save(doc);
        return toDto(saved);
    }

    public List<StockDocumentDto> listByWarehouse(Long warehouseId) {
        return stockDocumentRepository.findByWarehouseIdOrderByCreatedAtDesc(warehouseId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<StockDocumentDto> listAll() {
        return stockDocumentRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public StockDocumentDto getById(Long id) {
        StockDocument doc = stockDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock document not found: " + id));
        return toDto(doc);
    }

    @Transactional
    public StockDocumentDto cancel(Long id) {
        StockDocument doc = stockDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock document not found: " + id));

        if (doc.getStatus() != StockDocument.DocumentStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT document can be cancelled");
        }

        // Release reservations cho OUTBOUND documents
        if (doc.getType() == StockDocument.DocumentType.OUTBOUND) {
            for (StockDocumentLine line : doc.getLines()) {
                if (line.getReservedLotInfo() != null) {
                    try {
                        List<StockReservationService.LotReservation> lotReservations =
                                objectMapper.readValue(line.getReservedLotInfo(),
                                        objectMapper.getTypeFactory().constructCollectionType(List.class, StockReservationService.LotReservation.class));

                        stockReservationService.releaseReservation(
                                line.getProductUnitId(),
                                doc.getWarehouseId(),
                                doc.getStockLocationId(),
                                line.getQuantity(),
                                lotReservations);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to deserialize reservation info for line " + line.getId(), e);
                    }
                }
            }
        }

        doc.setStatus(StockDocument.DocumentStatus.CANCELLED);
        StockDocument saved = stockDocumentRepository.save(doc);
        return toDto(saved);
    }

    @Transactional
    public StockDocumentDto updateLine(Long lineId, StockDocumentDto.Line lineDto) {
        StockDocumentLine line = stockDocumentLineRepository.findById(lineId)
                .orElseThrow(() -> new RuntimeException("Stock document line not found: " + lineId));

        StockDocument doc = line.getDocument();
        if (doc.getStatus() != StockDocument.DocumentStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT document can be edited");
        }

        if (lineDto.productUnitId != null) {
            line.setProductUnitId(lineDto.productUnitId);
        }
        if (lineDto.quantity != null) {
            if (lineDto.quantity < 0) throw new RuntimeException("quantity must be >= 0");
            line.setQuantity(lineDto.quantity);
        }

        stockDocumentLineRepository.save(line);
        return toDto(doc);
    }

    private StockDocumentDto toDto(StockDocument doc) {
        StockDocumentDto dto = new StockDocumentDto();
        dto.id = doc.getId();
        dto.type = doc.getType() != null ? doc.getType().name() : null;
        dto.status = doc.getStatus() != null ? doc.getStatus().name() : null;
        dto.warehouseId = doc.getWarehouseId();
        dto.stockLocationId = doc.getStockLocationId();
        dto.referenceNumber = doc.getReferenceNumber();
        dto.note = doc.getNote();
        dto.createdAt = doc.getCreatedAt();
        dto.approvedAt = doc.getApprovedAt();
        if (doc.getLines() != null) {
            dto.lines = doc.getLines().stream().map(l -> {
                StockDocumentDto.Line d = new StockDocumentDto.Line();
                d.id = l.getId();
                d.productUnitId = l.getProductUnitId();
                d.quantity = l.getQuantity();
                return d;
            }).collect(Collectors.toList());
        }
        return dto;
    }
}



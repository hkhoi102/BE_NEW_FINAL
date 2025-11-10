package com.smartretail.inventoryservice.controller;

import com.smartretail.inventoryservice.dto.StockDocumentDto;
import com.smartretail.inventoryservice.service.StockDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/documents")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StockDocumentController {

    private final StockDocumentService stockDocumentService;

    // Tạo phiếu (nhập/xuất)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody StockDocumentDto dto) {
        StockDocumentDto created = stockDocumentService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Document created",
                "data", created
        ));
    }

    // Lấy chi tiết 1 phiếu (kèm lines)
    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        StockDocumentDto dto = stockDocumentService.getById(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", dto
        ));
    }

    // Sửa 1 dòng của phiếu (sản phẩm/quantity)
    @PutMapping("/lines/{lineId}")
    public ResponseEntity<?> updateLine(@PathVariable Long lineId, @RequestBody StockDocumentDto.Line line) {
        try {
            StockDocumentDto dto = stockDocumentService.updateLine(lineId, line);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", dto
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    // Thêm dòng vào phiếu (sau khi tạo header)
    @PostMapping("/{id}/lines")
    public ResponseEntity<?> addLine(@PathVariable Long id, @RequestBody StockDocumentDto.Line line) {
        StockDocumentDto updated = stockDocumentService.addLine(id, line);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Line added",
                "data", updated
        ));
    }

    // Lấy các dòng của phiếu
    @GetMapping("/{id}/lines")
    public ResponseEntity<?> getLines(@PathVariable Long id) {
        var lines = stockDocumentService.getLines(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "total", lines.size(),
                "data", lines
        ));
    }

    // Thêm nhiều dòng cùng lúc
    @PostMapping("/{id}/lines/bulk")
    public ResponseEntity<?> addLinesBulk(@PathVariable Long id, @RequestBody Map<String, java.util.List<StockDocumentDto.Line>> payload) {
        var lines = payload.get("lines");
        StockDocumentDto updated = stockDocumentService.addLinesBulk(id, lines);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Lines added",
                "data", updated
        ));
    }

    // Xóa một dòng khỏi phiếu
    @DeleteMapping("/lines/{lineId}")
    public ResponseEntity<?> deleteLine(@PathVariable Long lineId) {
        stockDocumentService.deleteLine(lineId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Line deleted"
        ));
    }

    // Duyệt phiếu -> phát sinh giao dịch kho
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        StockDocumentDto approved = stockDocumentService.approve(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document approved",
                "data", approved
        ));
    }

    // Từ chối phiếu (đặt trạng thái CANCELLED, không phát sinh giao dịch)
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        StockDocumentDto rejected = stockDocumentService.reject(id, reason);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document rejected",
                "data", rejected
        ));
    }

    // Hủy phiếu (đặt trạng thái CANCELLED, giải phóng reservation)
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        try {
            StockDocumentDto cancelled = stockDocumentService.cancel(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Document cancelled successfully",
                    "data", cancelled
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // Danh sách phiếu theo kho
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) Long warehouseId) {
        List<StockDocumentDto> list = (warehouseId != null)
                ? stockDocumentService.listByWarehouse(warehouseId)
                : stockDocumentService.listAll();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "total", list.size(),
                "data", list
        ));
    }
}



package com.smartretail.promotionservice.controller;

import com.smartretail.promotionservice.dto.PromotionHeaderDto;
import com.smartretail.promotionservice.dto.PromotionLineDto;
import com.smartretail.promotionservice.dto.PromotionDetailDto;
import com.smartretail.promotionservice.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/promotions")
@CrossOrigin(origins = "*")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    // ==================== PROMOTION HEADER ====================

    /**
     * Tạo mới chương trình khuyến mãi
     */
    @PostMapping("/headers")
    public ResponseEntity<PromotionHeaderDto> createPromotionHeader(@RequestBody PromotionHeaderDto dto) {
        try {
            PromotionHeaderDto created = promotionService.createPromotionHeader(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Cập nhật chương trình khuyến mãi
     */
    @PutMapping("/headers/{id}")
    public ResponseEntity<PromotionHeaderDto> updatePromotionHeader(@PathVariable Long id, @RequestBody PromotionHeaderDto dto) {
        try {
            Optional<PromotionHeaderDto> updated = promotionService.updatePromotionHeader(id, dto);
            return updated.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Kích hoạt khuyến mãi
     */
    @PutMapping("/headers/{id}/activate")
    public ResponseEntity<String> activatePromotionHeader(@PathVariable Long id) {
        try {
            boolean success = promotionService.activatePromotionHeader(id);
            if (success) {
                return ResponseEntity.ok("Khuyến mãi đã được kích hoạt thành công");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Tắt khuyến mãi
     */
    @PutMapping("/headers/{id}/deactivate")
    public ResponseEntity<String> deactivatePromotionHeader(@PathVariable Long id) {
        try {
            boolean success = promotionService.deactivatePromotionHeader(id);
            if (success) {
                return ResponseEntity.ok("Khuyến mãi đã được tắt thành công");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Xóa chương trình khuyến mãi (soft delete)
     */
    @DeleteMapping("/headers/{id}")
    public ResponseEntity<String> deletePromotionHeader(@PathVariable Long id) {
        try {
            boolean success = promotionService.deletePromotionHeader(id);
            if (success) {
                return ResponseEntity.ok("Khuyến mãi đã được xóa thành công");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy chương trình khuyến mãi theo ID
     */
    @GetMapping("/headers/{id}")
    public ResponseEntity<PromotionHeaderDto> getPromotionHeaderById(@PathVariable Long id) {
        Optional<PromotionHeaderDto> header = promotionService.getPromotionHeaderById(id);
        return header.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lấy tất cả chương trình khuyến mãi đang hoạt động
     */
    @GetMapping("/headers/active")
    public ResponseEntity<List<PromotionHeaderDto>> getActivePromotionHeaders() {
        List<PromotionHeaderDto> headers = promotionService.getAllActivePromotionHeaders();
        return ResponseEntity.ok(headers);
    }

    /**
     * Lấy tất cả chương trình khuyến mãi (cả active và inactive)
     */
    @GetMapping("/headers")
    public ResponseEntity<List<PromotionHeaderDto>> getAllPromotionHeaders() {
        List<PromotionHeaderDto> headers = promotionService.getAllPromotionHeaders();
        return ResponseEntity.ok(headers);
    }

    /**
     * Lấy chương trình khuyến mãi đang hiệu lực theo ngày
     */
    @GetMapping("/headers/active/date")
    public ResponseEntity<List<PromotionHeaderDto>> getActivePromotionsByDate(@RequestParam String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            List<PromotionHeaderDto> headers = promotionService.getActivePromotionsByDate(localDate);
            return ResponseEntity.ok(headers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Tìm kiếm chương trình khuyến mãi theo tên
     */
    @GetMapping("/headers/search")
    public ResponseEntity<List<PromotionHeaderDto>> searchPromotionHeadersByName(@RequestParam String name) {
        List<PromotionHeaderDto> headers = promotionService.searchPromotionHeadersByName(name);
        return ResponseEntity.ok(headers);
    }

    // ==================== PROMOTION LINE ====================

    /**
     * Tạo promotion line mới
     */
    @PostMapping("/lines")
    public ResponseEntity<PromotionLineDto> createPromotionLine(@RequestBody PromotionLineDto dto) {
        try {
            PromotionLineDto created = promotionService.createPromotionLine(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Cập nhật promotion line
    @PutMapping("/lines/{id}")
    public ResponseEntity<?> updatePromotionLine(@PathVariable Long id, @RequestBody PromotionLineDto dto) {
        try {
            Optional<PromotionLineDto> updated = promotionService.updatePromotionLine(id, dto);
            if (updated.isPresent()) {
                return ResponseEntity.ok(java.util.Map.of("success", true, "data", updated.get()));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of(
                    "success", false,
                    "message", "Promotion line not found: " + id
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Kích hoạt promotion line
     */
    @PutMapping("/lines/{id}/activate")
    public ResponseEntity<String> activatePromotionLine(@PathVariable Long id) {
        try {
            boolean success = promotionService.activatePromotionLine(id);
            if (success) {
                return ResponseEntity.ok("Promotion line đã được kích hoạt thành công");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Tắt promotion line
     */
    @PutMapping("/lines/{id}/deactivate")
    public ResponseEntity<String> deactivatePromotionLine(@PathVariable Long id) {
        try {
            boolean success = promotionService.deactivatePromotionLine(id);
            if (success) {
                return ResponseEntity.ok("Promotion line đã được tắt thành công");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy tất cả promotion lines của một promotion header (chỉ active)
     */
    @GetMapping("/headers/{headerId}/lines")
    public ResponseEntity<List<PromotionLineDto>> getPromotionLinesByHeaderId(@PathVariable Long headerId) {
        List<PromotionLineDto> lines = promotionService.getPromotionLinesByHeaderId(headerId);
        return ResponseEntity.ok(lines);
    }

    /**
     * Lấy tất cả promotion lines của một promotion header (cả active và inactive)
     */
    @GetMapping("/headers/{headerId}/lines/all")
    public ResponseEntity<List<PromotionLineDto>> getAllPromotionLinesByHeaderId(@PathVariable Long headerId) {
        List<PromotionLineDto> lines = promotionService.getAllPromotionLinesByHeaderId(headerId);
        return ResponseEntity.ok(lines);
    }

    // ==================== PROMOTION DETAIL ====================

    /**
     * Tạo mới promotion detail
     */
    @PostMapping("/details")
    public ResponseEntity<PromotionDetailDto> createPromotionDetail(@RequestBody PromotionDetailDto dto) {
        try {
            PromotionDetailDto created = promotionService.createPromotionDetail(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Cập nhật promotion detail
    @PutMapping("/details/{id}")
    public ResponseEntity<?> updatePromotionDetail(@PathVariable Long id, @RequestBody PromotionDetailDto dto) {
        try {
            Optional<PromotionDetailDto> updated = promotionService.updatePromotionDetail(id, dto);
            if (updated.isPresent()) {
                return ResponseEntity.ok(java.util.Map.of("success", true, "data", updated.get()));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of(
                    "success", false,
                    "message", "Promotion detail not found: " + id
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Kích hoạt promotion detail
     */
    @PutMapping("/details/{id}/activate")
    public ResponseEntity<String> activatePromotionDetail(@PathVariable Long id) {
        try {
            boolean success = promotionService.activatePromotionDetail(id);
            if (success) {
                return ResponseEntity.ok("Promotion detail đã được kích hoạt thành công");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Tắt promotion detail
     */
    @PutMapping("/details/{id}/deactivate")
    public ResponseEntity<String> deactivatePromotionDetail(@PathVariable Long id) {
        try {
            boolean success = promotionService.deactivatePromotionDetail(id);
            if (success) {
                return ResponseEntity.ok("Promotion detail đã được tắt thành công");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Lấy tất cả promotion details của một promotion line (chỉ active)
     */
    @GetMapping("/lines/{lineId}/details")
    public ResponseEntity<List<PromotionDetailDto>> getPromotionDetailsByLineId(@PathVariable Long lineId) {
        List<PromotionDetailDto> details = promotionService.getPromotionDetailsByLineId(lineId);
        return ResponseEntity.ok(details);
    }

    /**
     * Lấy tất cả promotion details của một promotion line (cả active và inactive)
     */
    @GetMapping("/lines/{lineId}/details/all")
    public ResponseEntity<List<PromotionDetailDto>> getAllPromotionDetailsByLineId(@PathVariable Long lineId) {
        List<PromotionDetailDto> details = promotionService.getAllPromotionDetailsByLineId(lineId);
        return ResponseEntity.ok(details);
    }

    // ==================== HEALTH CHECK ====================

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Promotion Service is running!");
    }
}

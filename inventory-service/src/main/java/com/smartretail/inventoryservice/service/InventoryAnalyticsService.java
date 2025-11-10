package com.smartretail.inventoryservice.service;

import com.smartretail.inventoryservice.client.ProductServiceClient;
import com.smartretail.inventoryservice.dto.LowStockAlertDto;
import com.smartretail.inventoryservice.dto.ProductStockSummaryDto;
import com.smartretail.inventoryservice.repository.StockBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryAnalyticsService {

    private final StockBalanceRepository stockBalanceRepository;
    private final ProductServiceClient productServiceClient;

    public List<ProductStockSummaryDto> getStockSummaryByProduct(Long warehouseId, Long stockLocationId) {
        return stockBalanceRepository
                .getProductStockSummaries(warehouseId, stockLocationId)
                .stream()
                .map(this::enrichProductStockSummary)
                .collect(Collectors.toList());
    }

    public List<LowStockAlertDto> getLowStockAlerts(Integer threshold, Long warehouseId, Long stockLocationId) {
        int effective = threshold != null ? threshold : 0;
        return stockBalanceRepository
                .getLowStockProducts(warehouseId, stockLocationId, effective)
                .stream()
                .map(a -> enrichLowStockAlert(a, effective))
                .collect(Collectors.toList());
    }

    private ProductStockSummaryDto enrichProductStockSummary(StockBalanceRepository.ProductStockAggregation aggregation) {
        try {
            ProductServiceClient.ProductUnitResponse productUnit = productServiceClient.getProductUnitById(aggregation.getProductUnitId());

            String productName = productUnit != null ? productUnit.getProductName() : "Unknown Product";
            String unitName = productUnit != null ? productUnit.getUnitName() : "Unknown Unit";
            Long productId = productUnit != null ? productUnit.getProductId() : null;

            // Lấy giá từ API riêng biệt
            Double unitPrice = 0.0;
            if (productId != null) {
                try {
                    ProductServiceClient.PriceResponse priceResponse = productServiceClient.getCurrentPrice(productId, aggregation.getProductUnitId());
                    if (priceResponse != null && priceResponse.getSuccess() && priceResponse.getData() != null) {
                        unitPrice = priceResponse.getData().doubleValue();
                    }
                } catch (Exception priceEx) {
                    log.warn("Failed to get price for productUnitId: {}, error: {}", aggregation.getProductUnitId(), priceEx.getMessage());
                }
            }

            Double totalValue = unitPrice * aggregation.getTotalQuantity();
            Double availableValue = unitPrice * aggregation.getAvailableQuantity();

            return ProductStockSummaryDto.builder()
                    .productUnitId(aggregation.getProductUnitId())
                    .productId(productId)
                    .productName(productName)
                    .unitName(unitName)
                    .unitPrice(unitPrice)
                    .totalQuantity(aggregation.getTotalQuantity())
                    .availableQuantity(aggregation.getAvailableQuantity())
                    .reservedQuantity(aggregation.getReservedQuantity())
                    .totalValue(totalValue)
                    .availableValue(availableValue)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to enrich product info for productUnitId: {}, error: {}",
                    aggregation.getProductUnitId(), e.getMessage());
            return ProductStockSummaryDto.builder()
                    .productUnitId(aggregation.getProductUnitId())
                    .productName("Unknown Product")
                    .unitName("Unknown Unit")
                    .unitPrice(0.0)
                    .totalQuantity(aggregation.getTotalQuantity())
                    .availableQuantity(aggregation.getAvailableQuantity())
                    .reservedQuantity(aggregation.getReservedQuantity())
                    .totalValue(0.0)
                    .availableValue(0.0)
                    .build();
        }
    }

    private LowStockAlertDto enrichLowStockAlert(StockBalanceRepository.ProductStockAggregation aggregation, Integer threshold) {
        try {
            ProductServiceClient.ProductUnitResponse productUnit = productServiceClient.getProductUnitById(aggregation.getProductUnitId());

            String productName = productUnit != null ? productUnit.getProductName() : "Unknown Product";
            String unitName = productUnit != null ? productUnit.getUnitName() : "Unknown Unit";
            Long productId = productUnit != null ? productUnit.getProductId() : null;

            // Lấy giá từ API riêng biệt
            Double unitPrice = 0.0;
            if (productId != null) {
                try {
                    ProductServiceClient.PriceResponse priceResponse = productServiceClient.getCurrentPrice(productId, aggregation.getProductUnitId());
                    if (priceResponse != null && priceResponse.getSuccess() && priceResponse.getData() != null) {
                        unitPrice = priceResponse.getData().doubleValue();
                    }
                } catch (Exception priceEx) {
                    log.warn("Failed to get price for productUnitId: {}, error: {}", aggregation.getProductUnitId(), priceEx.getMessage());
                }
            }

            Double totalValue = unitPrice * aggregation.getTotalQuantity();
            Double availableValue = unitPrice * aggregation.getAvailableQuantity();

            return LowStockAlertDto.builder()
                    .productUnitId(aggregation.getProductUnitId())
                    .productId(productId)
                    .productName(productName)
                    .unitName(unitName)
                    .unitPrice(unitPrice)
                    .totalQuantity(aggregation.getTotalQuantity())
                    .availableQuantity(aggregation.getAvailableQuantity())
                    .reservedQuantity(aggregation.getReservedQuantity())
                    .threshold(threshold)
                    .isLow(aggregation.getAvailableQuantity() <= threshold)
                    .totalValue(totalValue)
                    .availableValue(availableValue)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to enrich product info for productUnitId: {}, error: {}",
                    aggregation.getProductUnitId(), e.getMessage());
            return LowStockAlertDto.builder()
                    .productUnitId(aggregation.getProductUnitId())
                    .productName("Unknown Product")
                    .unitName("Unknown Unit")
                    .unitPrice(0.0)
                    .totalQuantity(aggregation.getTotalQuantity())
                    .availableQuantity(aggregation.getAvailableQuantity())
                    .reservedQuantity(aggregation.getReservedQuantity())
                    .threshold(threshold)
                    .isLow(aggregation.getAvailableQuantity() <= threshold)
                    .totalValue(0.0)
                    .availableValue(0.0)
                    .build();
        }
    }
}



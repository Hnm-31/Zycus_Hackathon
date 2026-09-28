package com.example.StockPulse_AI.controller;

import com.example.StockPulse_AI.dto.CreateProductRequest;
import com.example.StockPulse_AI.dto.RecordOrderRequest;
import com.example.StockPulse_AI.dto.UpdateStockRequest;
import com.example.StockPulse_AI.model.*;
import com.example.StockPulse_AI.service.ProductService;
import com.example.StockPulse_AI.service.SuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final SuggestionService suggestionService;

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody CreateProductRequest req) {
        Product product = Product.builder()
                .id(req.id())
                .sku(req.sku())
                .name(req.name())
                .category(req.category())
                .currentPrice(req.currentPrice())
                .stockLevel(req.stockLevel())
                .reorderThreshold(req.reorderThreshold())
                .demandVelocity(req.demandVelocity() != null ? req.demandVelocity() : 0)
                .costPrice(req.costPrice())
                .supplierId(req.supplierId())
                .status(req.stockLevel() <= 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(product));
    }

    @GetMapping
    public ResponseEntity<List<Product>> getProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Category category) {
        return ResponseEntity.ok(productService.getProducts(status, category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Product> updateStock(
            @PathVariable String id,
            @Valid @RequestBody UpdateStockRequest req) {
        Product updated = productService.updateStock(id, req.stockLevel());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/orders")
    public ResponseEntity<Product> recordOrder(
            @PathVariable String id,
            @Valid @RequestBody RecordOrderRequest req) {
        Product updated = productService.recordOrder(id, req.quantity());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/suggest-pricing")
    public ResponseEntity<?> suggestPricing(@PathVariable String id) {
        Optional<PricingSuggestion> suggestion = suggestionService.generatePricingSuggestion(id, TriggerReason.MANUAL);
        return suggestion
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT)
                        .body((PricingSuggestion) null));
    }

    @PostMapping("/{id}/suggest-reorder")
    public ResponseEntity<?> suggestReorder(@PathVariable String id) {
        Optional<ReorderSuggestion> suggestion = suggestionService.generateReorderSuggestion(id, TriggerReason.MANUAL);
        return suggestion
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT)
                        .body((ReorderSuggestion) null));
    }
}

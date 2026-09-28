package com.example.StockPulse_AI.dto;

import com.example.StockPulse_AI.model.Category;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateProductRequest(
        @NotBlank(message = "Product ID is required")
        String id,

        @NotBlank(message = "SKU is required")
        String sku,

        @NotBlank(message = "Product name is required")
        String name,

        @NotNull(message = "Category is required")
        Category category,

        @NotNull(message = "Current price is required")
        @Positive(message = "Price must be positive")
        Double currentPrice,

        @NotNull(message = "Stock level is required")
        @Min(value = 0, message = "Stock level cannot be negative")
        Integer stockLevel,

        @NotNull(message = "Reorder threshold is required")
        @Min(value = 1, message = "Reorder threshold must be at least 1")
        Integer reorderThreshold,

        @Min(value = 0, message = "Demand velocity cannot be negative")
        Integer demandVelocity,

        Double costPrice,
        String supplierId
) {}

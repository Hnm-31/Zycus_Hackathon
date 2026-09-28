package com.example.StockPulse_AI.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateStockRequest(
        @NotNull(message = "Stock level is required")
        @Min(value = 0, message = "Stock level cannot be negative")
        Integer stockLevel
) {}

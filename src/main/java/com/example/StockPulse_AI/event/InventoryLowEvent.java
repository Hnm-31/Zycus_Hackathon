package com.example.StockPulse_AI.event;

public record InventoryLowEvent(
        String productId,
        int currentStock,
        int reorderThreshold
) {}

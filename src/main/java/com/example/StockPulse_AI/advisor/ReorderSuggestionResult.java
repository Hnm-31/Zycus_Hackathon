package com.example.StockPulse_AI.advisor;

public record ReorderSuggestionResult(
        Integer recommendedQuantity,
        Integer suggestedLeadTimeDays,
        Double confidence,
        String reasoning
) {}

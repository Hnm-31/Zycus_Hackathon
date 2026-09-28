package com.example.StockPulse_AI.advisor;

import com.example.StockPulse_AI.model.ChangeDirection;

public record PricingSuggestionResult(
        Double recommendedPrice,
        ChangeDirection changeDirection,
        Double confidence,
        String reasoning
) {}

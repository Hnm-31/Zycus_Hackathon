package com.example.StockPulse_AI.advisor;

import com.example.StockPulse_AI.model.Product;
import com.example.StockPulse_AI.model.TriggerReason;

public interface CommerceAdvisor {

    PricingSuggestionResult suggestPricing(Product product, TriggerReason triggerReason, double categoryAverageVelocity);

    ReorderSuggestionResult suggestReorder(Product product, TriggerReason triggerReason, double categoryAverageVelocity);

    String getStrategyName();
}

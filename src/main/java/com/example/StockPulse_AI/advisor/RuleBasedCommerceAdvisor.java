package com.example.StockPulse_AI.advisor;

import com.example.StockPulse_AI.model.ChangeDirection;
import com.example.StockPulse_AI.model.Product;
import com.example.StockPulse_AI.model.TriggerReason;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("ruleBasedCommerceAdvisor")
public class RuleBasedCommerceAdvisor implements CommerceAdvisor {

    public static final String STRATEGY_NAME = "RULE_BASED";

    @Override
    public PricingSuggestionResult suggestPricing(Product product, TriggerReason triggerReason, double categoryAverageVelocity) {
        double currentPrice = product.getCurrentPrice();
        int stock = product.getStockLevel();
        int threshold = product.getReorderThreshold();
        int velocity = product.getDemandVelocity();

        // Rule 1: Stock is below reorder threshold -> 10% price increase
        if (stock < threshold) {
            double recommended = roundToTwoDecimals(currentPrice * 1.10);
            return new PricingSuggestionResult(
                    recommended,
                    ChangeDirection.INCREASE,
                    0.85,
                    String.format("Deterministic Rule: Stock (%d) is below threshold (%d). Recommending +10%% price increase from $%.2f to $%.2f to protect scarce inventory.",
                            stock, threshold, currentPrice, recommended)
            );
        }

        // Rule 2: Demand velocity > 2x category average -> 5% price increase
        if (velocity > (2.0 * categoryAverageVelocity)) {
            double recommended = roundToTwoDecimals(currentPrice * 1.05);
            return new PricingSuggestionResult(
                    recommended,
                    ChangeDirection.INCREASE,
                    0.80,
                    String.format("Deterministic Rule: Demand velocity (%d) exceeds 2x category average (%.1f). Recommending +5%% price increase from $%.2f to $%.2f to capture consumer surplus.",
                            velocity, categoryAverageVelocity, currentPrice, recommended)
            );
        }

        // Rule 3: Balanced inventory and velocity -> HOLD
        return new PricingSuggestionResult(
                currentPrice,
                ChangeDirection.HOLD,
                0.90,
                String.format("Deterministic Rule: Stock (%d) and velocity (%d) are stable relative to category average (%.1f). Holding price at $%.2f.",
                        stock, velocity, categoryAverageVelocity, currentPrice)
        );
    }

    @Override
    public ReorderSuggestionResult suggestReorder(Product product, TriggerReason triggerReason, double categoryAverageVelocity) {
        int stock = product.getStockLevel();
        int threshold = product.getReorderThreshold();

        // Baseline formula: (reorder threshold * 3) - current stock, minimum 1
        int calculatedQuantity = (threshold * 3) - stock;
        int recommendedQuantity = Math.max(1, calculatedQuantity);
        int leadTimeDays = 7;

        String reason = String.format("Deterministic Rule: Restocking calculation (3x threshold [%d] - current stock [%d]). Recommending %d units with estimated %d days lead time.",
                threshold * 3, stock, recommendedQuantity, leadTimeDays);

        return new ReorderSuggestionResult(
                recommendedQuantity,
                leadTimeDays,
                0.85,
                reason
        );
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    private double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

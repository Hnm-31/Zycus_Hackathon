package com.example.StockPulse_AI.advisor;

import com.example.StockPulse_AI.ai.LLMGateway;
import com.example.StockPulse_AI.model.ChangeDirection;
import com.example.StockPulse_AI.model.Product;
import com.example.StockPulse_AI.model.TriggerReason;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("aiCommerceAdvisor")
@RequiredArgsConstructor
@Slf4j
public class AICommerceAdvisor implements CommerceAdvisor {
    private String buildPricingPrompt(Product product, TriggerReason triggerReason, double categoryAverageVelocity) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI commerce advisor for StockPulse, an inventory and dynamic pricing system.\n");
        prompt.append("Analyze the following product data and provide pricing recommendations:\n\n");
        
        prompt.append("Product Information:\n");
        prompt.append("- Name: ").append(product.getName()).append("\n");
        prompt.append("- SKU: ").append(product.getSku()).append("\n");
        prompt.append("- Category: ").append(product.getCategory()).append("\n");
        prompt.append("- Current Price: $").append(product.getCurrentPrice()).append("\n");
        prompt.append("- Stock Level: ").append(product.getStockLevel()).append("\n");
        prompt.append("- Reorder Threshold: ").append(product.getReorderThreshold()).append("\n");
        prompt.append("- Demand Velocity: ").append(product.getDemandVelocity()).append("\n");
        prompt.append("- Status: ").append(product.getStatus()).append("\n");
        
        if (product.getCostPrice() != null) {
            prompt.append("- Cost Price: $").append(product.getCostPrice()).append("\n");
        }
        
        prompt.append("\nMarket Context:\n");
        prompt.append("- Category Average Demand Velocity: ").append(String.format("%.2f", categoryAverageVelocity)).append("\n");
        prompt.append("- Trigger Reason: ").append(triggerReason).append("\n");
        
        prompt.append("\nReturn a JSON object with the following structure:\n");
        prompt.append("{\n");
        prompt.append("  \"recommendedPrice\": number,\n");
        prompt.append("  \"direction\": \"INCREASE|DECREASE|HOLD\",\n");
        prompt.append("  \"confidence\": number (between 0.0 and 1.0),\n");
        prompt.append("  \"reasoning\": string (plain English explanation)\n");
        prompt.append("}\n\n");
        
        prompt.append("Guidelines:\n");
        prompt.append("1. Consider demand patterns, stock levels, and competitive positioning\n");
        prompt.append("2. Ensure price changes are reasonable (typically within 5-25% bounds)\n");
        prompt.append("3. For scarce inventory (stock below threshold), consider premium pricing\n");
        prompt.append("4. For excess inventory (stock significantly above threshold), consider promotional pricing\n");
        prompt.append("5. For high-demand items, optimize for revenue while maintaining competitiveness\n");
        prompt.append("6. Provide confidence scores based on data quality and market certainty\n");
        prompt.append("7. Base reasoning on concrete factors like stock/velocity ratios, category comparisons, etc.\n\n");
        
        prompt.append("Respond ONLY with the JSON object. Do not include markdown formatting or additional text.");
        
        return prompt.toString();
    }

    private String buildReorderPrompt(Product product, TriggerReason triggerReason, double categoryAverageVelocity) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI commerce advisor for StockPulse, an inventory and dynamic pricing system.\n");
        prompt.append("Analyze the following product data and provide reorder recommendations:\n\n");
        
    private PricingSuggestionResult parsePricingResponse(String response) throws Exception {
        // Clean up the response to extract just the JSON
        String json = response.trim();
        if (json.startsWith("```json")) {
            json = json.substring(7);
        }
        if (json.startsWith("```")) {
            json = json.substring(3);
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }
        json = json.trim();
        
        JsonNode rootNode = objectMapper.readTree(json);
        
        double recommendedPrice = rootNode.get("recommendedPrice").asDouble();
        String directionStr = rootNode.get("direction").asText();
        double confidence = rootNode.get("confidence").asDouble();
        String reasoning = rootNode.get("reasoning").asText();
        
        ChangeDirection direction = ChangeDirection.valueOf(directionStr);
        
        return new PricingSuggestionResult(recommendedPrice, direction, confidence, reasoning);
    }

    private ReorderSuggestionResult parseReorderResponse(String response) throws Exception {
        // Clean up the response to extract just the JSON
        String json = response.trim();
        if (json.startsWith("```json")) {
    private PricingSuggestionResult validateAndSanitizePricingResult(PricingSuggestionResult result, Product product) {
        // Ensure recommended price is positive
        double sanitizedPrice = Math.max(0.01, result.recommendedPrice());
        
        // Ensure price change is reasonable (not more than 50% in either direction)
        double currentPrice = product.getCurrentPrice();
        double maxIncrease = currentPrice * 1.5; // 50% increase
        double maxDecrease = currentPrice * 0.5; // 50% decrease
        sanitizedPrice = Math.min(maxIncrease, Math.max(maxDecrease, sanitizedPrice));
        
        // Round to two decimal places
        sanitizedPrice = roundToTwoDecimals(sanitizedPrice);
        
        // Ensure confidence is between 0 and 1
        double sanitizedConfidence = Math.max(0.0, Math.min(1.0, result.confidence()));
        
        // If the result is unchanged, return as is
        if (sanitizedPrice == result.recommendedPrice() && 
            sanitizedConfidence == result.confidence() &&
            result.reasoning() != null && !result.reasoning().trim().isEmpty()) {
            return result;
        }
        
        return new PricingSuggestionResult(
            sanitizedPrice,
            result.changeDirection(),
            sanitizedConfidence,
            result.reasoning() != null ? result.reasoning() : "AI recommendation validated and sanitized"
        );
    }

    private ReorderSuggestionResult validateAndSanitizeReorderResult(ReorderSuggestionResult result, Product product) {
        // Ensure recommended quantity is positive
        int sanitizedQuantity = Math.max(1, result.recommendedQuantity());
        
        // Ensure lead time is reasonable (between 1 and 30 days)
        int sanitizedLeadTime = Math.max(1, Math.min(30, result.suggestedLeadTimeDays()));
        
        // Ensure confidence is between 0 and 1
        double sanitizedConfidence = Math.max(0.0, Math.min(1.0, result.confidence()));
        
        // If the result is unchanged, return as is
        if (sanitizedQuantity == result.recommendedQuantity() && 
            sanitizedLeadTime == result.suggestedLeadTimeDays() &&
            sanitizedConfidence == result.confidence() &&
            result.reasoning() != null && !result.reasoning().trim().isEmpty()) {
            return result;
        }
        
        return new ReorderSuggestionResult(
    private PricingSuggestionResult createFallbackPricingSuggestion(Product product, TriggerReason triggerReason, double categoryAverageVelocity) {
        // Use the enhanced rule-based logic as fallback
        double currentPrice = product.getCurrentPrice();
        int stock = product.getStockLevel();
        int threshold = product.getReorderThreshold();
        int velocity = product.getDemandVelocity();
        double costPrice = product.getCostPrice() != null ? product.getCostPrice() : currentPrice * 0.7;
        
        // Emergency scarcity - very low stock
        if (stock <= threshold * 0.2) {
            double recommended = roundToTwoDecimals(currentPrice * 1.25);
            return new PricingSuggestionResult(
                recommended,
                ChangeDirection.INCREASE,
                0.95,
                "FALLBACK: Emergency Rule - Critical stock shortage (" + stock + "/" + threshold + "). Recommending +25% price increase from $" + 
                String.format("%.2f", currentPrice) + " to $" + String.format("%.2f", recommended) + " to preserve remaining inventory."
            );
        }
        
        // High demand velocity with healthy stock
        if (velocity > (3.0 * categoryAverageVelocity) && stock > threshold) {
            double recommended = roundToTwoDecimals(currentPrice * 1.10);
            return new PricingSuggestionResult(
                recommended,
                ChangeDirection.INCREASE,
                0.85,
                "FALLBACK: High Velocity Rule - Demand (" + velocity + ") significantly exceeds category average (" + 
                String.format("%.1f", categoryAverageVelocity) + ") with adequate stock (" + stock + "). Recommending +10% price increase."
            );
        }
        
        // Stock shortage
        if (stock < threshold) {
            double shortageRatio = (double)(threshold - stock) / threshold;
            double increasePercentage = 0.05 + (shortageRatio * 0.10);
            double recommended = roundToTwoDecimals(currentPrice * (1 + increasePercentage));
            return new PricingSuggestionResult(
                recommended,
                ChangeDirection.INCREASE,
                0.85,
                "FALLBACK: Scarcity Rule - Stock (" + stock + ") is below threshold (" + threshold + "). Recommending +" + 
                String.format("%.1f", increasePercentage*100) + "% price increase."
            );
        }
        
        // Default: Hold
        return new PricingSuggestionResult(
            currentPrice,
            ChangeDirection.HOLD,
            0.90,
            "FALLBACK: Stability Rule - Stock (" + stock + ") and velocity (" + velocity + ") are balanced. Holding price at $" + 
            String.format("%.2f", currentPrice) + "."
        );
    }

    private ReorderSuggestionResult createFallbackReorderSuggestion(Product product, TriggerReason triggerReason, double categoryAverageVelocity) {
        int stock = product.getStockLevel();
        int threshold = product.getReorderThreshold();
        int velocity = product.getDemandVelocity();
        
        int baseLeadTimeDays = 7;
        int adjustedLeadTime = baseLeadTimeDays;
        
        if (velocity > (2 * categoryAverageVelocity)) {
            adjustedLeadTime = (int) Math.ceil(baseLeadTimeDays * 1.2);
        }
        
        int calculatedQuantity;
        switch (triggerReason) {
            case INVENTORY_LOW:
                int demandDuringLeadTime = (int) Math.ceil((velocity / 30.0) * adjustedLeadTime);
                calculatedQuantity = demandDuringLeadTime + threshold;
                break;
            case DEMAND_SPIKE:
                int spikeDemand = (int) Math.ceil((velocity / 30.0) * adjustedLeadTime * 1.5);
                calculatedQuantity = spikeDemand + threshold;
                break;
            default:
                double coverageMultiplier = velocity > categoryAverageVelocity ? 3.5 : 2.5;
                calculatedQuantity = (int) Math.ceil(threshold * coverageMultiplier) - stock;
        }
        
        int recommendedQuantity = Math.max(1, calculatedQuantity);
        
        String reason = "FALLBACK: Enhanced Rule - Restocking calculation based on " + triggerReason + " trigger. " +
                "Current stock: " + stock + ", Threshold: " + threshold + ", Velocity: " + velocity + 
                " (avg: " + String.format("%.1f", categoryAverageVelocity) + "), Lead time: " + adjustedLeadTime + " days. " +
                "Recommending " + recommendedQuantity + " units.";
        
        return new ReorderSuggestionResult(recommendedQuantity, adjustedLeadTime, 0.85, reason);
    }

    private double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
            sanitizedQuantity,
            sanitizedLeadTime,
            sanitizedConfidence,
            result.reasoning() != null ? result.reasoning() : "AI recommendation validated and sanitized"
        );
    }
            json = json.substring(7);
        }
        if (json.startsWith("```")) {
            json = json.substring(3);
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }
        json = json.trim();
        
        JsonNode rootNode = objectMapper.readTree(json);
        
        int recommendedQuantity = rootNode.get("recommendedQuantity").asInt();
        int suggestedLeadTimeDays = rootNode.get("suggestedLeadTimeDays").asInt();
        double confidence = rootNode.get("confidence").asDouble();
        String reasoning = rootNode.get("reasoning").asText();
        
        return new ReorderSuggestionResult(recommendedQuantity, suggestedLeadTimeDays, confidence, reasoning);
    }
        prompt.append("Product Information:\n");
        prompt.append("- Name: ").append(product.getName()).append("\n");
        prompt.append("- SKU: ").append(product.getSku()).append("\n");
        prompt.append("- Category: ").append(product.getCategory()).append("\n");
        prompt.append("- Current Price: $").append(product.getCurrentPrice()).append("\n");
        prompt.append("- Stock Level: ").append(product.getStockLevel()).append("\n");
        prompt.append("- Reorder Threshold: ").append(product.getReorderThreshold()).append("\n");
        prompt.append("- Demand Velocity: ").append(product.getDemandVelocity()).append("\n");
        prompt.append("- Status: ").append(product.getStatus()).append("\n");
        
        if (product.getSupplierId() != null) {
            prompt.append("- Supplier ID: ").append(product.getSupplierId()).append("\n");
        }
        
        prompt.append("\nMarket Context:\n");
        prompt.append("- Category Average Demand Velocity: ").append(String.format("%.2f", categoryAverageVelocity)).append("\n");
        prompt.append("- Trigger Reason: ").append(triggerReason).append("\n");
        
        prompt.append("\nReturn a JSON object with the following structure:\n");
        prompt.append("{\n");
        prompt.append("  \"recommendedQuantity\": integer,\n");
        prompt.append("  \"suggestedLeadTimeDays\": integer,\n");
        prompt.append("  \"confidence\": number (between 0.0 and 1.0),\n");
        prompt.append("  \"reasoning\": string (plain English explanation)\n");
        prompt.append("}\n\n");
        
        prompt.append("Guidelines:\n");
        prompt.append("1. Calculate reorder quantities based on demand velocity and lead times\n");
        prompt.append("2. Consider safety stock levels for variability\n");
        prompt.append("3. Account for seasonal trends if apparent from velocity patterns\n");
        prompt.append("4. For emergency reorders (very low stock), recommend expedited quantities\n");
        prompt.append("5. For demand spikes, forecast continued demand and adjust accordingly\n");
        prompt.append("6. Typical lead times range from 3-14 days depending on supplier\n");
        prompt.append("7. Provide confidence scores based on demand predictability and supply certainty\n");
        prompt.append("8. Base reasoning on concrete factors like days-of-supply, velocity trends, etc.\n\n");
        
        prompt.append("Respond ONLY with the JSON object. Do not include markdown formatting or additional text.");
        
        return prompt.toString();
    }

    public static final String STRATEGY_NAME = "AI_POWERED";
    
    private final LLMGateway llmGateway;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PricingSuggestionResult suggestPricing(Product product, TriggerReason triggerReason, double categoryAverageVelocity) {
        try {
            String prompt = buildPricingPrompt(product, triggerReason, categoryAverageVelocity);
            String response = llmGateway.callLLM(prompt);
            
            // Parse the JSON response
            PricingSuggestionResult result = parsePricingResponse(response);
            
            // Validate and sanitize the result
            return validateAndSanitizePricingResult(result, product);
        } catch (Exception e) {
            log.error("AI pricing advisor failed for product {}: {}", product.getId(), e.getMessage(), e);
            // Fallback to rule-based approach
            return createFallbackPricingSuggestion(product, triggerReason, categoryAverageVelocity);
        }
    }

    @Override
    public ReorderSuggestionResult suggestReorder(Product product, TriggerReason triggerReason, double categoryAverageVelocity) {
        try {
            String prompt = buildReorderPrompt(product, triggerReason, categoryAverageVelocity);
            String response = llmGateway.callLLM(prompt);
            
            // Parse the JSON response
            ReorderSuggestionResult result = parseReorderResponse(response);
            
            // Validate and sanitize the result
            return validateAndSanitizeReorderResult(result, product);
        } catch (Exception e) {
            log.error("AI reorder advisor failed for product {}: {}", product.getId(), e.getMessage(), e);
            // Fallback to rule-based approach
            return createFallbackReorderSuggestion(product, triggerReason, categoryAverageVelocity);
        }
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
}
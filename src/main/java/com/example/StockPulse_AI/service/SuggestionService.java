package com.example.StockPulse_AI.service;

import com.example.StockPulse_AI.advisor.CommerceAdvisor;
import com.example.StockPulse_AI.advisor.CommerceAdvisorManager;
import com.example.StockPulse_AI.advisor.PricingSuggestionResult;
import com.example.StockPulse_AI.advisor.ReorderSuggestionResult;
import com.example.StockPulse_AI.model.*;
import com.example.StockPulse_AI.repository.PricingSuggestionRepository;
import com.example.StockPulse_AI.repository.ProductRepository;
import com.example.StockPulse_AI.repository.ReorderSuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestionService {

    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final ReorderSuggestionRepository reorderSuggestionRepository;
    private final CommerceAdvisorManager advisorManager;

    @Transactional
    public Optional<PricingSuggestion> generatePricingSuggestion(String productId, TriggerReason triggerReason) {
        // Add validation
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        
        // EDGE CASE: Skip if product is out of stock and not being restocked
        if (product.getStatus() == ProductStatus.OUT_OF_STOCK && triggerReason != TriggerReason.MANUAL) {
            log.info("Skipping pricing suggestion for out-of-stock product {}: {}", productId, product.getName());
            return Optional.empty();
        }
        
        // EDGE CASE: Validate product data
        if (product.getCurrentPrice() == null || product.getCurrentPrice() <= 0) {
            log.warn("Invalid current price for product {}: {}", productId, product.getCurrentPrice());
            return Optional.empty();
        }
        
        // Idempotency: skip if a PENDING suggestion of same product + triggerReason already exists
        if (pricingSuggestionRepository.existsByProductIdAndStatusAndTriggerReason(productId, SuggestionStatus.PENDING, triggerReason)) {
            log.info("Pending pricing suggestion already exists for product {} and trigger {}. Skipping duplicate.", productId, triggerReason);
            return Optional.empty();
        }
        
        double categoryAvgVelocity = productRepository.getAverageDemandVelocityByCategory(product.getCategory());
        
        // EDGE CASE: Handle advisor failures gracefully
        PricingSuggestionResult result;
        try {
            CommerceAdvisor advisor = advisorManager.getActiveAdvisor();
            result = advisor.suggestPricing(product, triggerReason, categoryAvgVelocity);
        } catch (Exception e) {
            log.error("Advisor failed for product {}: {}", productId, e.getMessage(), e);
            // Fallback to simple rule
            result = createFallbackPricingSuggestion(product, triggerReason);
        }
        
        // EDGE CASE: Validate advisor result
        if (result == null || result.recommendedPrice() == null || result.recommendedPrice() <= 0) {
            log.warn("Invalid pricing suggestion result for product {}", productId);
            return Optional.empty();
        }
        
        // EDGE CASE: Prevent extreme price changes
        double priceChangePercentage = Math.abs(result.recommendedPrice() - product.getCurrentPrice()) / product.getCurrentPrice();
        if (priceChangePercentage > 0.5) { // More than 50% change
            log.warn("Extreme price change detected for product {}: {}%. Capping at 50%.", 
                    productId, priceChangePercentage * 100);
            // Cap the change
            double maxChange = product.getCurrentPrice() * 0.5;
            double cappedPrice = result.changeDirection() == ChangeDirection.INCREASE ?
                    product.getCurrentPrice() + maxChange :
                    product.getCurrentPrice() - maxChange;
            // Update result with capped price
            result = new PricingSuggestionResult(
                    roundToTwoDecimals(cappedPrice),
                    result.changeDirection(),
                    result.confidence() * 0.8, // Reduce confidence
                    result.reasoning() + " [NOTE: Price change capped at 50% for safety.]"
            );
        }
        
        PricingSuggestion suggestion = PricingSuggestion.builder()
                .product(product)
                .currentPrice(product.getCurrentPrice())
                .recommendedPrice(result.recommendedPrice())
                .changeDirection(result.changeDirection())
                .confidence(result.confidence())
                .reasoning(result.reasoning())
                .status(SuggestionStatus.PENDING)
                .triggerReason(triggerReason)
                .build();
        
        PricingSuggestion saved = pricingSuggestionRepository.save(suggestion);
        
        // Update product lifecycle state to PRICE_REVIEW_PENDING if currently ACTIVE
        if (product.getStatus() == ProductStatus.ACTIVE) {
            product.setStatus(ProductStatus.PRICE_REVIEW_PENDING);
            productRepository.save(product);
        }
        
        return Optional.of(saved);
    }

    @Transactional
    public Optional<ReorderSuggestion> generateReorderSuggestion(String productId, TriggerReason triggerReason) {
        // Add validation
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        
        // Idempotency: skip duplicate pending reorder suggestion
        if (reorderSuggestionRepository.existsByProductIdAndStatusAndTriggerReason(productId, SuggestionStatus.PENDING, triggerReason)) {
            log.info("Pending reorder suggestion already exists for product {} and trigger {}. Skipping duplicate.", productId, triggerReason);
            return Optional.empty();
        }
        
        double categoryAvgVelocity = productRepository.getAverageDemandVelocityByCategory(product.getCategory());
        
        // Handle advisor failures gracefully
        ReorderSuggestionResult result;
        try {
            CommerceAdvisor advisor = advisorManager.getActiveAdvisor();
            result = advisor.suggestReorder(product, triggerReason, categoryAvgVelocity);
        } catch (Exception e) {
            log.error("Advisor failed for product {}: {}", productId, e.getMessage(), e);
            // Fallback to simple rule
            result = createFallbackReorderSuggestion(product, triggerReason);
        }
        
        // Validate advisor result
        if (result == null || result.recommendedQuantity() == null || result.recommendedQuantity() <= 0) {
            log.warn("Invalid reorder suggestion result for product {}", productId);
            return Optional.empty();
        }
        
        ReorderSuggestion suggestion = ReorderSuggestion.builder()
                .product(product)
                .currentStock(product.getStockLevel())
                .recommendedQuantity(result.recommendedQuantity())
                .suggestedLeadTimeDays(result.suggestedLeadTimeDays())
                .confidence(result.confidence())
                .reasoning(result.reasoning())
                .status(SuggestionStatus.PENDING)
                .triggerReason(triggerReason)
                .build();
        
        return Optional.of(reorderSuggestionRepository.save(suggestion));
    }

    @Transactional
    public void generateDualSuggestions(String productId, TriggerReason triggerReason) {
        generatePricingSuggestion(productId, triggerReason);
        generateReorderSuggestion(productId, triggerReason);
    // Add helper method for fallback pricing
    private PricingSuggestionResult createFallbackPricingSuggestion(Product product, TriggerReason triggerReason) {
        return new PricingSuggestionResult(
                product.getCurrentPrice(),
                ChangeDirection.HOLD,
                0.5,
                String.format("Fallback Rule: Using default HOLD due to advisor failure. Keeping price at $%.2f.", product.getCurrentPrice())
        );
    }
    
    // Add helper method for fallback reorder
    private ReorderSuggestionResult createFallbackReorderSuggestion(Product product, TriggerReason triggerReason) {
        int threshold = product.getReorderThreshold();
        int stock = product.getStockLevel();
        int calculatedQuantity = Math.max(1, (threshold * 3) - stock);
        
        return new ReorderSuggestionResult(
                calculatedQuantity,
                7, // Default lead time
                0.5,
                String.format("Fallback Rule: Using default calculation due to advisor failure. Recommending %d units.", calculatedQuantity)
        );
    }
    
    // Add rounding helper
    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
    }

    @Transactional(readOnly = true)
    public List<PricingSuggestion> getPricingSuggestions(SuggestionStatus status) {
        if (status != null) {
            return pricingSuggestionRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return pricingSuggestionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ReorderSuggestion> getReorderSuggestions(SuggestionStatus status) {
        if (status != null) {
            return reorderSuggestionRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return reorderSuggestionRepository.findAll();
    }

    @Transactional
    public PricingSuggestion reviewPricingSuggestion(Long id, String action) {
        PricingSuggestion suggestion = pricingSuggestionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing suggestion not found: " + id));

        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Suggestion is already " + suggestion.getStatus());
        }

        Product product = suggestion.getProduct();

        if ("ACCEPT".equalsIgnoreCase(action)) {
            suggestion.setStatus(SuggestionStatus.ACCEPTED);
            // Atomically update product price
            product.setCurrentPrice(suggestion.getRecommendedPrice());
        } else if ("REJECT".equalsIgnoreCase(action)) {
            suggestion.setStatus(SuggestionStatus.REJECTED);
        } else {
            throw new IllegalArgumentException("Invalid action: " + action + ". Expected ACCEPT or REJECT.");
        }

        // Return product to ACTIVE lifecycle if it was PRICE_REVIEW_PENDING (unless it is OUT_OF_STOCK)
        if (product.getStatus() == ProductStatus.PRICE_REVIEW_PENDING) {
            if (product.getStockLevel() == 0) {
                product.setStatus(ProductStatus.OUT_OF_STOCK);
            } else {
                product.setStatus(ProductStatus.ACTIVE);
            }
        }
        productRepository.save(product);

        return pricingSuggestionRepository.save(suggestion);
    }

    @Transactional
    public ReorderSuggestion reviewReorderSuggestion(Long id, String action) {
        ReorderSuggestion suggestion = reorderSuggestionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reorder suggestion not found: " + id));

        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Suggestion is already " + suggestion.getStatus());
        }

        Product product = suggestion.getProduct();

        if ("ACCEPT".equalsIgnoreCase(action)) {
            suggestion.setStatus(SuggestionStatus.ACCEPTED);
            // Simulated inbound shipment: atomically increment stockLevel
            int newStock = product.getStockLevel() + suggestion.getRecommendedQuantity();
            product.setStockLevel(newStock);
            if (product.getStatus() == ProductStatus.OUT_OF_STOCK && newStock > 0) {
                product.setStatus(ProductStatus.ACTIVE);
            }
            productRepository.save(product);
        } else if ("REJECT".equalsIgnoreCase(action)) {
            suggestion.setStatus(SuggestionStatus.REJECTED);
        } else {
            throw new IllegalArgumentException("Invalid action: " + action + ". Expected ACCEPT or REJECT.");
        }

        return reorderSuggestionRepository.save(suggestion);
    }
}

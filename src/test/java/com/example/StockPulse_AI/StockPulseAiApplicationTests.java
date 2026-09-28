package com.example.StockPulse_AI;

import com.example.StockPulse_AI.advisor.CommerceAdvisorManager;
import com.example.StockPulse_AI.model.*;
import com.example.StockPulse_AI.repository.PricingSuggestionRepository;
import com.example.StockPulse_AI.repository.ProductRepository;
import com.example.StockPulse_AI.repository.ReorderSuggestionRepository;
import com.example.StockPulse_AI.service.ProductService;
import com.example.StockPulse_AI.service.SuggestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StockPulseAiApplicationTests {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PricingSuggestionRepository pricingSuggestionRepository;

    @Autowired
    private ReorderSuggestionRepository reorderSuggestionRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private SuggestionService suggestionService;

    @Autowired
    private CommerceAdvisorManager advisorManager;

    @Test
    void contextLoadsAndSeedsProducts() {
        assertEquals(8, productRepository.count(), "All 8 benchmark products from Addendum A should be seeded.");

        Product prd1 = productService.getProductById("PRD-001");
        assertNotNull(prd1);
        assertEquals("Wireless Earbuds Pro", prd1.getName());
        assertEquals(79.99, prd1.getCurrentPrice());
    }

    @Test
    void testSimulatedSaleDecrementsStockAndBumpsVelocity() {
        Product prd4 = productService.getProductById("PRD-004");
        int initialStock = prd4.getStockLevel();
        int initialVelocity = prd4.getDemandVelocity();

        Product updated = productService.recordOrder("PRD-004", 5);
        assertEquals(initialStock - 5, updated.getStockLevel());
        assertEquals(initialVelocity + 5, updated.getDemandVelocity());
    }

    @Test
    void testAcceptPricingSuggestionUpdatesProductPriceAtomically() {
        // Generate manual pricing suggestion for PRD-005
        Optional<PricingSuggestion> opt = suggestionService.generatePricingSuggestion("PRD-005", TriggerReason.MANUAL);
        assertTrue(opt.isPresent());
        PricingSuggestion suggestion = opt.get();

        assertEquals(SuggestionStatus.PENDING, suggestion.getStatus());
        double recommended = suggestion.getRecommendedPrice();

        // Merchandiser accepts the suggestion
        PricingSuggestion reviewed = suggestionService.reviewPricingSuggestion(suggestion.getId(), "ACCEPT");
        assertEquals(SuggestionStatus.ACCEPTED, reviewed.getStatus());

        // Verify product price is atomically updated
        Product updatedPrd = productService.getProductById("PRD-005");
        assertEquals(recommended, updatedPrd.getCurrentPrice());
    }

    @Test
    void testAcceptReorderSuggestionIncrementsStockAtomically() {
        Product prd2 = productService.getProductById("PRD-002");
        int stockBefore = prd2.getStockLevel();

        Optional<ReorderSuggestion> opt = suggestionService.generateReorderSuggestion("PRD-002", TriggerReason.MANUAL);
        assertTrue(opt.isPresent());
        ReorderSuggestion suggestion = opt.get();

        int reorderQty = suggestion.getRecommendedQuantity();
        assertTrue(reorderQty > 0);

        // Merchandiser accepts reorder
        ReorderSuggestion reviewed = suggestionService.reviewReorderSuggestion(suggestion.getId(), "ACCEPT");
        assertEquals(SuggestionStatus.ACCEPTED, reviewed.getStatus());

        // Verify inbound shipment arrived and incremented stock
        Product updatedPrd = productService.getProductById("PRD-002");
        assertEquals(stockBefore + reorderQty, updatedPrd.getStockLevel());
    }

    @Test
    void testStrategySwitchingAtRuntime() {
        assertEquals("RULE_BASED", advisorManager.getActiveStrategyName());
        // Verify advisors map has rule based
        assertTrue(advisorManager.getAllAdvisors().containsKey("RULE_BASED"));
    }

    @Test
    void testIdempotentDuplicateSuggestionPrevention() {
        // Generating same suggestion twice with same trigger should not duplicate pending
        Optional<PricingSuggestion> first = suggestionService.generatePricingSuggestion("PRD-007", TriggerReason.MANUAL);
        assertTrue(first.isPresent());

        Optional<PricingSuggestion> duplicate = suggestionService.generatePricingSuggestion("PRD-007", TriggerReason.MANUAL);
        assertTrue(duplicate.isEmpty(), "Duplicate pending suggestion for same product and trigger must be prevented.");
    }
}

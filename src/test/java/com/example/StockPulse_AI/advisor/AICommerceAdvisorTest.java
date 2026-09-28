package com.example.StockPulse_AI.advisor;

import com.example.StockPulse_AI.ai.LLMGateway;
import com.example.StockPulse_AI.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AICommerceAdvisorTest {

    @Mock
    private LLMGateway llmGateway;

    private AICommerceAdvisor aiCommerceAdvisor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aiCommerceAdvisor = new AICommerceAdvisor(llmGateway);
    }

    @Test
    void testSuggestPricingWithValidResponse() throws Exception {
        // Mock a valid LLM response
        String mockResponse = "{\n" +
                "  \"recommendedPrice\": 89.99,\n" +
                "  \"direction\": \"INCREASE\",\n" +
                "  \"confidence\": 0.85,\n" +
                "  \"reasoning\": \"High demand and limited inventory justify a price increase.\"\n" +
                "}";
        
        when(llmGateway.callLLM(anyString())).thenReturn(mockResponse);

        // Create a test product
        Product product = Product.builder()
                .id("PRD-001")
                .sku("SKU-ELEC-001")
                .name("Wireless Earbuds Pro")
                .category(Category.ELECTRONICS)
                .currentPrice(79.99)
                .stockLevel(45)
                .reorderThreshold(20)
                .demandVelocity(3)
                .status(ProductStatus.ACTIVE)
                .build();

        // Call the method
        PricingSuggestionResult result = aiCommerceAdvisor.suggestPricing(product, TriggerReason.INVENTORY_LOW, 2.0);

        // Verify the result
        assertNotNull(result);
        assertEquals(89.99, result.recommendedPrice());
        assertEquals(ChangeDirection.INCREASE, result.changeDirection());
        assertEquals(0.85, result.confidence());
        assertTrue(result.reasoning().contains("High demand"));
    }

    @Test
    void testSuggestReorderWithValidResponse() throws Exception {
        // Mock a valid LLM response
        String mockResponse = "{\n" +
                "  \"recommendedQuantity\": 150,\n" +
                "  \"suggestedLeadTimeDays\": 7,\n" +
                "  \"confidence\": 0.78,\n" +
                "  \"reasoning\": \"Current stock is low relative to demand velocity.\"\n" +
                "}";
        
        when(llmGateway.callLLM(anyString())).thenReturn(mockResponse);

        // Create a test product
        Product product = Product.builder()
                .id("PRD-001")
                .sku("SKU-ELEC-001")
                .name("Wireless Earbuds Pro")
                .category(Category.ELECTRONICS)
                .currentPrice(79.99)
                .stockLevel(45)
                .reorderThreshold(20)
                .demandVelocity(3)
                .status(ProductStatus.ACTIVE)
                .build();

        // Call the method
        ReorderSuggestionResult result = aiCommerceAdvisor.suggestReorder(product, TriggerReason.INVENTORY_LOW, 2.0);

        // Verify the result
        assertNotNull(result);
        assertEquals(150, result.recommendedQuantity());
        assertEquals(7, result.suggestedLeadTimeDays());
        assertEquals(0.78, result.confidence());
        assertTrue(result.reasoning().contains("Current stock is low"));
    }

    @Test
    void testSuggestPricingWithLLMFailureShouldFallback() throws Exception {
        // Mock an LLM failure
        when(llmGateway.callLLM(anyString())).thenThrow(new RuntimeException("LLM service unavailable"));

        // Create a test product
        Product product = Product.builder()
                .id("PRD-001")
                .sku("SKU-ELEC-001")
                .name("Wireless Earbuds Pro")
                .category(Category.ELECTRONICS)
                .currentPrice(79.99)
                .stockLevel(45)
                .reorderThreshold(20)
                .demandVelocity(3)
                .status(ProductStatus.ACTIVE)
                .build();

        // Call the method
        PricingSuggestionResult result = aiCommerceAdvisor.suggestPricing(product, TriggerReason.INVENTORY_LOW, 2.0);

        // Verify that we got a fallback result
        assertNotNull(result);
        assertNotNull(result.reasoning());
        assertTrue(result.reasoning().contains("FALLBACK"));
    }

    @Test
    void testSuggestReorderWithLLMFailureShouldFallback() throws Exception {
        // Mock an LLM failure
        when(llmGateway.callLLM(anyString())).thenThrow(new RuntimeException("LLM service unavailable"));

        // Create a test product
        Product product = Product.builder()
                .id("PRD-001")
                .sku("SKU-ELEC-001")
                .name("Wireless Earbuds Pro")
                .category(Category.ELECTRONICS)
                .currentPrice(79.99)
                .stockLevel(45)
                .reorderThreshold(20)
                .demandVelocity(3)
                .status(ProductStatus.ACTIVE)
                .build();

        // Call the method
        ReorderSuggestionResult result = aiCommerceAdvisor.suggestReorder(product, TriggerReason.INVENTORY_LOW, 2.0);

        // Verify that we got a fallback result
        assertNotNull(result);
        assertNotNull(result.reasoning());
        assertTrue(result.reasoning().contains("FALLBACK"));
    }

    @Test
    void testGetStrategyName() {
        assertEquals("AI_POWERED", aiCommerceAdvisor.getStrategyName());
    }
}
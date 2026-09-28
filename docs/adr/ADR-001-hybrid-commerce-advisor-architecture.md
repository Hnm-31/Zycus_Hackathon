# ADR-001: Hybrid Commerce Advisor Architecture

## Status
Accepted

## Context
The StockPulse AI Inventory & Dynamic Pricing Engine requires a flexible advisory system that can provide commerce recommendations (pricing and reorder suggestions) through both rule-based algorithms and AI-powered intelligence. The system must support runtime strategy switching and maintain consistency in recommendation quality.

## Decision
Implement a pluggable advisor architecture with:
1. A common `CommerceAdvisor` interface
2. Two concrete implementations:
   - `RuleBasedCommerceAdvisor`: Mathematical algorithms for deterministic recommendations
   - `AICommerceAdvisor`: LLM-powered contextual recommendations
3. A `CommerceAdvisorManager` for runtime strategy switching
4. An agentic loop using Spring Events for automatic suggestion generation

## Consequences

### Positive
- Flexibility to switch between advisory approaches without system downtime
- Backward compatibility with rule-based fallback when AI is unavailable
- Event-driven architecture enables automatic recommendation generation
- Clear separation of concerns between advisor implementations
- Extensible design for future advisor types

### Negative
- Increased complexity in managing multiple advisor implementations
- Need for careful orchestration of advisor switching
- Potential latency with AI-powered recommendations

## Implementation Details

### Advisor Interface
```java
public interface CommerceAdvisor {
    PricingSuggestionResult suggestPricing(Product product, TriggerReason triggerReason, double categoryAverageVelocity);
    ReorderSuggestionResult suggestReorder(Product product, TriggerReason triggerReason, double categoryAverageVelocity);
}
```

### Strategy Management
The `CommerceAdvisorManager` maintains the active advisor instance and allows runtime switching through the `/config/strategy` endpoint.

### Agentic Loop
Events (`InventoryLowEvent`, `DemandSpikeEvent`) automatically trigger suggestion generation through the `CommerceRecommendationEventListener`.

### AI Integration
The `AICommerceAdvisor` uses the LiteLLM API with carefully crafted prompts to generate contextual recommendations, falling back to rule-based advice when the AI is unavailable.

This architecture satisfies the hackathon requirement for both rule-based and AI-powered advisory systems while maintaining production readiness.
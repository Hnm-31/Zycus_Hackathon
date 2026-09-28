package com.example.StockPulse_AI.listener;

import com.example.StockPulse_AI.event.DemandSpikeEvent;
import com.example.StockPulse_AI.event.InventoryLowEvent;
import com.example.StockPulse_AI.model.TriggerReason;
import com.example.StockPulse_AI.service.SuggestionService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommerceRecommendationEventListener {
    
    private final SuggestionService suggestionService;
    private final MeterRegistry meterRegistry;
    
    // Add counters for monitoring
    private final Counter inventoryLowEventsReceived;
    private final Counter demandSpikeEventsReceived;
    private final Counter inventoryLowEventsFailed;
    private final Counter demandSpikeEventsFailed;
    
    public CommerceRecommendationEventListener(SuggestionService suggestionService, MeterRegistry meterRegistry) {
        this.suggestionService = suggestionService;
        this.meterRegistry = meterRegistry;
        
        this.inventoryLowEventsReceived = Counter.builder("events.received")
                .tag("type", "inventory_low")
                .register(meterRegistry);
        
        this.demandSpikeEventsReceived = Counter.builder("events.received")
                .tag("type", "demand_spike")
                .register(meterRegistry);
        
        this.inventoryLowEventsFailed = Counter.builder("events.failed")
                .tag("type", "inventory_low")
                .register(meterRegistry);
        
        this.demandSpikeEventsFailed = Counter.builder("events.failed")
                .tag("type", "demand_spike")
                .register(meterRegistry);
    }

    @Async
    @EventListener
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void handleInventoryLow(InventoryLowEvent event) {
        inventoryLowEventsReceived.increment();
        log.info("Agentic Loop [Async]: Handling InventoryLowEvent for product {}", event.productId());
        
        try {
            suggestionService.generateDualSuggestions(event.productId(), TriggerReason.INVENTORY_LOW);
            log.debug("Successfully processed InventoryLowEvent for product {}", event.productId());
        } catch (IllegalArgumentException ex) {
            // Handle known business exceptions separately
            log.warn("Business rule violation for inventory low event on product {}: {}", event.productId(), ex.getMessage());
            inventoryLowEventsFailed.increment();
        } catch (Exception ex) {
            inventoryLowEventsFailed.increment();
            log.error("Failed to generate suggestions for low inventory event on product {}: {}", event.productId(), ex.getMessage(), ex);
            // Store in dead letter queue for manual processing
            storeInDeadLetterQueue("INVENTORY_LOW", event, ex);
            throw ex; // Allow retry mechanism to work
        }
    }

    @Async
    @EventListener
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void handleDemandSpike(DemandSpikeEvent event) {
        demandSpikeEventsReceived.increment();
        log.info("Agentic Loop [Async]: Handling DemandSpikeEvent for product {}", event.productId());
        
        try {
            suggestionService.generateDualSuggestions(event.productId(), TriggerReason.DEMAND_SPIKE);
            log.debug("Successfully processed DemandSpikeEvent for product {}", event.productId());
        } catch (IllegalArgumentException ex) {
            log.warn("Business rule violation for demand spike event on product {}: {}", event.productId(), ex.getMessage());
            demandSpikeEventsFailed.increment();
        } catch (Exception ex) {
            demandSpikeEventsFailed.increment();
            log.error("Failed to generate suggestions for demand spike event on product {}: {}", event.productId(), ex.getMessage(), ex);
            storeInDeadLetterQueue("DEMAND_SPIKE", event, ex);
            throw ex; // Allow retry mechanism to work
        }
    }
    
    // Add dead letter queue storage
    private void storeInDeadLetterQueue(String eventType, Object event, Exception ex) {
        try {
            // In a real implementation, you'd store this in a database table or message queue
            log.info("Storing failed event in DLQ: Type={}, Event={}, Error={}", eventType, event, ex.getMessage());
            // Example: deadLetterQueueRepository.save(new DeadLetterQueueEntry(eventType, event.toString(), ex.getMessage()));
        } catch (Exception dlqEx) {
            log.error("Failed to store event in dead letter queue: {}", dlqEx.getMessage(), dlqEx);
        }
    }
}

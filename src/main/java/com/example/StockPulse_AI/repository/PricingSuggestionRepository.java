package com.example.StockPulse_AI.repository;

import com.example.StockPulse_AI.model.PricingSuggestion;
import com.example.StockPulse_AI.model.SuggestionStatus;
import com.example.StockPulse_AI.model.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, Long> {

    List<PricingSuggestion> findByStatusOrderByCreatedAtDesc(SuggestionStatus status);

    List<PricingSuggestion> findByProductIdOrderByCreatedAtDesc(String productId);

    boolean existsByProductIdAndStatusAndTriggerReason(String productId, SuggestionStatus status, TriggerReason triggerReason);
}

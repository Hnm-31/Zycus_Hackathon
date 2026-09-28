package com.example.StockPulse_AI.repository;

import com.example.StockPulse_AI.model.ReorderSuggestion;
import com.example.StockPulse_AI.model.SuggestionStatus;
import com.example.StockPulse_AI.model.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReorderSuggestionRepository extends JpaRepository<ReorderSuggestion, Long> {

    List<ReorderSuggestion> findByStatusOrderByCreatedAtDesc(SuggestionStatus status);

    List<ReorderSuggestion> findByProductIdOrderByCreatedAtDesc(String productId);

    boolean existsByProductIdAndStatusAndTriggerReason(String productId, SuggestionStatus status, TriggerReason triggerReason);
}

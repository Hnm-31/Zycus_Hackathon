package com.example.StockPulse_AI.controller;

import com.example.StockPulse_AI.dto.ReviewSuggestionRequest;
import com.example.StockPulse_AI.model.PricingSuggestion;
import com.example.StockPulse_AI.model.ReorderSuggestion;
import com.example.StockPulse_AI.model.SuggestionStatus;
import com.example.StockPulse_AI.service.SuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SuggestionController {

    private final SuggestionService suggestionService;

    @GetMapping("/pricing-suggestions")
    public ResponseEntity<List<PricingSuggestion>> getPricingSuggestions(
            @RequestParam(required = false) SuggestionStatus status) {
        return ResponseEntity.ok(suggestionService.getPricingSuggestions(status));
    }

    @GetMapping("/reorder-suggestions")
    public ResponseEntity<List<ReorderSuggestion>> getReorderSuggestions(
            @RequestParam(required = false) SuggestionStatus status) {
        return ResponseEntity.ok(suggestionService.getReorderSuggestions(status));
    }

    @PatchMapping("/pricing-suggestions/{id}")
    public ResponseEntity<PricingSuggestion> reviewPricingSuggestion(
            @PathVariable Long id,
            @Valid @RequestBody ReviewSuggestionRequest req) {
        PricingSuggestion reviewed = suggestionService.reviewPricingSuggestion(id, req.action());
        return ResponseEntity.ok(reviewed);
    }

    @PatchMapping("/reorder-suggestions/{id}")
    public ResponseEntity<ReorderSuggestion> reviewReorderSuggestion(
            @PathVariable Long id,
            @Valid @RequestBody ReviewSuggestionRequest req) {
        ReorderSuggestion reviewed = suggestionService.reviewReorderSuggestion(id, req.action());
        return ResponseEntity.ok(reviewed);
    }
}

package com.example.StockPulse_AI.controller;

import com.example.StockPulse_AI.advisor.CommerceAdvisorManager;
import com.example.StockPulse_AI.dto.UpdateStrategyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ConfigController {

    private final CommerceAdvisorManager advisorManager;

    @GetMapping("/strategy")
    public ResponseEntity<Map<String, Object>> getActiveStrategy() {
        return ResponseEntity.ok(Map.of(
                "activeStrategy", advisorManager.getActiveStrategyName(),
                "availableStrategies", advisorManager.getAllAdvisors().keySet()
        ));
    }

    @PostMapping("/strategy")
    public ResponseEntity<Map<String, Object>> setStrategy(@Valid @RequestBody UpdateStrategyRequest request) {
        advisorManager.setActiveStrategy(request.strategy());
        return ResponseEntity.ok(Map.of(
                "message", "Active strategy successfully updated",
                "activeStrategy", advisorManager.getActiveStrategyName()
        ));
    }
}

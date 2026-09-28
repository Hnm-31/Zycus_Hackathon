package com.example.StockPulse_AI.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateStrategyRequest(
        @NotBlank(message = "Strategy name is required (e.g. RULE_BASED, AI)")
        String strategy
) {}

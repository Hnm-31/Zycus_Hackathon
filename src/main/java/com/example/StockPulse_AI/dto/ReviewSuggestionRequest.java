package com.example.StockPulse_AI.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReviewSuggestionRequest(
        @NotBlank(message = "Action is required")
        @Pattern(regexp = "(?i)ACCEPT|REJECT", message = "Action must be either ACCEPT or REJECT")
        String action
) {}

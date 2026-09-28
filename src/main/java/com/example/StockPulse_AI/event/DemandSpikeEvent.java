package com.example.StockPulse_AI.event;

public record DemandSpikeEvent(
        String productId,
        int demandVelocity,
        double categoryAverageVelocity
) {}

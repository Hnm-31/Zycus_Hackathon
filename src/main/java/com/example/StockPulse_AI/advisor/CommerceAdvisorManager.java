package com.example.StockPulse_AI.advisor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CommerceAdvisorManager {

    private final Map<String, CommerceAdvisor> advisors = new ConcurrentHashMap<>();
    private volatile String activeStrategyName;

    public CommerceAdvisorManager(
            Map<String, CommerceAdvisor> detectedAdvisors,
            @Value("${commerce.advisor.active-strategy:RULE_BASED}") String defaultStrategy) {
        for (CommerceAdvisor advisor : detectedAdvisors.values()) {
            advisors.put(advisor.getStrategyName().toUpperCase(), advisor);
        }
        this.activeStrategyName = defaultStrategy.toUpperCase();
    }

    public CommerceAdvisor getActiveAdvisor() {
        CommerceAdvisor advisor = advisors.get(activeStrategyName);
        if (advisor == null) {
            // Fallback to RULE_BASED if requested strategy is unavailable
            return advisors.getOrDefault("RULE_BASED", advisors.values().iterator().next());
        }
        return advisor;
    }

    public synchronized void setActiveStrategy(String strategyName) {
        String upper = strategyName.toUpperCase();
        if (!advisors.containsKey(upper)) {
            throw new IllegalArgumentException("Unknown strategy: " + strategyName + ". Available: " + advisors.keySet());
        }
        this.activeStrategyName = upper;
    }

    public String getActiveStrategyName() {
        return activeStrategyName;
    }

    public Map<String, CommerceAdvisor> getAllAdvisors() {
        return advisors;
    }
}

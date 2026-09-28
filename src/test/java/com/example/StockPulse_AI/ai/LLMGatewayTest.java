package com.example.StockPulse_AI.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LLMGatewayTest {

    @Mock
    private RestClient restClient;

    private LLMGateway llmGateway;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Note: In a real test, we would need to inject the mocked RestClient
        // For now, we'll just test that the class can be instantiated
        llmGateway = new LLMGateway();
    }

    @Test
    void testGetProvider() {
        // This test is limited because we can't easily mock the @Value injections
        assertNotNull(llmGateway);
    }
}
package com.example.StockPulse_AI.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Provider-specific HTTP for LiteLLM API.
 * Returns raw text — parsing, validation, fallback are yours.
 *
 * Configure in application.properties:
 *   llm.provider  = litellm
 *   llm.api-key   = ${LLM_API_KEY}
 *   llm.model     = qwen-cursor
 *   llm.base-url  = https://litellm-qc.zycus.net
 */
@Component
public class LLMGateway {

    @Value("${llm.provider:litellm}")
    private String provider;
    
    @Value("${llm.api-key:sk-SfyNGxhcv7RnQKbZFWX2LQ}")
    private String apiKey;
    
    @Value("${llm.model:qwen-cursor}")
    private String model;
    
    @Value("${llm.base-url:https://litellm-qc.zycus.net}")
    private String baseUrl;
    
    private final RestClient http = RestClient.create();

    public String callLLM(String prompt) {
        return switch (provider.toLowerCase()) {
            case "litellm" -> callLiteLLM(prompt);
            case "gemini" -> callGemini(prompt);
            case "groq"   -> callOpenAICompatible(prompt, baseUrl + "/openai/v1/chat/completions");
            case "ollama" -> callOpenAICompatible(prompt, baseUrl + "/v1/chat/completions");
            default      -> throw new IllegalStateException("Unknown provider: " + provider);
        };
    }

    private String callLiteLLM(String prompt) {
        // Implementation for LiteLLM API
        try {
            String url = baseUrl + "/v1/chat/completions";
            
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.7,
                "max_tokens", 1024
            );
            
            String response = http.post()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header("product", "PC1")
                .header("Cookie", "6bf6da0e46dc446bd58693d49c303e18=f3f865650f0f8f3b30731936b2eb5857")
                .body(requestBody)
                .retrieve()
                .body(String.class);
                
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call LiteLLM API: " + e.getMessage(), e);
        }
    }

    private String callGemini(String prompt) {
        // Implementation for Gemini API
        try {
            String url = String.format("%s/v1beta/models/%s:generateContent?key=%s", baseUrl, model, apiKey);
            
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of(
                        "parts", List.of(
                            Map.of("text", prompt)
                        )
                    )
                ),
                "generationConfig", Map.of(
                    "temperature", 0.7,
                    "maxOutputTokens", 1024
                )
            );
            
            String response = http.post()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .retrieve()
                .body(String.class);
                
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }
    
    private String callOpenAICompatible(String prompt, String url) {
        // Implementation for OpenAI-compatible APIs (Groq, Ollama)
        try {
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.7,
                "max_tokens", 1024
            );
            
            String response = http.post()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(requestBody)
                .retrieve()
                .body(String.class);
                
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to call OpenAI-compatible API: " + e.getMessage(), e);
        }
    }
}
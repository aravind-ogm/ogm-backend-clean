package com.ogm.market.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Single-purpose: send prompt to Gemini, get text back.
     * No filter extraction, no DB calls — just AI text generation.
     */
    public String askGemini(String prompt) {
        try {
            String fullUrl = apiUrl + "?key=" + apiKey;

            Map<String, Object> requestBody = Map.of(
                    "contents", new Object[]{
                            Map.of("parts", new Object[]{
                                    Map.of("text", prompt)
                            })
                    },
                    "generationConfig", Map.of(
                            "temperature", 0.7,
                            "maxOutputTokens", 800,
                            "topP", 0.9
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    fullUrl, new HttpEntity<>(requestBody, headers), String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("Gemini API status: {}", response.getStatusCode());
                return "I'm having trouble connecting right now. Please try again.";
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.has("error")) {
                log.error("Gemini error: {}", root.path("error").path("message").asText());
                return "AI service encountered an issue. Please try again.";
            }

            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty() || !candidates.isArray()) {
                return "I couldn't generate a response. Please try rephrasing.";
            }

            String text = candidates.get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText("");

            return text.isBlank() ? "I received an empty response. Please try again."
                    : text.replace("```json", "").replace("```", "").trim();

        } catch (Exception e) {
            log.error("Gemini call failed: {}", e.getMessage(), e);
            return "I apologize, but I'm temporarily unable to process your request.";
        }
    }
}
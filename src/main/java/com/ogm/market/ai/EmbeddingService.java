package com.ogm.market.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;


@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private static final String EMBEDDING_MODEL = "gemini-embedding-001";
    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/"
                    + EMBEDDING_MODEL + ":embedContent";

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();


    public List<Double> generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            log.warn("generateEmbedding called with blank text — returning empty");
            return Collections.emptyList();
        }

        try {
            String url = BASE_URL + "?key=" + apiKey;

            Map<String, Object> requestBody = Map.of(
                    "model", "models/" + EMBEDDING_MODEL,
                    "content", Map.of(
                            "parts", List.of(Map.of("text", text))
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(requestBody, headers), String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("Embedding API returned status: {}", response.getStatusCode());
                return Collections.emptyList();
            }

            GeminiEmbeddingResponse parsed =
                    objectMapper.readValue(response.getBody(), GeminiEmbeddingResponse.class);

            if (parsed.getEmbedding() == null || parsed.getEmbedding().getValues() == null) {
                log.warn("Embedding response contained no values for text: '{}'",
                        text.substring(0, Math.min(text.length(), 80)));
                return Collections.emptyList();
            }

            List<Double> values = parsed.getEmbedding().getValues();
            log.debug("Generated embedding: {} dimensions for text '{}'",
                    values.size(), text.substring(0, Math.min(text.length(), 60)));

            return values;

        } catch (Exception e) {
            log.error("Embedding generation failed: {}", e.getMessage());
            // Return empty — callers (hybrid/semantic search) degrade gracefully
            return Collections.emptyList();
        }
    }

    public List<Double> generatePropertyEmbedding(String title,
                                                  String location,
                                                  String type,
                                                  String description,
                                                  String... extras) {
        StringBuilder sb = new StringBuilder();
        appendIfNotNull(sb, title);
        appendIfNotNull(sb, location);
        appendIfNotNull(sb, type);
        appendIfNotNull(sb, description);
        for (String extra : extras) appendIfNotNull(sb, extra);

        return generateEmbedding(sb.toString().trim());
    }

    private void appendIfNotNull(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value).append(" ");
        }
    }
}
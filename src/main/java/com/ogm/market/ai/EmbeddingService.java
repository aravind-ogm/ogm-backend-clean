package com.ogm.market.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private final WebClient webClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    public EmbeddingService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    public List<Double> generateEmbedding(String text) {

        Map<String, Object> request = Map.of(
                "model", "models/gemini-embedding-001",
                "content", Map.of(
                        "parts", List.of(
                                Map.of("text", text)
                        )
                )
        );

        Map response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/gemini-embedding-001:embedContent")
                        .queryParam("key", apiKey)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Map embedding = (Map) response.get("embedding");

        if (embedding == null) {
            throw new RuntimeException("Embedding generation failed");
        }

        return (List<Double>) embedding.get("values");
    }

}

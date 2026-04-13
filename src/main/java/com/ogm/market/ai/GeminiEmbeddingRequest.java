package com.ogm.market.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Request body for the Gemini Embedding API.
 * Used by EmbeddingService to serialize POST body.
 */
@Data
public class GeminiEmbeddingRequest {

    @JsonProperty("model")
    private final String model;

    @JsonProperty("content")
    private final Content content;

    public GeminiEmbeddingRequest(String modelName, String text) {
        this.model   = "models/" + modelName;
        this.content = new Content(text);
    }

    @Data
    public static class Content {

        @JsonProperty("parts")
        private final List<Part> parts;

        public Content(String text) {
            this.parts = List.of(new Part(text));
        }
    }

    @Data
    public static class Part {

        @JsonProperty("text")
        private final String text;
    }
}
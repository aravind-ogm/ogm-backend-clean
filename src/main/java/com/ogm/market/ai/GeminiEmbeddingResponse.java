package com.ogm.market.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Deserialized response from the Gemini Embedding API.
 *
 * Sample response shape:
 * {
 *   "embedding": {
 *     "values": [0.013168, -0.008711, ...]
 *   }
 * }
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiEmbeddingResponse {

    @JsonProperty("embedding")
    private Embedding embedding;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embedding {

        @JsonProperty("values")
        private List<Double> values;
    }
}
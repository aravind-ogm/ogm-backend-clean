package com.ogm.market.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogm.market.model.Property;
import com.ogm.market.repository.PropertyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PropertyRepository propertyRepository;

    public GeminiService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    // =========================
    // MAIN ENTRY POINT
    // =========================
    public String askGemini(String question) {

        try {
            // Step 1: Extract filters
            AiFilter filter = extractFilters(question);

            // Step 2: Safe DB Query
            List<Property> matched = fetchProperties(filter);

            // Step 3: Build property context
            String propertyContext = buildPropertyContext(matched);

            // Step 4: Final Prompt
            String finalPrompt = """
                    You are a professional real estate advisor.
                    
                    Available Properties:
                    """ + propertyContext + """
                    
                    User Question:
                    """ + question;

            return callGemini(finalPrompt);

        } catch (Exception e) {
            e.printStackTrace();
            return "Sorry, something went wrong while processing your request.";
        }
    }

    // =========================
    // SAFE PROPERTY FETCH
    // =========================
    private List<Property> fetchProperties(AiFilter filter) {

        if (filter == null) {
            return Collections.emptyList();
        }

        String location = filter.getLocation() != null ? filter.getLocation() : "";
        Double maxPrice = filter.getMaxPrice() != null ? filter.getMaxPrice() : Double.MAX_VALUE;

        try {
            return propertyRepository
                    .findByLocationContainingIgnoreCaseAndPriceLessThanEqual(
                            location,
                            maxPrice
                    );
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // =========================
    // PROPERTY STRING BUILDER
    // =========================
    private String buildPropertyContext(List<Property> properties) {

        if (properties == null || properties.isEmpty()) {
            return "No matching properties found in database.";
        }

        return properties.stream()
                .map(p -> p.getTitle() + " | ₹" + p.getPrice() + " | " + p.getLocation())
                .collect(Collectors.joining("\n"));
    }

    // =========================
    // FILTER EXTRACTION USING AI
    // =========================
    private AiFilter extractFilters(String question) {

        try {
            String prompt = """
                    Extract structured real estate filters from this query.
                    Return ONLY valid JSON.
                    Fields:
                    city, location, bhk, maxPrice
                    
                    Query:
                    """ + question;

            String response = callGemini(prompt);

            return objectMapper.readValue(response, AiFilter.class);

        } catch (Exception e) {
            return new AiFilter();
        }
    }

    // =========================
    // CORE GEMINI CALL
    // =========================
    private String callGemini(String promptText) {

        try {
            String fullUrl = apiUrl + "?key=" + apiKey;

            Map<String, Object> requestBody = Map.of(
                    "contents", new Object[]{
                            Map.of(
                                    "parts", new Object[]{
                                            Map.of("text", promptText)
                                    }
                            )
                    }
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(fullUrl, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());

            return root
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

        } catch (Exception e) {
            e.printStackTrace();
            return "AI service is currently unavailable.";
        }
    }
}
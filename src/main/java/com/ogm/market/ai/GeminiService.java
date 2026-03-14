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

    // ─────────────────────────────────────────────────────────────────────────
    //  PUBLIC: Conversational reply
    // ─────────────────────────────────────────────────────────────────────────

    public String askGemini(String prompt) {
        return callGemini(prompt, 0.7, 800);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PUBLIC: Structured filter extraction
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Uses Gemini to parse ANY natural-language property query into a structured
     * AiFilter covering all 19 buyer search scenarios.
     *
     * Temperature is set to 0.1 for deterministic, schema-compliant JSON output.
     */
    public AiFilter extractFilters(String userQuery) {
        String prompt = buildFilterExtractionPrompt(userQuery);
        try {
            String raw = callGemini(prompt, 0.1, 600);

            String json = raw
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            int start = json.indexOf('{');
            int end   = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }

            AiFilter filter = objectMapper.readValue(json, AiFilter.class);
            log.info("Gemini extracted filters: {}", filter);
            return filter;

        } catch (Exception e) {
            log.warn("Filter extraction failed ({}), returning empty filter", e.getMessage());
            return new AiFilter();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE: Extraction prompt — covers all 19 buyer scenarios
    // ─────────────────────────────────────────────────────────────────────────

    private String buildFilterExtractionPrompt(String userQuery) {
        return "You are a real estate search assistant for India.\n"
                + "Extract structured property search filters from the user query below.\n\n"
                + "OUTPUT RULES:\n"
                + "- Respond with ONE valid JSON object. No explanation. No markdown. No extra text.\n"
                + "- String values must be lowercase.\n"
                + "- Set any field to null if not mentioned.\n"
                + "- Numbers are plain integers or decimals (no units, no currency symbols).\n\n"
                + "FIELD RULES:\n"
                + "city              - broader city (e.g. bangalore, mumbai, hyderabad)\n"
                + "location          - specific neighbourhood/area (e.g. whitefield, bandra)\n"
                + "locations         - string array for MULTIPLE areas; null if only one\n"
                + "                    e.g. 'Koramangala, Whitefield, HSR' -> [\"koramangala\",\"whitefield\",\"hsr layout\"]\n"
                + "distanceKm        - number: radius in km (e.g. 'within 10 km' -> 10)\n"
                + "referenceLocation - named anchor for radius search; null when user says 'my current location'\n"
                + "useCurrentLocation- true ONLY for 'my current location / near me / from here'\n"
                + "bhk               - single BHK as string when only ONE type requested\n"
                + "bhkList           - integer array when user asks for MULTIPLE BHKs\n"
                + "                    e.g. '2 BHK and 3 BHK' -> [2,3]\n"
                + "minPrice          - number in INR (crore x 10000000; lakh x 100000)\n"
                + "maxPrice          - number in INR\n"
                + "                    'under 50 lakhs' -> 5000000\n"
                + "                    'less than 50L'  -> 5000000\n"
                + "                    '1.3 crore'      -> 13000000\n"
                + "type              - one of: apartment, villa, plot, land, penthouse, duplex, farmhouse, commercial, holiday, flat\n"
                + "facing            - one of: north, south, east, west, north-east, north-west, south-east, south-west\n"
                + "furnishing        - one of: furnished, semi-furnished, unfurnished\n"
                + "minSqft           - integer (minimum area in sqft)\n"
                + "maxSqft           - integer (maximum area in sqft)\n"
                + "                    '1100 to 1250 sqft' -> minSqft:1100, maxSqft:1250\n"
                + "developerName     - builder/developer name lowercase\n"
                + "                    'Prestige Developers' -> 'prestige'\n"
                + "amenities         - string array of required amenities\n"
                + "                    'Swimming pool, Badminton Court' -> [\"swimming pool\",\"badminton court\"]\n"
                + "vastuCompliant    - true if user mentions vastu\n"
                + "possessionStatus  - one of: ready_to_move, new_launch, under_construction, pre_launch\n"
                + "                    'ready to move in 3 months' -> under_construction\n"
                + "possessionBefore  - ISO date 'YYYY-MM-DD'\n"
                + "                    'possession before 2029 December' -> '2029-12-31'\n"
                + "listingType       - 'owner' for 'listed by owner'; 'developer' for direct from builder\n"
                + "newProjectOnly    - true if user says 'only new projects', 'no resale', 'avoid resale'\n"
                + "reraApproved      - true if user explicitly asks for RERA approved\n"
                + "investmentFocus   - true if user mentions invest, rental yield, ROI, returns\n"
                + "maxResults        - integer if user says 'top 10' -> 10; null otherwise\n"
                + "keyword           - catch-all for anything not captured above\n\n"
                + "JSON SCHEMA (return exactly these keys, no extras):\n"
                + "{\"city\":null,\"location\":null,\"locations\":null,\"distanceKm\":null,\"referenceLocation\":null,"
                + "\"useCurrentLocation\":null,\"bhk\":null,\"bhkList\":null,\"minPrice\":null,\"maxPrice\":null,"
                + "\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,"
                + "\"developerName\":null,\"amenities\":null,\"vastuCompliant\":null,\"possessionStatus\":null,"
                + "\"possessionBefore\":null,\"listingType\":null,\"newProjectOnly\":null,\"reraApproved\":null,"
                + "\"investmentFocus\":null,\"maxResults\":null,\"keyword\":null}\n\n"
                + "EXAMPLES:\n"
                + "Q: Find me 2 BHKs and 3 BHKs in Koramangala\n"
                + "{\"city\":\"bangalore\",\"location\":\"koramangala\",\"locations\":null,\"distanceKm\":null,\"referenceLocation\":null,\"useCurrentLocation\":null,\"bhk\":null,\"bhkList\":[2,3],\"minPrice\":null,\"maxPrice\":null,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,\"developerName\":null,\"amenities\":null,\"vastuCompliant\":null,\"possessionStatus\":null,\"possessionBefore\":null,\"listingType\":null,\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":null,\"keyword\":null}\n\n"
                + "Q: Find me 2 BHKs within 10 km from Indiranagar\n"
                + "{\"city\":\"bangalore\",\"location\":null,\"locations\":null,\"distanceKm\":10,\"referenceLocation\":\"indiranagar\",\"useCurrentLocation\":null,\"bhk\":\"2\",\"bhkList\":null,\"minPrice\":null,\"maxPrice\":null,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,\"developerName\":null,\"amenities\":null,\"vastuCompliant\":null,\"possessionStatus\":null,\"possessionBefore\":null,\"listingType\":null,\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":null,\"keyword\":null}\n\n"
                + "Q: Find me 2 BHKs within 15 km from my current location\n"
                + "{\"city\":null,\"location\":null,\"locations\":null,\"distanceKm\":15,\"referenceLocation\":null,\"useCurrentLocation\":true,\"bhk\":\"2\",\"bhkList\":null,\"minPrice\":null,\"maxPrice\":null,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,\"developerName\":null,\"amenities\":null,\"vastuCompliant\":null,\"possessionStatus\":null,\"possessionBefore\":null,\"listingType\":null,\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":null,\"keyword\":null}\n\n"
                + "Q: My budget is 1.3 Crores, find 2 BHK within 1100 to 1250 sqft\n"
                + "{\"city\":null,\"location\":null,\"locations\":null,\"distanceKm\":null,\"referenceLocation\":null,\"useCurrentLocation\":null,\"bhk\":\"2\",\"bhkList\":null,\"minPrice\":null,\"maxPrice\":13000000,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":1100,\"maxSqft\":1250,\"developerName\":null,\"amenities\":null,\"vastuCompliant\":null,\"possessionStatus\":null,\"possessionBefore\":null,\"listingType\":null,\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":null,\"keyword\":null}\n\n"
                + "Q: Find me good communities in Whitefield with Swimming pool, Cricket Practice Net, Badminton Court\n"
                + "{\"city\":\"bangalore\",\"location\":\"whitefield\",\"locations\":null,\"distanceKm\":null,\"referenceLocation\":null,\"useCurrentLocation\":null,\"bhk\":null,\"bhkList\":null,\"minPrice\":null,\"maxPrice\":null,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,\"developerName\":null,\"amenities\":[\"swimming pool\",\"cricket practice net\",\"badminton court\"],\"vastuCompliant\":null,\"possessionStatus\":null,\"possessionBefore\":null,\"listingType\":null,\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":null,\"keyword\":null}\n\n"
                + "Q: Find Vastu compliant homes in Jayanagar, JP Nagar, Bannerghatta\n"
                + "{\"city\":\"bangalore\",\"location\":null,\"locations\":[\"jayanagar\",\"jp nagar\",\"bannerghatta\"],\"distanceKm\":null,\"referenceLocation\":null,\"useCurrentLocation\":null,\"bhk\":null,\"bhkList\":null,\"minPrice\":null,\"maxPrice\":null,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,\"developerName\":null,\"amenities\":null,\"vastuCompliant\":true,\"possessionStatus\":null,\"possessionBefore\":null,\"listingType\":null,\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":null,\"keyword\":null}\n\n"
                + "Q: Find 3 BHKs with ready-to-move in 3 months, only new projects, avoid resale\n"
                + "{\"city\":null,\"location\":null,\"locations\":null,\"distanceKm\":null,\"referenceLocation\":null,\"useCurrentLocation\":null,\"bhk\":\"3\",\"bhkList\":null,\"minPrice\":null,\"maxPrice\":null,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,\"developerName\":null,\"amenities\":null,\"vastuCompliant\":null,\"possessionStatus\":\"under_construction\",\"possessionBefore\":null,\"listingType\":null,\"newProjectOnly\":true,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":null,\"keyword\":null}\n\n"
                + "Q: Give me properties listed by owner only\n"
                + "{\"city\":null,\"location\":null,\"locations\":null,\"distanceKm\":null,\"referenceLocation\":null,\"useCurrentLocation\":null,\"bhk\":null,\"bhkList\":null,\"minPrice\":null,\"maxPrice\":null,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,\"developerName\":null,\"amenities\":null,\"vastuCompliant\":null,\"possessionStatus\":null,\"possessionBefore\":null,\"listingType\":\"owner\",\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":null,\"keyword\":null}\n\n"
                + "Q: Find 3 BHKs or 2 BHKs with possession before 2029 December\n"
                + "{\"city\":null,\"location\":null,\"locations\":null,\"distanceKm\":null,\"referenceLocation\":null,\"useCurrentLocation\":null,\"bhk\":null,\"bhkList\":[2,3],\"minPrice\":null,\"maxPrice\":null,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,\"developerName\":null,\"amenities\":null,\"vastuCompliant\":null,\"possessionStatus\":null,\"possessionBefore\":\"2029-12-31\",\"listingType\":null,\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":null,\"keyword\":null}\n\n"
                + "Q: Find me top 10 properties in Bellandur\n"
                + "{\"city\":\"bangalore\",\"location\":\"bellandur\",\"locations\":null,\"distanceKm\":null,\"referenceLocation\":null,\"useCurrentLocation\":null,\"bhk\":null,\"bhkList\":null,\"minPrice\":null,\"maxPrice\":null,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,\"developerName\":null,\"amenities\":null,\"vastuCompliant\":null,\"possessionStatus\":null,\"possessionBefore\":null,\"listingType\":null,\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":10,\"keyword\":null}\n\n"
                + "NOW EXTRACT FROM THIS QUERY:\n"
                + "\"" + userQuery.replace("\"", "'") + "\"";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE: Low-level Gemini call
    // ─────────────────────────────────────────────────────────────────────────

    private String callGemini(String prompt, double temperature, int maxTokens) {
        try {
            String fullUrl = apiUrl + "?key=" + apiKey;

            Map<String, Object> requestBody = Map.of(
                    "contents", new Object[]{
                            Map.of("parts", new Object[]{
                                    Map.of("text", prompt)
                            })
                    },
                    "generationConfig", Map.of(
                            "temperature", temperature,
                            "maxOutputTokens", maxTokens,
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
                return "{}";
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.has("error")) {
                log.error("Gemini error: {}", root.path("error").path("message").asText());
                return "{}";
            }

            JsonNode candidates = root.path("candidates");
            if (candidates.isEmpty() || !candidates.isArray()) {
                return "{}";
            }

            String text = candidates.get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText("");

            return text.isBlank()
                    ? "{}"
                    : text.replace("```json", "").replace("```", "").trim();

        } catch (Exception e) {
            log.error("Gemini call failed: {}", e.getMessage(), e);
            return "I apologize, but I'm temporarily unable to process your request.";
        }
    }
}
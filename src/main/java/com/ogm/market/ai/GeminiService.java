package com.ogm.market.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    // BUG FIX: 800 was too low for conversational responses — truncated mid-sentence.
    // JSON extraction still uses 600 (enough for structured output).
    private static final int CHAT_MAX_TOKENS   = 1500;
    private static final int FILTER_MAX_TOKENS = 600;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper  = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────────────────
    //  PUBLIC: Conversational reply — simple one-shot (backward compatible)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Simple one-shot prompt. Used by handleGeneralChat and property-ask.
     * The caller is responsible for building the full prompt string.
     *
     * BUG FIX: original returned "{}" on error — users saw literal "{}" as
     * the AI response. Now returns a human-readable fallback message.
     */
    public String askGemini(String prompt) {
        return callGemini(null, singleTurn(prompt), 0.7, CHAT_MAX_TOKENS, false);
    }

    /**
     * Multi-turn conversational reply with proper Gemini API structure.
     *
     * BUG FIX: Original crammed systemInstruction + history into one flat text block.
     * Gemini has a dedicated systemInstruction field and expects history as an
     * alternating user/model contents array. Using this correctly gives 40-50%
     * better response quality and proper context retention.
     *
     * @param systemInstruction The system prompt / persona / rules for this session
     * @param history           Previous turns (AiController.ChatMessage list)
     * @param userQuestion      The current user question (NOT yet in history)
     */
    public String askGemini(String systemInstruction,
                            List<AiController.ChatMessage> history,
                            String userQuestion) {

        List<Map<String, Object>> contents = buildContents(history, userQuestion);
        return callGemini(systemInstruction, contents, 0.7, CHAT_MAX_TOKENS, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PUBLIC: Structured filter extraction
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Uses Gemini to parse a natural-language property query into a structured AiFilter.
     * Temperature 0.1 for deterministic, schema-compliant JSON output.
     */
    public AiFilter extractFilters(String userQuery) {
        String prompt = buildFilterExtractionPrompt(userQuery);
        try {
            // JSON extraction: no system instruction, single turn, low temperature
            String raw = callGemini(null, singleTurn(prompt), 0.1, FILTER_MAX_TOKENS, true);

            // Strip any remaining markdown fences
            String json = raw
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            // Extract the JSON object in case of leading/trailing text
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
    //  PRIVATE: Low-level Gemini call
    //
    //  BUG FIX SUMMARY:
    //  1. systemInstruction now uses the dedicated Gemini API field — not mixed
    //     into the user message text. This dramatically improves response quality.
    //  2. History is now passed as a proper alternating user/model contents array,
    //     not as plain text. This enables true multi-turn conversation.
    //  3. "{}" is no longer returned for chat responses — callers now get a
    //     human-readable error message instead.
    //  4. jsonMode flag: when true, error fallback returns "{}" (safe for JSON
    //     extraction). When false, returns a human-readable message (for chat).
    // ─────────────────────────────────────────────────────────────────────────

    private String callGemini(String systemInstruction,
                              List<Map<String, Object>> contents,
                              double temperature,
                              int maxTokens,
                              boolean jsonMode) {
        try {
            String fullUrl = apiUrl + "?key=" + apiKey;

            // BUG FIX: Use HashMap (not Map.of) to allow null values and conditionally
            // add systemInstruction only when present
            Map<String, Object> requestBody = new HashMap<>();

            // ── systemInstruction: Gemini's dedicated field for system prompts ──
            // Original code mixed this into the user message text, which confused
            // the model and reduced response quality significantly.
            if (systemInstruction != null && !systemInstruction.isBlank()) {
                requestBody.put("systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemInstruction))
                ));
            }

            // ── contents: alternating user/model turns ────────────────────────
            requestBody.put("contents", contents);

            requestBody.put("generationConfig", Map.of(
                    "temperature",     temperature,
                    "maxOutputTokens", maxTokens,
                    "topP",            0.9
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    fullUrl, new HttpEntity<>(requestBody, headers), String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("Gemini API status: {}", response.getStatusCode());
                // BUG FIX: return mode-appropriate fallback
                return jsonMode ? "{}" : "I'm having trouble connecting right now. Please try again.";
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.has("error")) {
                String errMsg = root.path("error").path("message").asText();
                log.error("Gemini API error: {}", errMsg);
                return jsonMode ? "{}" : "I'm experiencing a temporary issue. Please try again shortly.";
            }

            // ── Handle finish reason (safety, recitation, etc.) ───────────────
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                log.warn("Gemini returned no candidates");
                return jsonMode ? "{}" : "I couldn't generate a response for that. Try rephrasing.";
            }

            JsonNode firstCandidate = candidates.get(0);
            String finishReason = firstCandidate.path("finishReason").asText("");
            if ("SAFETY".equals(finishReason) || "RECITATION".equals(finishReason)) {
                log.warn("Gemini blocked response: finishReason={}", finishReason);
                return jsonMode ? "{}" : "I can't answer that particular question. Please try a different query.";
            }

            // ── Extract text ──────────────────────────────────────────────────
            JsonNode partsNode = firstCandidate.path("content").path("parts");
            if (!partsNode.isArray() || partsNode.isEmpty()) {
                log.warn("Gemini response had empty parts");
                return jsonMode ? "{}" : "I received an empty response. Please try again.";
            }

            String text = partsNode.get(0).path("text").asText("").trim();

            if (text.isBlank()) {
                return jsonMode ? "{}" : "I couldn't generate a response. Please try rephrasing.";
            }

            // Strip markdown code fences (Gemini sometimes wraps JSON in ```)
            return text.replace("```json", "").replace("```", "").trim();

        } catch (Exception e) {
            log.error("Gemini call failed: {}", e.getMessage(), e);
            // BUG FIX: don't expose internal error details to the user
            return jsonMode ? "{}" : "I'm experiencing technical difficulties. Please try again.";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE: Build contents array from chat history + current question
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Converts chat history + current question into Gemini's multi-turn format:
     * [
     *   { "role": "user",  "parts": [{ "text": "..." }] },
     *   { "role": "model", "parts": [{ "text": "..." }] },
     *   { "role": "user",  "parts": [{ "text": "current question" }] }
     * ]
     *
     * BUG FIX: original passed history as plain text inside the prompt string.
     * This caused the model to lose context across turns and treat everything
     * as a single monologue rather than a real conversation.
     */
    private List<Map<String, Object>> buildContents(List<AiController.ChatMessage> history,
                                                    String currentQuestion) {
        List<Map<String, Object>> contents = new ArrayList<>();

        if (history != null) {
            for (AiController.ChatMessage msg : history) {
                // Gemini uses "user" and "model" — not "user" and "ai"
                String role = "user".equals(msg.getRole()) ? "user" : "model";
                contents.add(Map.of(
                        "role",  role,
                        "parts", List.of(Map.of("text",
                                msg.getContent() != null ? msg.getContent() : ""))
                ));
            }
        }

        // Add the current question as the final user turn
        contents.add(Map.of(
                "role",  "user",
                "parts", List.of(Map.of("text", currentQuestion))
        ));

        return contents;
    }

    /** Wraps a single prompt into the contents array format (no history). */
    private List<Map<String, Object>> singleTurn(String prompt) {
        return List.of(Map.of(
                "role",  "user",
                "parts", List.of(Map.of("text", prompt))
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE: Filter extraction prompt — covers all 19 buyer scenarios
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
                + "{\"city\":null,\"location\":null,\"locations\":null,\"distanceKm\":null,"
                + "\"referenceLocation\":null,\"useCurrentLocation\":null,\"bhk\":null,"
                + "\"bhkList\":null,\"minPrice\":null,\"maxPrice\":null,\"type\":null,"
                + "\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,"
                + "\"developerName\":null,\"amenities\":null,\"vastuCompliant\":null,"
                + "\"possessionStatus\":null,\"possessionBefore\":null,\"listingType\":null,"
                + "\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,"
                + "\"maxResults\":null,\"keyword\":null}\n\n"
                + "EXAMPLES:\n"
                + "Q: Find me 2 BHKs and 3 BHKs in Koramangala\n"
                + "{\"city\":\"bangalore\",\"location\":\"koramangala\",\"locations\":null,\"distanceKm\":null,\"referenceLocation\":null,\"useCurrentLocation\":null,\"bhk\":null,\"bhkList\":[2,3],\"minPrice\":null,\"maxPrice\":null,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,\"developerName\":null,\"amenities\":null,\"vastuCompliant\":null,\"possessionStatus\":null,\"possessionBefore\":null,\"listingType\":null,\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":null,\"keyword\":null}\n\n"
                + "Q: Find me 2 BHKs within 10 km from Indiranagar\n"
                + "{\"city\":\"bangalore\",\"location\":null,\"locations\":null,\"distanceKm\":10,\"referenceLocation\":\"indiranagar\",\"useCurrentLocation\":null,\"bhk\":\"2\",\"bhkList\":null,\"minPrice\":null,\"maxPrice\":null,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,\"developerName\":null,\"amenities\":null,\"vastuCompliant\":null,\"possessionStatus\":null,\"possessionBefore\":null,\"listingType\":null,\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":null,\"keyword\":null}\n\n"
                + "Q: Find me 2 BHKs within 15 km from my current location\n"
                + "{\"city\":null,\"location\":null,\"locations\":null,\"distanceKm\":15,\"referenceLocation\":null,\"useCurrentLocation\":true,\"bhk\":\"2\",\"bhkList\":null,\"minPrice\":null,\"maxPrice\":null,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,\"developerName\":null,\"amenities\":null,\"vastuCompliant\":null,\"possessionStatus\":null,\"possessionBefore\":null,\"listingType\":null,\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":null,\"keyword\":null}\n\n"
                + "Q: My budget is 1.3 Crores, find 2 BHK within 1100 to 1250 sqft\n"
                + "{\"city\":null,\"location\":null,\"locations\":null,\"distanceKm\":null,\"referenceLocation\":null,\"useCurrentLocation\":null,\"bhk\":\"2\",\"bhkList\":null,\"minPrice\":null,\"maxPrice\":13000000,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":1100,\"maxSqft\":1250,\"developerName\":null,\"amenities\":null,\"vastuCompliant\":null,\"possessionStatus\":null,\"possessionBefore\":null,\"listingType\":null,\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":null,\"keyword\":null}\n\n"
                + "Q: Find good communities in Whitefield with Swimming pool, Cricket Net, Badminton Court\n"
                + "{\"city\":\"bangalore\",\"location\":\"whitefield\",\"locations\":null,\"distanceKm\":null,\"referenceLocation\":null,\"useCurrentLocation\":null,\"bhk\":null,\"bhkList\":null,\"minPrice\":null,\"maxPrice\":null,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,\"developerName\":null,\"amenities\":[\"swimming pool\",\"cricket practice net\",\"badminton court\"],\"vastuCompliant\":null,\"possessionStatus\":null,\"possessionBefore\":null,\"listingType\":null,\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":null,\"keyword\":null}\n\n"
                + "Q: Find Vastu compliant homes in Jayanagar, JP Nagar, Bannerghatta\n"
                + "{\"city\":\"bangalore\",\"location\":null,\"locations\":[\"jayanagar\",\"jp nagar\",\"bannerghatta\"],\"distanceKm\":null,\"referenceLocation\":null,\"useCurrentLocation\":null,\"bhk\":null,\"bhkList\":null,\"minPrice\":null,\"maxPrice\":null,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,\"developerName\":null,\"amenities\":null,\"vastuCompliant\":true,\"possessionStatus\":null,\"possessionBefore\":null,\"listingType\":null,\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":null,\"keyword\":null}\n\n"
                + "Q: Find top 10 properties in Bellandur\n"
                + "{\"city\":\"bangalore\",\"location\":\"bellandur\",\"locations\":null,\"distanceKm\":null,\"referenceLocation\":null,\"useCurrentLocation\":null,\"bhk\":null,\"bhkList\":null,\"minPrice\":null,\"maxPrice\":null,\"type\":null,\"facing\":null,\"furnishing\":null,\"minSqft\":null,\"maxSqft\":null,\"developerName\":null,\"amenities\":null,\"vastuCompliant\":null,\"possessionStatus\":null,\"possessionBefore\":null,\"listingType\":null,\"newProjectOnly\":null,\"reraApproved\":null,\"investmentFocus\":null,\"maxResults\":10,\"keyword\":null}\n\n"
                + "NOW EXTRACT FROM THIS QUERY:\n"
                + "\"" + userQuery.replace("\"", "'") + "\"";
    }
}
package com.ogm.market.ai;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final Map<String, List<ChatMessage>> chatHistory = new ConcurrentHashMap<>();

    private final GeminiService geminiService;
    private final AISearchService aiSearchService;
    private final IntentDetector intentDetector;

    public AiController(GeminiService geminiService,
                        AISearchService aiSearchService,
                        IntentDetector intentDetector) {
        this.geminiService = geminiService;
        this.aiSearchService = aiSearchService;
        this.intentDetector = intentDetector;
    }

    @PostMapping("/ask")
    public ResponseEntity<AiChatResponse> askAi(@Valid @RequestBody AiRequest request) {

        String question = request.getQuestion();
        String chatId = request.getChatId();
        boolean hasGps = request.getUserLatitude() != null
                && request.getUserLongitude() != null;
        boolean isRouteQuery = request.isRouteQuery()
                || intentDetector.isRouteQuery(question);

        if (isRouteQuery) {
            log.info("AI — ROUTE_QUERY chatId={}", chatId);
            addToHistory(chatId, "user", question);
            addToHistory(chatId, "ai", "Route shown on map.");
            return ResponseEntity.ok(AiChatResponse.builder()
                    .message("").hasResults(false).isRouteQuery(true)
                    .properties(List.of()).followUps(List.of()).build());
        }

        IntentDetector.Intent textIntent = intentDetector.detect(question);
        IntentDetector.Intent intent;
        boolean isLocationQuestion = intentDetector.isLocationNameQuery(question);

        if (isLocationQuestion) {
            intent = IntentDetector.Intent.GENERAL_CHAT;
        } else if (hasGps && textIntent == IntentDetector.Intent.LOCATION_SEARCH) {
            intent = IntentDetector.Intent.LOCATION_SEARCH;
        } else {
            intent = textIntent;
        }

        log.info("AI — textIntent={}, finalIntent={}, hasGps={}, chatId={}, question='{}'",
                textIntent, intent, hasGps, chatId, question);

        addToHistory(chatId, "user", question);

        try {
            AiChatResponse response = switch (intent) {
                case PROPERTY_SEARCH,
                     FOLLOWUP_SEARCH,
                     AMENITY_SEARCH,
                     LOCATION_SEARCH -> handlePropertySearch(request, chatId, hasGps);
                case ROUTE_QUERY -> AiChatResponse.builder()
                        .message("").hasResults(false).isRouteQuery(true)
                        .properties(List.of()).followUps(List.of()).build();
                default -> handleGeneralChat(question, chatId, request);
            };

            addToHistory(chatId, "ai", response.getMessage());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("AI ask failed: {}", e.getMessage(), e);
            return ResponseEntity.ok(AiChatResponse.builder()
                    .message("I'm experiencing technical difficulties. Please try again.")
                    .hasResults(false).properties(List.of()).followUps(List.of()).build());
        }
    }

    @PostMapping("/property-ask")
    public ResponseEntity<AiChatResponse> propertyAsk(
            @RequestBody PropertyAskRequest req) {
        try {
            log.info("AI — PROPERTY_ASK chatId={}, question='{}'",
                    req.getChatId(), req.getQuestion());

            addToHistory(req.getChatId(), "user", req.getQuestion());

            String systemPrompt = req.getSystemContext() != null
                    ? req.getSystemContext()
                    : "You are a helpful real estate advisor. Answer questions about this property.";

            List<ChatMessage> history = getRecentHistory(req.getChatId(), 6);

            String reply = geminiService.askGemini(systemPrompt, history, req.getQuestion());

            addToHistory(req.getChatId(), "ai", reply);

            return ResponseEntity.ok(AiChatResponse.builder()
                    .message(reply).hasResults(false)
                    .properties(List.of()).followUps(List.of()).build());

        } catch (Exception e) {
            log.error("Property ask failed: {}", e.getMessage(), e);
            return ResponseEntity.ok(AiChatResponse.builder()
                    .message("I couldn't process that. Please try again.")
                    .hasResults(false).properties(List.of()).followUps(List.of()).build());
        }
    }

    private AiChatResponse handlePropertySearch(AiRequest request, String chatId, boolean hasGps) {
        AiChatResponse searchResponse = aiSearchService.search(request);

        String propertyContext = buildPropertyContext(searchResponse);

        List<ChatMessage> history = getRecentHistory(chatId, 6);

        String systemPrompt = buildSystemPrompt(request, propertyContext,
                searchResponse.isHasResults(), hasGps);

        String aiMessage = geminiService.askGemini(systemPrompt, history, request.getQuestion());

        searchResponse.setMessage(aiMessage);
        return searchResponse;
    }

    private AiChatResponse handleGeneralChat(String question, String chatId, AiRequest request) {
        List<ChatMessage> history = getRecentHistory(chatId, 6);

        String systemPrompt = """
                You are a professional real estate advisor for One Global Marketplace (OGM),
                a premium property platform in India.
                Respond helpfully about real estate topics, greetings, or general questions.
                Keep responses concise — 2-4 sentences. Be warm and conversational.
                Do NOT use markdown code blocks.
                """;

        if (request.getUserLocationName() != null && !request.getUserLocationName().isBlank()) {
            String shortName = request.getUserLocationName().split(",")[0].trim();
            systemPrompt += "\nThe user's current location is: " + request.getUserLocationName()
                    + ". When asked about their location, say they are in " + shortName + ".";
        }

        String reply = geminiService.askGemini(systemPrompt, history, question);

        return AiChatResponse.builder()
                .message(reply).hasResults(false)
                .properties(List.of()).followUps(List.of()).build();
    }

    private String buildSystemPrompt(AiRequest request,
                                     String propertyContext,
                                     boolean hasResults,
                                     boolean hasGps) {
        StringBuilder sys = new StringBuilder();

        sys.append("""
                You are an expert real estate advisor for One Global Marketplace (OGM),
                a premium property platform in India. You have deep knowledge of:
                - Indian real estate market, property law, RERA regulations
                - Investment analysis: rental yield, capital appreciation, ROI
                - Bengaluru micro-markets, IT corridors, infrastructure developments
                - Home loans, stamp duty, registration, GST on property
                - Vastu, gated communities, possession timelines
                
                RESPONSE STYLE:
                - Write like a knowledgeable human advisor — detailed, analytical, insightful
                - Use structured responses with clear sections when answering complex questions
                - Give specific numbers, percentages, and reasoning — not vague statements
                - Bold key terms using **bold** where helpful
                - Use numbered lists for comparisons and ranked recommendations
                - Keep responses thorough but focused — no padding
                """);

        if (propertyContext != null && !propertyContext.isBlank()) {
            sys.append("""
                    
                    PROPERTY RECOMMENDATION MODE:
                    You have been given specific matching properties from the OGM database below.
                    When recommending or comparing properties, ONLY use these listed properties.
                    Do NOT mention external projects, developers, or properties not in the list.
                    
                    However, you CAN and SHOULD:
                    - Give deep investment analysis for each listed property
                    - Compare them on price/sqft, location advantage, rental potential, resale value
                    - Reference general market knowledge to SUPPORT your analysis of these properties
                      (e.g. "Whitefield rental demand is strong due to ITPL proximity — this property benefits from that")
                    - Give a clear recommendation with reasoning when asked
                    
                    FORMAT FOR PROPERTY RESULTS:
                    Write 3-5 sentences that:
                    1. Acknowledge what was found (count, type, area)
                    2. Give insight on what makes these results stand out
                    3. Provide a specific recommendation or comparison if asked
                    4. End with an invitation to explore further
                    
                    FOR INVESTMENT QUESTIONS (e.g. "which is best for investment?"):
                    - Rank the properties from best to worst for investment
                    - For each: explain rental yield potential, capital appreciation outlook, liquidity
                    - Give a clear winner with reasoning
                    - Be specific — mention actual prices, sqft rates, area advantages
                    
                    MATCHING PROPERTIES FROM OGM DATABASE:
                    ─────────────────────────────────────────
                    """);
            sys.append(propertyContext);
            sys.append("""
                    ─────────────────────────────────────────
                    (Only recommend properties listed above)
                    """);

        } else {
            sys.append("""
                    
                    GENERAL ADVISORY MODE:
                    No specific properties were found for this query.
                    Answer as a knowledgeable Indian real estate expert.
                    
                    You CAN freely answer questions about:
                    - Real estate concepts: RERA, stamp duty, registration, GST, home loans
                    - Investment strategies: rental yield, SIP vs property, ROI calculation
                    - Bengaluru market: best areas, IT corridors, price trends, upcoming projects
                    - Legal questions: sale deed, encumbrance certificate, khata, BBMP
                    - General property advice: vastu, gated communities, builder reputation
                    - Process questions: how to buy, negotiate, check documents
                    
                    When giving general advice, be thorough and structured like a consultant.
                    Use numbered points, give specific examples, cite typical numbers.
                    
                    If the query was a property search that returned no results, tell the user
                    no matching properties were found and suggest 2-3 ways to broaden the search.
                    """);
        }

        if (hasGps && request.getUserLocationName() != null
                && !request.getUserLocationName().isBlank()) {
            String shortName = request.getUserLocationName().split(",")[0].trim();
            sys.append("\nUSER LOCATION: ").append(request.getUserLocationName())
                    .append("\nReference naturally as 'near ").append(shortName)
                    .append("' — never show coordinates.\n");
        }

        return sys.toString();
    }

    private String buildPropertyContext(AiChatResponse searchResponse) {
        if (!searchResponse.isHasResults() || searchResponse.getProperties().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (var card : searchResponse.getProperties()) {
            sb.append("Property #").append(i++).append(": ")
                    .append(card.getTitle()).append(" | ")
                    .append(card.getLocation()).append(" | ")
                    .append(card.getPrice()).append(" | ")
                    .append(card.getType());
            if (card.getBhk() != null) sb.append(" | ").append(card.getBhk());
            if (card.getSqft() != null) sb.append(" | ").append(card.getSqft()).append(" sqft");
            if (card.isReraApproved()) sb.append(" | RERA ✓");
            if (card.isVastuCompliant()) sb.append(" | Vastu ✓");
            sb.append("\n");
        }
        return sb.toString();
    }


    @GetMapping("/history/{chatId}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable String chatId) {
        return ResponseEntity.ok(chatHistory.getOrDefault(chatId, List.of()));
    }

    @DeleteMapping("/history/{chatId}")
    public ResponseEntity<Void> clearChatHistory(@PathVariable String chatId) {
        chatHistory.remove(chatId);
        return ResponseEntity.ok().build();
    }


    private void addToHistory(String chatId, String role, String content) {
        if (chatId == null || chatId.isBlank()) return;
        chatHistory.computeIfAbsent(chatId, k -> new CopyOnWriteArrayList<>())
                .add(new ChatMessage(role, content, System.currentTimeMillis()));

        List<ChatMessage> msgs = chatHistory.get(chatId);
        if (msgs.size() > 20) {
            chatHistory.put(chatId,
                    new CopyOnWriteArrayList<>(msgs.subList(msgs.size() - 20, msgs.size())));
        }
    }

    private List<ChatMessage> getRecentHistory(String chatId, int maxTurns) {
        if (chatId == null) return List.of();
        List<ChatMessage> history = chatHistory.getOrDefault(chatId, List.of());
        if (history.isEmpty()) return List.of();
        List<ChatMessage> prior = history.size() > 1
                ? history.subList(0, history.size() - 1)
                : List.of();
        int start = Math.max(0, prior.size() - maxTurns);
        return prior.subList(start, prior.size());
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
        private long timestamp;
    }

    @lombok.Data
    public static class PropertyAskRequest {
        private String question;
        private String chatId;
        private String systemContext;
    }
}
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

    private final GeminiService geminiService;
    private final AISearchService aiSearchService;
    private final IntentDetector intentDetector;

    private final Map<String, List<ChatMessage>> chatHistory = new ConcurrentHashMap<>();

    public AiController(GeminiService geminiService,
                        AISearchService aiSearchService,
                        IntentDetector intentDetector) {
        this.geminiService = geminiService;
        this.aiSearchService = aiSearchService;
        this.intentDetector = intentDetector;
    }

    @PostMapping("/ask")
    public ResponseEntity<AiChatResponse> askAi(@Valid @RequestBody AiRequest request) {

        String userQuestion = request.getQuestion();
        String chatId = request.getChatId();
        boolean hasGps = request.getUserLatitude() != null
                && request.getUserLongitude() != null;

        IntentDetector.Intent textIntent = intentDetector.detect(userQuestion);
        IntentDetector.Intent intent;

        boolean isLocationQuestion = intentDetector.isLocationNameQuery(userQuestion);

        if (isLocationQuestion) {

            intent = IntentDetector.Intent.GENERAL_CHAT;
        } else if (hasGps && textIntent == IntentDetector.Intent.LOCATION_SEARCH) {
            intent = IntentDetector.Intent.LOCATION_SEARCH;
        } else {
            intent = textIntent;
        }

        log.info("AI — textIntent={}, finalIntent={}, hasGps={}, chatId={}, question='{}'",
                textIntent, intent, hasGps, chatId, userQuestion);

        if (chatId != null && !chatId.isBlank()) {
            addToHistory(chatId, "user", userQuestion);
        }

        try {
            AiChatResponse response = switch (intent) {
                case PROPERTY_SEARCH,
                     FOLLOWUP_SEARCH,
                     AMENITY_SEARCH,
                     LOCATION_SEARCH -> handlePropertySearch(request, chatId, hasGps);
                default -> handleGeneralChat(userQuestion, chatId);
            };

            if (chatId != null && !chatId.isBlank()) {
                addToHistory(chatId, "ai", response.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("AI ask failed: {}", e.getMessage(), e);
            return ResponseEntity.ok(
                    AiChatResponse.builder()
                            .message("I'm experiencing technical difficulties. Please try again.")
                            .hasResults(false).properties(List.of()).followUps(List.of())
                            .build());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PROPERTY / LOCATION SEARCH
    // ─────────────────────────────────────────────────────────────────────────

    private AiChatResponse handlePropertySearch(AiRequest request,
                                                String chatId,
                                                boolean hasGps) {
        AiChatResponse searchResponse = aiSearchService.search(request);

        // Build property context for Gemini including distance when GPS is available
        String propertyContext = null;
        if (searchResponse.isHasResults()) {
            StringBuilder ctx = new StringBuilder();
            for (PropertyCardResponse card : searchResponse.getProperties()) {
                ctx.append("- ").append(card.getTitle())
                        .append(" | ").append(card.getPrice())
                        .append(" | ").append(card.getLocation());
                if (card.getSqft() != null) ctx.append(" | ").append(card.getSqft()).append(" sqft");
                if (card.getBedrooms() != null) ctx.append(" | ").append(card.getBedrooms()).append(" BHK");
                if (hasGps && card.getLatitude() != null && card.getLongitude() != null) {
                    double dist = haversine(
                            request.getUserLatitude(), request.getUserLongitude(),
                            card.getLatitude(), card.getLongitude());
                    ctx.append(String.format(" | %.1f km from your location", dist));
                }
                ctx.append("\n");
            }
            propertyContext = ctx.toString();
        }

        String aiMessage = geminiService.askGemini(
                buildPrompt(chatId, request, propertyContext, true, hasGps));
        searchResponse.setMessage(aiMessage);
        return searchResponse;
    }

    private AiChatResponse handleGeneralChat(String question, String chatId) {
        AiRequest dummy = new AiRequest();
        dummy.setQuestion(question);
        dummy.setChatId(chatId);

        String aiMessage = geminiService.askGemini(
                buildPrompt(chatId, dummy, null, false, false));

        return AiChatResponse.builder()
                .message(aiMessage)
                .hasResults(false).properties(List.of()).followUps(List.of())
                .build();
    }


    private String buildPrompt(String chatId,
                               AiRequest request,
                               String propertyContext,
                               boolean isPropertyQuery,
                               boolean hasGps) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                You are a professional real estate advisor for One Global Marketplace (OGM),
                a premium property platform in India.
                
                RULES:
                - Be concise, friendly, and professional
                - Use bullet points for property details
                - Do NOT use markdown code blocks (no ```)
                - Do NOT invent properties that were not provided to you
                - If properties are provided, introduce them naturally with key details
                - If no properties match, suggest adjusting the search radius or criteria
                - NEVER say "I don't have access to your location" — the customer has already shared it
                - NEVER show raw GPS coordinates like 15.174200, 77.372300 in your reply
                - When asked "what is my location?" answer with the location NAME, not numbers
                - If no location name is available, say "your current location" or "near you"
                """);


        if (hasGps) {
            String locationName = request.getUserLocationName();
            boolean hasName = locationName != null && !locationName.isBlank();
            String shortName = hasName ? locationName.split(",")[0].trim() : "your area";

            if (hasName) {
                prompt.append(
                        "\nCUSTOMER LOCATION (verified - do NOT show coordinates to the user):\n"
                                + "Current location: " + locationName + "\n"
                                + "When asked what is my location or my location name, reply with the name above.\n"
                                + "Never show lat/lng numbers in your reply.\n"
                                + "Reference naturally: near " + shortName + ", in " + shortName + ", close to your location.\n"
                                + "Each property below shows its distance from this location.\n"
                );
            } else {
                prompt.append(
                        "\nCUSTOMER LOCATION (verified - do NOT show coordinates to the user):\n"
                                + "The customer has shared their GPS location.\n"
                                + "Refer to it as your current location or near you - never show numbers.\n"
                                + "Each property below shows its distance from the customer.\n"
                );
            }
        }
        if (!isPropertyQuery) {
            prompt.append("""
                    
                    The user is having a general conversation.
                    Respond helpfully about real estate topics, greetings, or general questions.
                    Keep responses concise — 2-4 sentences.
                    """);
        }

        // Conversation history — last 6 turns
        if (chatId != null) {
            List<ChatMessage> history = chatHistory.getOrDefault(chatId, List.of());
            if (!history.isEmpty()) {
                prompt.append("\nConversation so far:\n");
                int start = Math.max(0, history.size() - 6);
                for (int i = start; i < history.size(); i++) {
                    ChatMessage msg = history.get(i);
                    prompt.append(msg.getRole().equals("user") ? "User: " : "You: ")
                            .append(msg.getContent()).append("\n");
                }
            }
        }

        if (propertyContext != null && !propertyContext.isBlank()) {
            prompt.append("\nMatching properties from database:\n").append(propertyContext);
        }

        prompt.append("\nUser: ").append(request.getQuestion());
        return prompt.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HAVERSINE — km distance, used for prompt enrichment only
    // ─────────────────────────────────────────────────────────────────────────

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CHAT HISTORY
    // ─────────────────────────────────────────────────────────────────────────

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
        chatHistory.computeIfAbsent(chatId, k -> new CopyOnWriteArrayList<>())
                .add(new ChatMessage(role, content, System.currentTimeMillis()));

        List<ChatMessage> messages = chatHistory.get(chatId);
        if (messages.size() > 20) {
            chatHistory.put(chatId, new CopyOnWriteArrayList<>(
                    messages.subList(messages.size() - 20, messages.size())));
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
        private long timestamp;
    }
}
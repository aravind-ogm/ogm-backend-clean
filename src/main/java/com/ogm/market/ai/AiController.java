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

    /** In-memory chat history — for production, use Redis or DB */
    private final Map<String, List<ChatMessage>> chatHistory = new ConcurrentHashMap<>();

    public AiController(GeminiService geminiService,
                        AISearchService aiSearchService,
                        IntentDetector intentDetector) {
        this.geminiService = geminiService;
        this.aiSearchService = aiSearchService;
        this.intentDetector = intentDetector;
    }

    /* ═══════════════════════════════════════
       MAIN AI ENDPOINT
       ═══════════════════════════════════════ */

    @PostMapping("/ask")
    public ResponseEntity<AiChatResponse> askAi(@Valid @RequestBody AiRequest request) {

        String userQuestion = request.getQuestion();
        String chatId = request.getChatId();

        // ── Step 1: Detect intent ──
        IntentDetector.Intent intent = intentDetector.detect(userQuestion);
        log.info("AI — intent: {}, chatId: {}, question: '{}'", intent, chatId, userQuestion);

        // Save user message
        if (chatId != null && !chatId.isBlank()) {
            addToHistory(chatId, "user", userQuestion);
        }

        try {
            AiChatResponse response;

            switch (intent) {

                case PROPERTY_SEARCH:
                case FOLLOWUP_SEARCH:
                case AMENITY_SEARCH:
                    response = handlePropertySearch(userQuestion, chatId);
                    break;

                case GENERAL_CHAT:
                default:
                    response = handleGeneralChat(userQuestion, chatId);
                    break;
            }

            // Save AI response
            if (chatId != null && !chatId.isBlank()) {
                addToHistory(chatId, "ai", response.getMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("AI Ask failed: {}", e.getMessage(), e);
            return ResponseEntity.ok(
                    AiChatResponse.builder()
                            .message("I apologize, but I'm experiencing technical difficulties. Please try again.")
                            .hasResults(false)
                            .properties(List.of())
                            .followUps(List.of())
                            .build()
            );
        }
    }

    /* ═══════════════════════════════════════
       PROPERTY SEARCH — returns properties + AI message
       ═══════════════════════════════════════ */

    private AiChatResponse handlePropertySearch(String question, String chatId) {

        // Search for properties
        AiChatResponse searchResponse = aiSearchService.search(question);

        // Generate AI message with property context
        String prompt;
        if (searchResponse.isHasResults()) {
            StringBuilder context = new StringBuilder();
            for (PropertyCardResponse card : searchResponse.getProperties()) {
                context.append("- ").append(card.getTitle())
                        .append(" | ").append(card.getPrice())
                        .append(" | ").append(card.getLocation());
                if (card.getSqft() != null) context.append(" | ").append(card.getSqft()).append(" sqft");
                if (card.getBedrooms() != null) context.append(" | ").append(card.getBedrooms()).append(" BHK");
                context.append("\n");
            }

            prompt = buildPrompt(chatId, question, context.toString(), true);
        } else {
            prompt = buildPrompt(chatId, question, null, true);
        }

        String aiMessage = geminiService.askGemini(prompt);
        searchResponse.setMessage(aiMessage);

        return searchResponse;
    }

    /* ═══════════════════════════════════════
       GENERAL CHAT — NO property search, just conversation
       ═══════════════════════════════════════ */

    private AiChatResponse handleGeneralChat(String question, String chatId) {

        String prompt = buildPrompt(chatId, question, null, false);
        String aiMessage = geminiService.askGemini(prompt);

        return AiChatResponse.builder()
                .message(aiMessage)
                .hasResults(false)
                .properties(List.of())
                .followUps(List.of())
                .build();
    }

    /* ═══════════════════════════════════════
       PROMPT BUILDER
       ═══════════════════════════════════════ */

    private String buildPrompt(String chatId, String question,
                               String propertyContext, boolean isPropertyQuery) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                You are a professional real estate advisor for One Global Marketplace (OGM), 
                a premium property platform in India.
                
                RULES:
                - Be concise, friendly, and professional
                - Use bullet points for property details
                - Do NOT use markdown code blocks (no ```)
                - Do NOT invent properties that weren't provided to you
                - If properties are provided, introduce them naturally with key details
                - If no properties match, suggest adjusting criteria
                """);

        if (!isPropertyQuery) {
            prompt.append("""
                    
                    The user is having a general conversation (NOT searching for properties).
                    Respond helpfully about real estate topics, greetings, or general questions.
                    Do NOT mention any specific properties unless the user asks.
                    Keep responses concise — 2-4 sentences for simple questions.
                    """);
        }

        // Add conversation history
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
            prompt.append("\nMatching properties from database:\n")
                    .append(propertyContext);
        }

        prompt.append("\nUser: ").append(question);

        return prompt.toString();
    }

    /* ═══════════════════════════════════════
       CHAT HISTORY
       ═══════════════════════════════════════ */

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
        chatHistory
                .computeIfAbsent(chatId, k -> new CopyOnWriteArrayList<>())
                .add(new ChatMessage(role, content, System.currentTimeMillis()));

        List<ChatMessage> messages = chatHistory.get(chatId);
        if (messages.size() > 20) {
            chatHistory.put(chatId, new CopyOnWriteArrayList<>(
                    messages.subList(messages.size() - 20, messages.size())
            ));
        }
    }

    /* ═══════════════════════════════════════
       CHAT MESSAGE DTO
       ═══════════════════════════════════════ */

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
        private long timestamp;
    }
}
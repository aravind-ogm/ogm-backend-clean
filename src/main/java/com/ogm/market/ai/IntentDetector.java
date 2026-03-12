package com.ogm.market.ai;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Determines user intent from their message — no AI call needed.
 * Fast, reliable, and free.
 */
@Component
public class IntentDetector {

    public enum Intent {
        PROPERTY_SEARCH,   // User wants to find/see properties
        GENERAL_CHAT,      // Greetings, questions about real estate concepts, thanks, etc.
        FOLLOWUP_SEARCH    // "show me cheaper", "what about villas" — needs previous context
    }

    /* ── Patterns that indicate property search intent ── */
    private static final Pattern SEARCH_PATTERN = Pattern.compile(
            "(?i)(" +
                    // Direct search phrases
                    "show\\s+me|find\\s+me|search|looking\\s+for|" +
                    "i\\s+want|i\\s+need|suggest|recommend|" +
                    "any\\s+(property|flat|villa|apartment|plot|house|home)|" +
                    "available|option|listing|" +

                    // BHK / property type signals
                    "\\d+\\s*bhk|villa|apartment|flat|plot|penthouse|duplex|" +
                    "farmhouse|row\\s*house|commercial|office|shop|" +

                    // Price signals
                    "\\d+\\s*(cr|crore|lakh|lac|lakhs)|under\\s+\\d|budget|" +

                    // Location signals with property context
                    "(?:in|at|near)\\s+\\w+.*(property|flat|bhk|villa|apartment|home|house|budget|price|under|cr|lakh)|" +
                    "(?:property|flat|bhk|villa|apartment|home|house).*(in|at|near)\\s+\\w+|" +

                    // Specific property queries
                    "send\\s+me|get\\s+me|give\\s+me.*(?:property|detail|list|option)|" +
                    "ready\\s+to\\s+move|new\\s+launch|upcoming|pre\\s*launch|" +
                    "rera\\s+approved\\s+(?:property|flat|villa|project)|" +
                    "gated\\s+community|" +

                    // Price comparison
                    "cheapest|affordable|premium|luxury|" +
                    "below|above|between.*(?:lakh|cr|crore)" +
                    ")"
    );

    /* ── Patterns that indicate follow-up (needs previous context) ── */
    private static final Pattern FOLLOWUP_PATTERN = Pattern.compile(
            "(?i)(" +
                    "show\\s+(?:me\\s+)?(?:more|similar|cheaper|expensive|bigger|smaller)|" +
                    "what\\s+about|how\\s+about|any\\s+other|" +
                    "compare|alternative|instead|" +
                    "(?:more|other|different)\\s+option|" +
                    "increase.*budget|decrease.*budget|" +
                    "(?:higher|lower)\\s+(?:price|budget)|" +
                    "near(?:by|er)|farther|" +
                    "with\\s+(?:more|less|bigger|smaller)|" +
                    "watchlist|report|shortlist" +
                    ")"
    );

    /* ── Pure chat signals — greetings, questions, thanks ── */
    private static final Set<String> GREETING_WORDS = Set.of(
            "hi", "hello", "hey", "helo", "good morning", "good afternoon",
            "good evening", "thanks", "thank you", "thank", "ok", "okay",
            "bye", "goodbye", "see you", "great", "nice", "cool", "awesome",
            "got it", "understood", "sure", "yes", "no", "nope", "yep"
    );

    private static final Pattern QUESTION_PATTERN = Pattern.compile(
            "(?i)^(what\\s+is|what\\s+are|how\\s+(?:does|do|is|are|can|to)|" +
                    "why\\s+(?:is|are|do|does|should)|" +
                    "explain|tell\\s+me\\s+about|" +
                    "difference\\s+between|meaning\\s+of|" +
                    "who\\s+(?:is|are)|when\\s+(?:is|was|will)|" +
                    "can\\s+you\\s+(?:explain|tell|help\\s+me\\s+understand)|" +
                    "is\\s+it\\s+(?:good|safe|worth|better)|" +
                    "should\\s+i|do\\s+i\\s+need|" +
                    "what.*(?:rera|stamp duty|registration|loan|emi|interest|tax|gst))"
    );

    /**
     * Detect intent from user message.
     */
    public Intent detect(String message) {
        if (message == null || message.isBlank()) {
            return Intent.GENERAL_CHAT;
        }

        String trimmed = message.trim().toLowerCase();

        // 1. Check if it's a pure greeting / short acknowledgment
        if (GREETING_WORDS.contains(trimmed) || trimmed.length() <= 3) {
            return Intent.GENERAL_CHAT;
        }

        // 2. Check if it's a general knowledge question (not property search)
        if (QUESTION_PATTERN.matcher(trimmed).find() && !SEARCH_PATTERN.matcher(trimmed).find()) {
            return Intent.GENERAL_CHAT;
        }

        // 3. Check if it's a follow-up referencing previous results
        if (FOLLOWUP_PATTERN.matcher(trimmed).find()) {
            return Intent.FOLLOWUP_SEARCH;
        }

        // 4. Check if it contains property search signals
        if (SEARCH_PATTERN.matcher(trimmed).find()) {
            return Intent.PROPERTY_SEARCH;
        }

        // 5. If message is short (< 5 words) and no search signal — likely chat
        if (trimmed.split("\\s+").length < 5) {
            return Intent.GENERAL_CHAT;
        }

        // 6. If it contains a location name with enough context, treat as search
        if (containsLocationName(trimmed) && trimmed.split("\\s+").length >= 3) {
            return Intent.PROPERTY_SEARCH;
        }

        // Default: general chat — DON'T search unless confident
        return Intent.GENERAL_CHAT;
    }

    private boolean containsLocationName(String query) {
        String[] locations = {
                "sarjapur", "whitefield", "electronic city", "bellandur", "varthur",
                "marathahalli", "koramangala", "indiranagar", "hsr layout", "btm layout",
                "jayanagar", "jp nagar", "bannerghatta", "hebbal", "yelahanka",
                "devanahalli", "thanisandra", "hennur", "horamavu", "kr puram",
                "hosur", "bagalur", "attibele", "chandapura", "kanakapura",
                "rajajinagar", "malleswaram", "sadashivanagar", "mg road",
                "manyata", "nagavara", "kasavanahalli", "gattahalli",
                "junnasandra", "haralur", "carmelaram", "kadugodi",
                "goa", "mumbai", "pune", "chennai", "hyderabad",
                "bali", "palacode", "nariyanahalli",
        };

        for (String loc : locations) {
            if (query.contains(loc)) return true;
        }
        return false;
    }
}
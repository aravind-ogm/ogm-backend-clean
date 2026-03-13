package com.ogm.market.ai;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class IntentDetector {

    public enum Intent {
        PROPERTY_SEARCH,
        AMENITY_SEARCH,     // "properties with swimming pool", "gym", "parking"
        GENERAL_CHAT,
        FOLLOWUP_SEARCH
    }

    /* Property search signals */
    private static final Pattern SEARCH_PATTERN = Pattern.compile(
            "(?i)(" +
                    "show\\s+me|find\\s+me|search|looking\\s+for|" +
                    "i\\s+want|i\\s+need|suggest|recommend|" +
                    "any\\s+(property|flat|villa|apartment|plot|house|home)|" +
                    "available|option|listing|" +
                    "\\d+\\s*bhk|villa|apartment|flat|plot|penthouse|duplex|" +
                    "farmhouse|row\\s*house|commercial|office|shop|" +
                    "\\d+\\s*(cr|crore|lakh|lac|lakhs)|under\\s+\\d|budget|" +
                    "(?:in|at|near)\\s+\\w+.*(property|flat|bhk|villa|apartment|home|house|budget|price|under|cr|lakh)|" +
                    "(?:property|flat|bhk|villa|apartment|home|house).*(in|at|near)\\s+\\w+|" +
                    "send\\s+me|get\\s+me|give\\s+me.*(?:property|detail|list|option)|" +
                    "ready\\s+to\\s+move|new\\s+launch|upcoming|pre\\s*launch|" +
                    "rera\\s+approved\\s+(?:property|flat|villa|project)|" +
                    "gated\\s+community|" +
                    "cheapest|affordable|premium|luxury|" +
                    "below|above|between.*(?:lakh|cr|crore)" +
                    ")"
    );

    /* Amenity/feature search signals */
    private static final Set<String> AMENITY_KEYWORDS = Set.of(
            "swimming pool", "pool", "gym", "gymnasium", "parking", "garden",
            "clubhouse", "club house", "playground", "play area", "kids area",
            "party hall", "community hall", "jogging track", "tennis court",
            "basketball", "badminton", "yoga", "spa", "sauna", "jacuzzi",
            "rooftop", "terrace", "balcony", "lift", "elevator", "security",
            "cctv", "intercom", "power backup", "generator", "water supply",
            "rain water", "solar", "ev charging", "electric vehicle",
            "pet friendly", "pets allowed", "co-working", "coworking",
            "library", "theatre", "theater", "amphitheatre", "multipurpose hall",
            "indoor games", "billiards", "table tennis", "squash",
            "senior citizen", "meditation", "temple", "walking track",
            "cycling track", "skating", "cricket", "football",
            "fire safety", "vastu", "feng shui",
            "sea view", "lake view", "mountain view", "city view", "green view",
            "near metro", "near school", "near hospital", "near mall",
            "near airport", "near highway", "near bus stop", "near railway"
    );

    private static final Pattern AMENITY_CONTEXT_PATTERN = Pattern.compile(
            "(?i)(with|having|has|include|including|that\\s+has|which\\s+has|" +
                    "want.*with|need.*with|looking.*with|find.*with|properties.*with|" +
                    "can\\s+you\\s+find)"
    );

    /* Follow-up signals */
    private static final Pattern FOLLOWUP_PATTERN = Pattern.compile(
            "(?i)(" +
                    "show\\s+(?:me\\s+)?(?:more|similar|cheaper|expensive|bigger|smaller)|" +
                    "what\\s+about|how\\s+about|any\\s+other|" +
                    "compare|alternative|instead|" +
                    "(?:more|other|different)\\s+option|" +
                    "increase.*budget|decrease.*budget|" +
                    "(?:higher|lower)\\s+(?:price|budget)|" +
                    "watchlist|report|shortlist" +
                    ")"
    );

    /* Pure chat signals */
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
        if (message == null || message.isBlank()) return Intent.GENERAL_CHAT;

        String trimmed = message.trim().toLowerCase();

        // 1. Pure greeting / short acknowledgment
        if (GREETING_WORDS.contains(trimmed) || trimmed.length() <= 3) {
            return Intent.GENERAL_CHAT;
        }

        // 2. Check for amenity/feature search — "with swimming pool", "has gym"
        if (containsAmenityKeyword(trimmed)) {
            return Intent.AMENITY_SEARCH;
        }

        // 3. General knowledge question (not property search)
        if (QUESTION_PATTERN.matcher(trimmed).find() && !SEARCH_PATTERN.matcher(trimmed).find()) {
            return Intent.GENERAL_CHAT;
        }

        // 4. Follow-up
        if (FOLLOWUP_PATTERN.matcher(trimmed).find()) {
            return Intent.FOLLOWUP_SEARCH;
        }

        // 5. Property search signals
        if (SEARCH_PATTERN.matcher(trimmed).find()) {
            return Intent.PROPERTY_SEARCH;
        }

        // 6. Short message with no search signal — likely chat
        if (trimmed.split("\\s+").length < 5) {
            return Intent.GENERAL_CHAT;
        }

        // 7. Contains location name with enough context
        if (containsLocationName(trimmed) && trimmed.split("\\s+").length >= 3) {
            return Intent.PROPERTY_SEARCH;
        }

        // Default: general chat
        return Intent.GENERAL_CHAT;
    }

    /**
     * Extract the amenity keyword from the message (if present)
     */
    public String extractAmenityKeyword(String message) {
        if (message == null) return null;
        String lower = message.toLowerCase();

        // Return the longest matching amenity keyword
        String best = null;
        for (String amenity : AMENITY_KEYWORDS) {
            if (lower.contains(amenity)) {
                if (best == null || amenity.length() > best.length()) {
                    best = amenity;
                }
            }
        }
        return best;
    }

    private boolean containsAmenityKeyword(String query) {
        // Must contain an amenity keyword
        boolean hasAmenity = false;
        for (String amenity : AMENITY_KEYWORDS) {
            if (query.contains(amenity)) {
                hasAmenity = true;
                break;
            }
        }
        if (!hasAmenity) return false;

        // And should have some search context (not just "what is a swimming pool?")
        return AMENITY_CONTEXT_PATTERN.matcher(query).find() ||
                SEARCH_PATTERN.matcher(query).find() ||
                query.contains("property") || query.contains("properties") ||
                query.contains("flat") || query.contains("villa") ||
                query.contains("apartment") || query.contains("home");
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
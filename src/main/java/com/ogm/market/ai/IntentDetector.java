package com.ogm.market.ai;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class IntentDetector {

    public enum Intent {
        PROPERTY_SEARCH,
        LOCATION_SEARCH,    // "near me", "nearby", "properties near my location"
        AMENITY_SEARCH,     // "properties with swimming pool", "gym", "parking"
        FOLLOWUP_SEARCH,
        ROUTE_QUERY,        // "distance between X and Y", "route from X to Y"
        GENERAL_CHAT
    }

    // ─── Route / distance signals — checked FIRST ─────────────────────────────
    // These must win over PROPERTY_SEARCH and LOCATION_SEARCH.
    private static final Pattern ROUTE_PATTERN = Pattern.compile(
            "(?i)(" +
                    // "distance between X and Y"
                    "distance\\s+between\\s+.+\\s+(and|to)\\s+.+|" +
                    // "route / directions / navigate from X to Y"
                    "(route|directions?|navigate|navigation|path)\\s+from\\s+.+\\s+to\\s+.+|" +
                    // "how far is X from Y"
                    "how\\s+far\\s+(is\\s+)?.*\\s+from\\s+.+|" +
                    // "X to Y distance / route"
                    ".+\\s+to\\s+.+\\s+(distance|route|directions?|km|kilometers?|miles?)|" +
                    // "travel time / commute / drive from X to Y"
                    "(travel\\s+time|commute|drive|driving)\\s+from\\s+.+\\s+to\\s+.+|" +
                    // "show route / directions X to Y"
                    "show\\s+(route|directions?|path)\\s+(from\\s+)?.+\\s+to\\s+.+|" +
                    // "find me route" / "get me directions"
                    "(find|get)\\s+me\\s+(route|directions?)\\s+(from\\s+)?.+|" +
                    // "time to reach X from Y"
                    "time\\s+to\\s+reach\\s+.+\\s+from\\s+.+" +
                    ")"
    );

    // ─── Proximity / location signals ─────────────────────────────────────────
    private static final Pattern PROXIMITY_PATTERN = Pattern.compile(
            "(?i)(" +
                    "near\\s+me|near\\s+by|nearby|" +
                    "my\\s+(current\\s+)?loc\\w+|" +
                    "what\\s+is\\s+my\\s+loc\\w+|" +
                    "where\\s+am\\s+i|" +
                    "from\\s+here|around\\s+me|close\\s+to\\s+me|" +
                    "properties\\s+(here|nearby|near\\s+me|around)|" +
                    "find\\s+.*(here|this\\s+area)|" +
                    "show\\s+.*(near(by)?|around\\s+me)|" +
                    "within\\s+\\d+\\s*km|\\d+\\s*km\\s+(from|near|around)" +
                    ")"
    );

    // ─── Property search signals ───────────────────────────────────────────────
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

    // ─── Amenity signals ───────────────────────────────────────────────────────
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

    // ─── Follow-up signals ─────────────────────────────────────────────────────
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

    // ─── Pure chat signals ─────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    //  MAIN DETECT
    // ─────────────────────────────────────────────────────────────────────────

    public Intent detect(String message) {
        if (message == null || message.isBlank()) return Intent.GENERAL_CHAT;

        String trimmed = message.trim().toLowerCase();

        // 1. Short greeting / acknowledgment
        if (GREETING_WORDS.contains(trimmed) || trimmed.length() <= 3) {
            return Intent.GENERAL_CHAT;
        }

        // 2. Route / distance — checked FIRST, before proximity and property search
        //    "distance between X and Y" must not fall through to PROPERTY_SEARCH
        if (ROUTE_PATTERN.matcher(trimmed).find()) {
            return Intent.ROUTE_QUERY;
        }

        // 3. Proximity / "near me"
        if (PROXIMITY_PATTERN.matcher(trimmed).find()) {
            return Intent.LOCATION_SEARCH;
        }

        // 4. Amenity search
        if (containsAmenityKeyword(trimmed)) {
            return Intent.AMENITY_SEARCH;
        }

        // 5. General knowledge question (not property search)
        if (QUESTION_PATTERN.matcher(trimmed).find()
                && !SEARCH_PATTERN.matcher(trimmed).find()) {
            return Intent.GENERAL_CHAT;
        }

        // 6. Follow-up
        if (FOLLOWUP_PATTERN.matcher(trimmed).find()) {
            return Intent.FOLLOWUP_SEARCH;
        }

        // 7. Explicit property search signals
        if (SEARCH_PATTERN.matcher(trimmed).find()) {
            return Intent.PROPERTY_SEARCH;
        }

        // 8. Short message with no search signal — likely chat
        if (trimmed.split("\\s+").length < 5) {
            return Intent.GENERAL_CHAT;
        }

        // 9. Contains known location name
        if (containsLocationName(trimmed) && trimmed.split("\\s+").length >= 3) {
            return Intent.PROPERTY_SEARCH;
        }

        return Intent.GENERAL_CHAT;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    public String extractAmenityKeyword(String message) {
        if (message == null) return null;
        String lower = message.toLowerCase();
        String best = null;
        for (String amenity : AMENITY_KEYWORDS) {
            if (lower.contains(amenity)) {
                if (best == null || amenity.length() > best.length()) best = amenity;
            }
        }
        return best;
    }

    public boolean isProximityQuery(String message) {
        if (message == null || message.isBlank()) return false;
        return PROXIMITY_PATTERN.matcher(message.toLowerCase()).find();
    }

    /** Returns true if the query is purely asking for distance/directions. */
    public boolean isRouteQuery(String message) {
        if (message == null || message.isBlank()) return false;
        return ROUTE_PATTERN.matcher(message.trim().toLowerCase()).find();
    }

    public boolean isLocationNameQuery(String message) {
        if (message == null || message.isBlank()) return false;
        String lower = message.trim().toLowerCase()
                .replaceAll("[?!.]", "").trim();

        if (lower.matches("(what is |tell me |show me |can you tell me )?my (current )?loc\\w*")) return true;
        if (lower.matches("(what is |where is )?my (current )?location( name)?")) return true;
        if (lower.matches("my location( name)?")) return true;
        if (lower.matches("where am i")) return true;
        if (lower.matches("(can you tell|tell) me (my|where i am|my current location).*")) return true;
        if (lower.matches("what.*my.*loc\\w*")) return true;

        boolean hasPropertySignal = SEARCH_PATTERN.matcher(lower).find()
                || lower.contains("property") || lower.contains("flat")
                || lower.contains("villa") || lower.contains("apartment")
                || lower.contains("bhk") || lower.contains("find");

        return !hasPropertySignal
                && (lower.contains("my location") || lower.contains("my loc")
                    || lower.contains("where am i"));
    }

    private boolean containsAmenityKeyword(String query) {
        boolean hasAmenity = AMENITY_KEYWORDS.stream().anyMatch(query::contains);
        if (!hasAmenity) return false;
        return AMENITY_CONTEXT_PATTERN.matcher(query).find()
                || SEARCH_PATTERN.matcher(query).find()
                || query.contains("property") || query.contains("properties")
                || query.contains("flat") || query.contains("villa")
                || query.contains("apartment") || query.contains("home");
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

package com.ogm.market.ai;

import com.ogm.market.model.Property;
import com.ogm.market.repository.PropertyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AISearchServiceImpl implements AISearchService {

    private static final Logger log = LoggerFactory.getLogger(AISearchServiceImpl.class);
    private static final int MAX_RESULTS = 6;

    private final PropertyRepository repository;
    private final EmbeddingService embeddingService;

    @Value("${storage.base-url:http://localhost:8080}")
    private String storageBaseUrl;

    public AISearchServiceImpl(PropertyRepository repository,
                               EmbeddingService embeddingService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
    }

    @Override
    public AiChatResponse search(String prompt) {

        if (prompt == null || prompt.isBlank()) {
            return emptyResponse("Please tell me what kind of property you're looking for.");
        }

        String query = prompt.toLowerCase().trim();

        // ── Extract structured filters ──
        Integer bhk = extractBhk(query);
        Double maxPrice = extractMaxPrice(query);
        Double minPrice = extractMinPrice(query);
        String location = extractLocation(query);

        log.info("AI Search — query: '{}', bhk: {}, maxPrice: {}, location: '{}'",
                query, bhk, maxPrice, location);

        // ── Search Strategy: try in order, stop when results found ──
        List<Property> results = Collections.emptyList();

        // 1. Hybrid search (vector + filters) — best quality
        if (results.isEmpty()) {
            results = tryHybridSearch(prompt, location, bhk, maxPrice);
        }

        // 2. Advanced text search with filters
        if (results.isEmpty()) {
            results = tryAdvancedSearch(query, null, minPrice, maxPrice, null, bhk, null, null);
        }

        // 3. Simple location + price fallback
        if (results.isEmpty() && location != null) {
            try {
                results = repository.findByLocationContainingIgnoreCaseAndPriceLessThanEqual(
                        location, maxPrice != null ? maxPrice : Double.MAX_VALUE
                );
            } catch (Exception e) {
                log.warn("Simple search failed: {}", e.getMessage());
            }
        }

        // 4. Pure semantic search — ONLY if user provided specific terms
        if (results.isEmpty() && hasSpecificSearchTerms(query)) {
            results = trySemanticSearch(prompt);
        }

        // ── NO random fallback! If nothing matches, say so. ──
        if (results.isEmpty()) {
            return emptyResponse(
                    "I couldn't find properties matching your criteria. " +
                            "Try adjusting your budget, location, or BHK. " +
                            "For example: \"2 BHK in Whitefield under 1 Cr\""
            );
        }

        // ── Build response ──
        List<PropertyCardResponse> cards = results.stream()
                .limit(MAX_RESULTS)
                .map(this::toCard)
                .collect(Collectors.toList());

        List<String> followUps = generateFollowUps(bhk, location, maxPrice, cards.size());

        return AiChatResponse.builder()
                .message("Here are the best matches I found:")
                .properties(cards)
                .hasResults(true)
                .followUps(followUps)
                .build();
    }

    /* ═══════════════════════════════════════
       SEARCH STRATEGIES
       ═══════════════════════════════════════ */

    private List<Property> tryHybridSearch(String prompt, String location,
                                           Integer bhk, Double maxPrice) {
        try {
            List<Double> vector = embeddingService.generateEmbedding(prompt);
            String pgVector = toPgVector(vector);
            List<Property> results = repository.hybridSearch(
                    pgVector, location, bhk, maxPrice, MAX_RESULTS
            );
            // Filter out irrelevant results — semantic search can return noise
            return filterRelevant(results, location, bhk, maxPrice);
        } catch (Exception e) {
            log.warn("Hybrid search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Property> tryAdvancedSearch(String q, String type, Double minPrice,
                                             Double maxPrice, Boolean rera, Integer bhk,
                                             String facing, String furnishing) {
        try {
            return repository.advancedSearch(
                    q, type, minPrice, maxPrice, rera, bhk, facing, furnishing,
                    PageRequest.of(0, MAX_RESULTS)
            ).getContent();
        } catch (Exception e) {
            log.warn("Advanced search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Property> trySemanticSearch(String prompt) {
        try {
            List<Double> vector = embeddingService.generateEmbedding(prompt);
            return repository.semanticSearch(toPgVector(vector), MAX_RESULTS);
        } catch (Exception e) {
            log.warn("Semantic search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Post-filter: remove results that clearly don't match user criteria.
     * Prevents returning a villa when user asked for 2 BHK flat.
     */
    private List<Property> filterRelevant(List<Property> results,
                                          String location, Integer bhk, Double maxPrice) {
        if (results == null || results.isEmpty()) return results;

        return results.stream()
                .filter(p -> {
                    // If user specified BHK, property must match (or be null)
                    if (bhk != null && p.getBedrooms() != null && !p.getBedrooms().equals(bhk)) {
                        return false;
                    }
                    // If user specified max price, property must be within range
                    if (maxPrice != null && p.getPrice() != null && p.getPrice() > maxPrice * 1.1) {
                        return false; // 10% tolerance
                    }
                    // If user specified location, property must contain it
                    if (location != null && p.getLocation() != null) {
                        return p.getLocation().toLowerCase().contains(location.toLowerCase());
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Check if the query has specific enough terms to warrant a semantic search.
     * Prevents "hello" or "thanks" from triggering a search.
     */
    private boolean hasSpecificSearchTerms(String query) {
        return query.matches(".*\\d+\\s*bhk.*") ||
                query.matches(".*\\d+.*(?:cr|crore|lakh|lac).*") ||
                query.contains("villa") || query.contains("apartment") ||
                query.contains("flat") || query.contains("plot") ||
                query.contains("property") || query.contains("house") ||
                query.contains("home") || extractLocation(query) != null;
    }

    /* ═══════════════════════════════════════
       NLP EXTRACTION
       ═══════════════════════════════════════ */

    private Integer extractBhk(String query) {
        Matcher m = Pattern.compile("(\\d+)\\s*bhk").matcher(query);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private Double extractMaxPrice(String query) {
        Matcher cr = Pattern.compile("(?:under|below|less than|budget|max|upto|within)\\s*(\\d+\\.?\\d*)\\s*(?:cr|crore)")
                .matcher(query);
        if (cr.find()) return Double.parseDouble(cr.group(1)) * 10_000_000;

        Matcher lk = Pattern.compile("(?:under|below|less than|budget|max|upto|within)\\s*(\\d+\\.?\\d*)\\s*(?:lakh|lac|lakhs|l\\b)")
                .matcher(query);
        if (lk.find()) return Double.parseDouble(lk.group(1)) * 100_000;

        // "X cr" without prefix
        Matcher crSimple = Pattern.compile("(\\d+\\.?\\d*)\\s*(?:cr|crore)").matcher(query);
        if (crSimple.find()) return Double.parseDouble(crSimple.group(1)) * 10_000_000;

        Matcher lkSimple = Pattern.compile("(\\d+\\.?\\d*)\\s*(?:lakh|lac|lakhs)").matcher(query);
        if (lkSimple.find()) return Double.parseDouble(lkSimple.group(1)) * 100_000;

        return null;
    }

    private Double extractMinPrice(String query) {
        Matcher cr = Pattern.compile("(?:above|over|more than|min|from|starting)\\s*(\\d+\\.?\\d*)\\s*(?:cr|crore)")
                .matcher(query);
        if (cr.find()) return Double.parseDouble(cr.group(1)) * 10_000_000;

        Matcher lk = Pattern.compile("(?:above|over|more than|min|from|starting)\\s*(\\d+\\.?\\d*)\\s*(?:lakh|lac|lakhs)")
                .matcher(query);
        if (lk.find()) return Double.parseDouble(lk.group(1)) * 100_000;

        return null;
    }

    private String extractLocation(String query) {
        String[] locations = {
                "sarjapur", "whitefield", "electronic city", "bellandur", "varthur",
                "marathahalli", "koramangala", "indiranagar", "hsr layout", "btm layout",
                "jayanagar", "jp nagar", "bannerghatta", "hebbal", "yelahanka",
                "devanahalli", "thanisandra", "hennur", "horamavu", "kr puram",
                "hosur", "bagalur", "attibele", "chandapura", "jigani",
                "kanakapura", "mysore road", "tumkur road", "old airport road",
                "outer ring road", "rajajinagar", "malleswaram", "sadashivanagar",
                "mg road", "manyata", "nagavara", "kasavanahalli", "gattahalli",
                "junnasandra", "haralur", "carmelaram", "kadugodi",
                "goa", "mumbai", "pune", "chennai", "hyderabad", "delhi",
                "noida", "gurgaon", "mysore", "mangalore", "coorg",
                "bali", "palacode", "nariyanahalli", "hosur road",
                "sobha", "brigade", "prestige", "godrej", "total environment",
        };

        String bestMatch = null;
        for (String loc : locations) {
            if (query.contains(loc)) {
                if (bestMatch == null || loc.length() > bestMatch.length()) {
                    bestMatch = loc;
                }
            }
        }

        // Try to extract after "in" / "at" / "near"
        if (bestMatch == null) {
            Matcher m = Pattern.compile("(?:in|at|near|around)\\s+([a-z][a-z\\s]{2,25}?)(?:\\s+(?:under|below|with|for|budget|\\d)|$)")
                    .matcher(query);
            if (m.find()) {
                bestMatch = m.group(1).trim();
            }
        }

        return bestMatch;
    }

    /* ═══════════════════════════════════════
       FOLLOW-UP SUGGESTIONS
       ═══════════════════════════════════════ */

    private List<String> generateFollowUps(Integer bhk, String location,
                                           Double maxPrice, int resultCount) {
        List<String> followUps = new ArrayList<>();

        if (resultCount > 1) followUps.add("Want to compare these properties?");

        if (maxPrice != null) {
            double higher = maxPrice * 1.5;
            String fmt = higher >= 10_000_000
                    ? String.format("%.1f Cr", higher / 10_000_000)
                    : String.format("%.0f L", higher / 100_000);
            followUps.add("Show options under " + fmt);
        }

        if (bhk != null && bhk < 4) {
            followUps.add("Show " + (bhk + 1) + " BHK options");
        }

        if (location != null) {
            followUps.add("More in " + capitalize(location));
        }

        return followUps.stream().limit(3).collect(Collectors.toList());
    }

    /* ═══════════════════════════════════════
       CARD MAPPING
       ═══════════════════════════════════════ */

    private PropertyCardResponse toCard(Property p) {
        return PropertyCardResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .price(p.getFormattedPrice())
                .location(p.getLocation())
                .type(p.getType())
                .sqft(p.getSqft())
                .bedrooms(p.getBedrooms())
                .bathrooms(p.getBathrooms())
                .facing(p.getFacing())
                .furnishing(p.getFurnishing())
                .description(p.getDescription())
                .primaryImage(prefixUrl(p.getPrimaryImage()))
                .gallery(prefixUrls(p.getImages()))
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .googleMapsUrl(p.getGoogleMapsUrl())
                .reraApproved(p.isReraApproved())
                .soldOut(p.isSoldOut())
                .amenities(p.getAmenities() != null ? p.getAmenities() : Collections.emptyList())
                .slug(p.getSlug())
                .build();
    }

    /* ═══════════════════════════════════════
       UTILITIES
       ═══════════════════════════════════════ */

    private String toPgVector(List<Double> vector) {
        return "[" + vector.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
    }

    private AiChatResponse emptyResponse(String message) {
        return AiChatResponse.builder()
                .message(message).hasResults(false)
                .properties(List.of()).followUps(List.of())
                .build();
    }

    private String prefixUrl(String url) {
        if (url == null || url.isBlank()) return null;
        if (url.startsWith("http")) return url;
        return storageBaseUrl + (url.startsWith("/") ? url : "/" + url);
    }

    private List<String> prefixUrls(List<String> urls) {
        if (urls == null) return Collections.emptyList();
        return urls.stream().map(this::prefixUrl).collect(Collectors.toList());
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
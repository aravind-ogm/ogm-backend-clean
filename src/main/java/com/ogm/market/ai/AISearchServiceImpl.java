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
    private final IntentDetector intentDetector;

    @Value("${storage.base-url:http://localhost:8080}")
    private String storageBaseUrl;

    public AISearchServiceImpl(PropertyRepository repository,
                               EmbeddingService embeddingService,
                               IntentDetector intentDetector) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.intentDetector = intentDetector;
    }

    @Override
    public AiChatResponse search(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return emptyResponse("Please tell me what kind of property you're looking for.");
        }

        String query = prompt.toLowerCase().trim();
        IntentDetector.Intent intent = intentDetector.detect(prompt);

        // Extract all filters
        Integer bhk = extractBhk(query);
        Double maxPrice = extractMaxPrice(query);
        Double minPrice = extractMinPrice(query);
        String location = extractLocation(query);
        String propertyType = extractPropertyType(query);
        Boolean rera = extractRera(query);
        String amenityKeyword = intentDetector.extractAmenityKeyword(query);

        // Also extract generic keywords for deep search
        List<String> keywords = extractKeywords(query);

        log.info("AI Search — intent: {}, type: '{}', bhk: {}, price: {}, location: '{}', amenity: '{}', keywords: {}",
                intent, propertyType, bhk, maxPrice, location, amenityKeyword, keywords);

        List<Property> results = Collections.emptyList();

        // ═══════════════════════════════════════
        // AMENITY SEARCH — "with swimming pool", "has gym"
        // ═══════════════════════════════════════
        if (intent == IntentDetector.Intent.AMENITY_SEARCH && amenityKeyword != null) {
            // 1. Direct amenity table search
            results = tryAmenitySearch(amenityKeyword, maxPrice, propertyType);

            // 2. Deep search across all fields (description, title, etc.)
            if (results.isEmpty()) {
                results = tryDeepSearch(amenityKeyword, maxPrice, propertyType);
            }

            // 3. Semantic search as fallback
            if (results.isEmpty()) {
                results = trySemanticSearch(prompt);
                results = filterByAmenityInMemory(results, amenityKeyword);
            }
        }

        // ═══════════════════════════════════════
        // PROPERTY SEARCH — standard filters
        // ═══════════════════════════════════════
        if (results.isEmpty()) {
            // 1. Hybrid search (vector + structured filters)
            results = tryHybridSearch(prompt, location, bhk, maxPrice, propertyType);

            // 2. Advanced text search
            if (results.isEmpty()) {
                results = tryAdvancedSearch(query, propertyType, minPrice, maxPrice, rera, bhk, null, null);
            }

            // 3. Deep search across ALL fields (title, location, description, amenities, type, facing, furnishing)
            if (results.isEmpty() && !keywords.isEmpty()) {
                for (String keyword : keywords) {
                    results = tryDeepSearch(keyword, maxPrice, propertyType);
                    if (!results.isEmpty()) break;
                }
            }

            // 4. Simple location + price
            if (results.isEmpty() && location != null) {
                try {
                    List<Property> locResults = repository.findByLocationContainingIgnoreCaseAndPriceLessThanEqual(
                            location, maxPrice != null ? maxPrice : Double.MAX_VALUE);
                    results = filterByType(locResults, propertyType);
                } catch (Exception e) {
                    log.warn("Simple search failed: {}", e.getMessage());
                }
            }

            // 5. Pure semantic search
            if (results.isEmpty() && hasSpecificSearchTerms(query)) {
                List<Property> semanticResults = trySemanticSearch(prompt);
                results = filterRelevant(semanticResults, location, bhk, maxPrice, propertyType);
            }
        }

        // No results
        if (results.isEmpty()) {
            String hint = amenityKeyword != null
                    ? "properties with " + amenityKeyword
                    : propertyType != null ? propertyType + "s matching your criteria" : "matching properties";
            return emptyResponse(
                    "I couldn't find " + hint + ". " +
                            "Try different amenities, adjust budget, or broaden your search."
            );
        }

        // Build response
        List<PropertyCardResponse> cards = results.stream()
                .limit(MAX_RESULTS)
                .map(this::toCard)
                .collect(Collectors.toList());

        List<String> followUps = generateFollowUps(bhk, location, maxPrice, amenityKeyword, cards.size());

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

    private List<Property> tryAmenitySearch(String amenity, Double maxPrice, String type) {
        try {
            return repository.searchByAmenity(amenity, maxPrice, type, MAX_RESULTS * 2);
        } catch (Exception e) {
            log.warn("Amenity search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Property> tryDeepSearch(String keyword, Double maxPrice, String type) {
        try {
            return repository.deepSearch(keyword, maxPrice, type, MAX_RESULTS * 2);
        } catch (Exception e) {
            log.warn("Deep search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Property> tryHybridSearch(String prompt, String location,
                                           Integer bhk, Double maxPrice, String propertyType) {
        try {
            List<Double> vector = embeddingService.generateEmbedding(prompt);
            String pgVector = toPgVector(vector);
            List<Property> results = repository.hybridSearch(
                    pgVector, location, bhk, maxPrice, propertyType, MAX_RESULTS * 2
            );
            return filterRelevant(results, location, bhk, maxPrice, propertyType);
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
     * In-memory filter: check if property amenities/description contain the keyword
     */
    private List<Property> filterByAmenityInMemory(List<Property> results, String amenity) {
        if (results == null || amenity == null) return results;
        String lower = amenity.toLowerCase();

        return results.stream()
                .filter(p -> {
                    // Check amenities list
                    if (p.getAmenities() != null) {
                        for (String a : p.getAmenities()) {
                            if (a.toLowerCase().contains(lower)) return true;
                        }
                    }
                    // Check description
                    if (p.getDescription() != null && p.getDescription().toLowerCase().contains(lower)) {
                        return true;
                    }
                    // Check title
                    if (p.getTitle() != null && p.getTitle().toLowerCase().contains(lower)) {
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    /* ═══════════════════════════════════════
       POST-FILTERS
       ═══════════════════════════════════════ */

    private List<Property> filterRelevant(List<Property> results, String location,
                                          Integer bhk, Double maxPrice, String propertyType) {
        if (results == null || results.isEmpty()) return results;

        return results.stream()
                .filter(p -> {
                    if (propertyType != null && p.getType() != null && !matchesType(p.getType(), propertyType))
                        return false;
                    if (bhk != null && p.getBedrooms() != null && !p.getBedrooms().equals(bhk))
                        return false;
                    if (maxPrice != null && p.getPrice() != null && p.getPrice() > maxPrice * 1.1)
                        return false;
                    if (location != null && p.getLocation() != null &&
                            !p.getLocation().toLowerCase().contains(location.toLowerCase()))
                        return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    private List<Property> filterByType(List<Property> results, String propertyType) {
        if (propertyType == null || results == null) return results;
        return results.stream()
                .filter(p -> p.getType() == null || matchesType(p.getType(), propertyType))
                .collect(Collectors.toList());
    }

    private boolean matchesType(String actual, String wanted) {
        String a = actual.toLowerCase();
        String w = wanted.toLowerCase();
        if (a.contains(w)) return true;

        return switch (w) {
            case "villa" -> a.contains("villa") || a.contains("bungalow") || a.contains("independent house") || a.contains("row house");
            case "apartment", "flat" -> a.contains("apartment") || a.contains("flat") || a.contains("residential building") || a.contains("condo");
            case "plot", "land" -> a.contains("plot") || a.contains("land") || a.contains("site");
            case "commercial" -> a.contains("commercial") || a.contains("office") || a.contains("shop");
            case "penthouse" -> a.contains("penthouse");
            case "duplex" -> a.contains("duplex");
            case "farmhouse" -> a.contains("farm");
            case "holiday" -> a.contains("holiday") || a.contains("vacation") || a.contains("resort");
            default -> a.contains(w);
        };
    }

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
        Matcher cr = Pattern.compile("(?:under|below|less than|budget|max|upto|within)\\s*(\\d+\\.?\\d*)\\s*(?:cr|crore)").matcher(query);
        if (cr.find()) return Double.parseDouble(cr.group(1)) * 10_000_000;
        Matcher lk = Pattern.compile("(?:under|below|less than|budget|max|upto|within)\\s*(\\d+\\.?\\d*)\\s*(?:lakh|lac|lakhs|l\\b)").matcher(query);
        if (lk.find()) return Double.parseDouble(lk.group(1)) * 100_000;
        Matcher crS = Pattern.compile("(\\d+\\.?\\d*)\\s*(?:cr|crore)").matcher(query);
        if (crS.find()) return Double.parseDouble(crS.group(1)) * 10_000_000;
        Matcher lkS = Pattern.compile("(\\d+\\.?\\d*)\\s*(?:lakh|lac|lakhs)").matcher(query);
        if (lkS.find()) return Double.parseDouble(lkS.group(1)) * 100_000;
        return null;
    }

    private Double extractMinPrice(String query) {
        Matcher cr = Pattern.compile("(?:above|over|more than|min|from|starting)\\s*(\\d+\\.?\\d*)\\s*(?:cr|crore)").matcher(query);
        if (cr.find()) return Double.parseDouble(cr.group(1)) * 10_000_000;
        Matcher lk = Pattern.compile("(?:above|over|more than|min|from|starting)\\s*(\\d+\\.?\\d*)\\s*(?:lakh|lac|lakhs)").matcher(query);
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
            if (query.contains(loc) && (bestMatch == null || loc.length() > bestMatch.length())) {
                bestMatch = loc;
            }
        }
        if (bestMatch == null) {
            Matcher m = Pattern.compile("(?:in|at|near|around)\\s+([a-z][a-z\\s]{2,25}?)(?:\\s+(?:under|below|with|for|budget|\\d)|$)").matcher(query);
            if (m.find()) bestMatch = m.group(1).trim();
        }
        return bestMatch;
    }

    private String extractPropertyType(String query) {
        if (query.contains("villa") || query.contains("villas")) return "villa";
        if (query.contains("apartment") || query.contains("apartments")) return "apartment";
        if (query.contains("flat") || query.contains("flats")) return "flat";
        if (query.contains("plot") || query.contains("plots") || query.contains("site")) return "plot";
        if (query.contains("land")) return "land";
        if (query.contains("penthouse")) return "penthouse";
        if (query.contains("duplex")) return "duplex";
        if (query.contains("row house") || query.contains("rowhouse") || query.contains("bungalow")) return "villa";
        if (query.contains("farmhouse") || query.contains("farm house")) return "farmhouse";
        if (query.contains("commercial") || query.contains("office") || query.contains("shop")) return "commercial";
        if (query.contains("holiday") || query.contains("vacation")) return "holiday";
        if (query.contains("independent house")) return "villa";
        return null;
    }

    private Boolean extractRera(String query) {
        return query.contains("rera") ? true : null;
    }

    /**
     * Extract meaningful keywords from query for deep search.
     * Removes stop words and common filler words.
     */
    private List<String> extractKeywords(String query) {
        Set<String> stopWords = Set.of(
                "can", "you", "find", "me", "show", "i", "want", "need", "a", "an", "the",
                "with", "in", "at", "near", "for", "and", "or", "of", "to", "is", "are",
                "please", "send", "get", "give", "looking", "search", "any", "some",
                "property", "properties", "bhk", "under", "below", "above", "budget",
                "cr", "crore", "lakh", "lakhs", "lac"
        );

        return Arrays.stream(query.split("\\s+"))
                .filter(w -> w.length() > 2)
                .filter(w -> !stopWords.contains(w))
                .filter(w -> !w.matches("\\d+"))
                .collect(Collectors.toList());
    }

    /* ═══════════════════════════════════════
       FOLLOW-UP SUGGESTIONS
       ═══════════════════════════════════════ */

    private List<String> generateFollowUps(Integer bhk, String location,
                                           Double maxPrice, String amenity, int count) {
        List<String> followUps = new ArrayList<>();
        if (count > 1) followUps.add("Want to compare these properties?");
        if (maxPrice != null) {
            double h = maxPrice * 1.5;
            followUps.add("Show options under " + (h >= 10_000_000 ? String.format("%.1f Cr", h / 10_000_000) : String.format("%.0f L", h / 100_000)));
        }
        if (bhk != null && bhk < 4) followUps.add("Show " + (bhk + 1) + " BHK options");
        if (location != null) followUps.add("More in " + capitalize(location));
        if (amenity != null) followUps.add("Show properties without " + amenity + " but cheaper");
        return followUps.stream().limit(3).collect(Collectors.toList());
    }

    /* ═══════════════════════════════════════
       CARD MAPPING — ALL fields
       ═══════════════════════════════════════ */

    private PropertyCardResponse toCard(Property p) {
        // Build highlights from amenities for frontend
        List<String> highlights = new ArrayList<>();
        if (p.getAmenities() != null && !p.getAmenities().isEmpty()) {
            highlights.addAll(p.getAmenities().stream().limit(5).collect(Collectors.toList()));
        }

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
                .highlights(highlights)
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
        return AiChatResponse.builder().message(message).hasResults(false)
                .properties(List.of()).followUps(List.of()).build();
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
        return (s == null || s.isBlank()) ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
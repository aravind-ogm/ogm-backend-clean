package com.ogm.market.ai;

import com.ogm.market.model.Property;
import com.ogm.market.repository.PropertyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AISearchServiceImpl implements AISearchService {

    private static final Logger log = LoggerFactory.getLogger(AISearchServiceImpl.class);
    private static final int DEFAULT_MAX  = 6;
    private static final int HARD_CAP     = 20;
    private static final double PRICE_TOLERANCE = 1.10;

    private final PropertyRepository  repository;
    private final GeminiService        geminiService;
    private final EmbeddingService     embeddingService;
    private final IntentDetector       intentDetector;

    @Value("${storage.base-url:http://localhost:8080}")
    private String storageBaseUrl;

    public AISearchServiceImpl(PropertyRepository repository,
                               GeminiService geminiService,
                               EmbeddingService embeddingService,
                               IntentDetector intentDetector) {
        this.repository       = repository;
        this.geminiService    = geminiService;
        this.embeddingService = embeddingService;
        this.intentDetector   = intentDetector;
    }

    @Override
    public AiChatResponse search(AiRequest request) {
        String prompt = request.getQuestion();
        if (prompt == null || prompt.isBlank()) {
            return emptyResponse("Please tell me what kind of property you're looking for.");
        }

        boolean hasGps = request.getUserLatitude()  != null
                && request.getUserLongitude() != null;

        AiFilter filter = geminiService.extractFilters(prompt);

        if (hasGps) {
            filter.setUserLatitude(request.getUserLatitude());
            filter.setUserLongitude(request.getUserLongitude());

            if (Boolean.TRUE.equals(filter.getUseCurrentLocation())) {
                if (filter.getDistanceKm() == null) {
                    filter.setDistanceKm(15.0);
                }
            }

        } else if (Boolean.TRUE.equals(filter.getUseCurrentLocation())) {
            filter.setUserLatitude(request.getUserLatitude());
            filter.setUserLongitude(request.getUserLongitude());
        }

        int limit = resolveLimit(filter);
        IntentDetector.Intent intent        = intentDetector.detect(prompt);
        String                detectedAmenity = intentDetector.extractAmenityKeyword(prompt);
        List<String>          amenityList   = filter.getAmenities();
        if ((amenityList == null || amenityList.isEmpty()) && detectedAmenity != null) {
            amenityList = List.of(detectedAmenity);
            filter.setAmenities(amenityList);
        }

        log.info("AI Search — intent={}, hasGps={}, useCurrentLocation={}, distanceKm={}, limit={}, filters={}",
                intent, hasGps, filter.getUseCurrentLocation(), filter.getDistanceKm(), limit, filter);

        List<Property> results = Collections.emptyList();
        if (filter.isDistanceSearch()) {
            results = tryDistanceSearch(filter, limit);
        }

        if (results.isEmpty() && amenityList != null && !amenityList.isEmpty()) {
            results = tryMultiAmenitySearch(filter, amenityList, limit);
        }

        if (results.isEmpty() && filter.isMultiLocation()) {
            results = tryMultiLocationSearch(filter, limit);
        }

        if (results.isEmpty()) {
            results = tryExtendedSearch(filter, limit);
        }

        if (results.isEmpty()) {
            results = tryHybridSearch(prompt, filter, limit);
        }

        if (results.isEmpty()) {
            results = tryKeywordDeepSearch(prompt, filter, limit);
        }

        // ── 7. Pure semantic fallback ──────────────────────────────────────────
        if (results.isEmpty()) {
            results = postFilter(trySemanticSearch(prompt, limit), filter);
        }

        // ── 8. Last resort: relax price filter to show closest alternatives ────
        if (results.isEmpty() && filter.getMaxPrice() != null) {
            AiFilter relaxed = cloneFilter(filter);
            relaxed.setMaxPrice(null);
            relaxed.setMinPrice(null);

            results = tryExtendedSearch(relaxed, limit);
            if (results.isEmpty()) results = tryKeywordDeepSearch(prompt, relaxed, limit);
            if (results.isEmpty()) results = postFilter(trySemanticSearch(prompt, limit), relaxed);

            if (!results.isEmpty()) {
                List<PropertyCardResponse> cards = results.stream()
                        .sorted(Comparator.comparingDouble(
                                p -> p.getPrice() != null ? p.getPrice() : Double.MAX_VALUE))
                        .limit(limit)
                        .map(p -> toCard(p, filter))
                        .collect(Collectors.toList());

                return AiChatResponse.builder()
                        .message("I couldn't find exact matches within your budget, but here are the closest available options. You may want to adjust your budget slightly.")
                        .properties(cards)
                        .hasResults(true)
                        .followUps(generateFollowUps(relaxed, null, cards.size(), results))
                        .build();
            }
        }

        if (results.isEmpty()) {
            return emptyResponse(buildNoResultMessage(filter, amenityList));
        }

        List<PropertyCardResponse> cards = results.stream()
                .limit(limit)
                .map(p -> toCard(p, filter))
                .collect(Collectors.toList());

        return AiChatResponse.builder()
                .message("")
                .properties(cards)
                .hasResults(true)
                .followUps(generateFollowUps(filter, detectedAmenity, cards.size(), results))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SEARCH STRATEGIES
    // ─────────────────────────────────────────────────────────────────────────

    private List<Property> tryExtendedSearch(AiFilter f, int limit) {
        List<Property> results = new ArrayList<>();

        List<Integer> bhkValues = f.getEffectiveBhkList();
        List<String>  locTokens = f.isMultiLocation()
                ? f.getLocations()
                : f.getEffectiveLocations();

        // null sentinel so the loop runs at least once when no filter is specified
        if (bhkValues.isEmpty()) bhkValues = Collections.singletonList(null);
        if (locTokens.isEmpty()) locTokens = Collections.singletonList(null);

        LocalDate possessionDate = parsePossessionDate(f.getPossessionBefore());

        for (String loc : locTokens) {
            for (Integer bhk : bhkValues) {
                try {
                    List<Property> hits = repository.extendedSearch(
                            loc,
                            f.getType(),
                            f.getMinPrice(),
                            f.getMaxPrice(),
                            f.getReraApproved(),
                            bhk,
                            f.getFacing(),
                            f.getFurnishing(),
                            f.getMinSqft(),
                            f.getMaxSqft(),
                            f.getDeveloperName(),
                            f.getVastuCompliant(),
                            f.getPossessionStatus(),
                            possessionDate,
                            f.getListingType(),
                            f.getNewProjectOnly(),
                            PageRequest.of(0, limit * 2)
                    ).getContent();

                    results.addAll(hits);
                } catch (Exception e) {
                    log.warn("Extended search failed loc='{}' bhk={}: {}", loc, bhk, e.getMessage());
                }
            }
        }

        return deduplicateAndSort(results);
    }

    /**
     * Distance / radius search — uses Haversine in SQL (no PostGIS required).
     * Only called when user explicitly asks for "near me" / "within X km".
     */
    private List<Property> tryDistanceSearch(AiFilter f, int limit) {
        double radiusKm = f.getDistanceKm();
        try {
            // GPS from frontend
            if (Boolean.TRUE.equals(f.getUseCurrentLocation())
                    && f.getUserLatitude()  != null
                    && f.getUserLongitude() != null) {

                log.info("Radius search: GPS ({},{}) within {} km",
                        f.getUserLatitude(), f.getUserLongitude(), radiusKm);

                return postFilter(
                        repository.findWithinRadius(
                                f.getUserLatitude(), f.getUserLongitude(),
                                radiusKm, f.getType(), f.getMaxPrice(), limit * 2),
                        f);
            }

            // Named reference location
            if (f.getReferenceLocation() != null && !f.getReferenceLocation().isBlank()) {
                log.info("Radius search: '{}' within {} km", f.getReferenceLocation(), radiusKm);

                double[] coords = resolveLocationCoords(f.getReferenceLocation());
                if (coords != null) {
                    return postFilter(
                            repository.findWithinRadius(
                                    coords[0], coords[1],
                                    radiusKm, f.getType(), f.getMaxPrice(), limit * 2),
                            f);
                }

                log.warn("No coords for '{}', falling back to location search", f.getReferenceLocation());
                f.setLocation(f.getReferenceLocation());
                return tryExtendedSearch(f, limit);
            }

        } catch (Exception e) {
            log.warn("Distance search failed: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<Property> tryMultiAmenitySearch(AiFilter f, List<String> amenities, int limit) {
        try {
            if (amenities.size() == 1) {
                return postFilter(
                        repository.searchByAmenity(amenities.get(0), f.getMaxPrice(), f.getType(), limit * 2),
                        f);
            }

            // Fetch candidates via first amenity, then in-memory intersect
            List<Property> candidates = repository.searchByAmenity(
                    amenities.get(0), f.getMaxPrice(), f.getType(), limit * 4);

            List<Property> allMatch = candidates.stream()
                    .filter(p -> hasAllAmenities(p, amenities))
                    .collect(Collectors.toList());

            if (!allMatch.isEmpty()) return postFilter(allMatch, f);

            // Relaxed: at least half the requested amenities
            int minMatch = Math.max(1, amenities.size() / 2);
            return candidates.stream()
                    .filter(p -> countMatchingAmenities(p, amenities) >= minMatch)
                    .sorted(Comparator.comparingInt(
                            (Property p) -> countMatchingAmenities(p, amenities)).reversed())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Multi-amenity search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Property> tryMultiLocationSearch(AiFilter f, int limit) {
        List<Property> all    = new ArrayList<>();
        int perLoc = Math.max(DEFAULT_MAX, limit / f.getLocations().size());

        for (String loc : f.getLocations()) {
            AiFilter copy = cloneFilter(f);
            copy.setLocation(loc);
            copy.setLocations(null);
            all.addAll(tryExtendedSearch(copy, perLoc));
        }

        return deduplicateAndSort(all);
    }

    private List<Property> tryHybridSearch(String prompt, AiFilter f, int limit) {
        try {
            List<Double> vector   = embeddingService.generateEmbedding(prompt);
            String       pgVector = toPgVector(vector);
            String       locToken = primaryLocationToken(f);

            List<Property> hits = repository.hybridSearch(
                    pgVector, locToken,
                    f.getPrimaryBhk(), f.getMaxPrice(),
                    f.getType(), limit * 2);

            return postFilter(hits, f);
        } catch (Exception e) {
            log.warn("Hybrid search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Property> tryKeywordDeepSearch(String rawPrompt, AiFilter f, int limit) {
        List<String> tokens = new ArrayList<>();
        if (f.getLocation()     != null) tokens.add(f.getLocation());
        if (f.getCity()         != null) tokens.add(f.getCity());
        if (f.getDeveloperName()!= null) tokens.add(f.getDeveloperName());
        if (f.getKeyword()      != null) tokens.add(f.getKeyword());
        tokens.addAll(rawTokens(rawPrompt, tokens));

        for (String token : tokens) {
            try {
                List<Property> hits = repository.deepSearch(
                        token, f.getMaxPrice(), f.getType(), limit * 2);
                if (!hits.isEmpty()) return postFilter(hits, f);
            } catch (Exception e) {
                log.warn("Deep search failed for '{}': {}", token, e.getMessage());
            }
        }
        return Collections.emptyList();
    }

    private List<Property> trySemanticSearch(String prompt, int limit) {
        try {
            List<Double> vector = embeddingService.generateEmbedding(prompt);
            return repository.semanticSearch(toPgVector(vector), limit);
        } catch (Exception e) {
            log.warn("Semantic search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  POST-FILTER — in-memory guard applied after every DB strategy
    // ─────────────────────────────────────────────────────────────────────────

    private List<Property> postFilter(List<Property> results, AiFilter f) {
        if (results == null || results.isEmpty()) return Collections.emptyList();

        if (Boolean.TRUE.equals(f.getInvestmentFocus())) {
            results = results.stream()
                    .sorted(Comparator.comparingDouble(
                            p -> p.getPrice() != null ? p.getPrice() : Double.MAX_VALUE))
                    .collect(Collectors.toList());
        }

        return results.stream()
                .filter(p -> matchesType(p.getType(), f.getType()))
                .filter(p -> matchesBhk(p, f))
                .filter(p -> matchesPrice(p, f))
                .filter(p -> matchesLocation(p, f))
                .filter(p -> matchesSqft(p, f))
                .filter(p -> matchesDeveloper(p, f))
                .filter(p -> matchesVastu(p, f))
                .filter(p -> matchesPossession(p, f))
                .filter(p -> matchesListingType(p, f))
                .filter(p -> matchesRera(p, f))
                .filter(p -> matchesNewProject(p, f))
                .collect(Collectors.toList());
    }

    // ── Predicates ─────────────────────────────────────────────────────────

    private boolean matchesType(String actual, String wanted) {
        if (wanted == null || actual == null) return true;
        String a = actual.toLowerCase(), w = wanted.toLowerCase();
        if (a.contains(w)) return true;
        return switch (w) {
            case "villa"              -> a.contains("villa") || a.contains("bungalow")
                    || a.contains("independent house") || a.contains("row house");
            case "apartment", "flat"  -> a.contains("apartment") || a.contains("flat") || a.contains("condo");
            case "plot",      "land"  -> a.contains("plot") || a.contains("land") || a.contains("site");
            case "commercial"         -> a.contains("commercial") || a.contains("office") || a.contains("shop");
            case "penthouse"          -> a.contains("penthouse");
            case "duplex"             -> a.contains("duplex");
            case "farmhouse"          -> a.contains("farm");
            case "holiday"            -> a.contains("holiday") || a.contains("vacation") || a.contains("resort");
            default                   -> a.contains(w);
        };
    }

    private boolean matchesBhk(Property p, AiFilter f) {
        List<Integer> list = f.getEffectiveBhkList();
        if (list.isEmpty() || p.getBedrooms() == null) return true;
        // Also check the bhk text field in case bedrooms column is 0
        if (list.contains(p.getBedrooms())) return true;
        if (p.getBhk() != null) {
            try {
                int bhkNum = Integer.parseInt(p.getBhk().replaceAll("[^0-9]", ""));
                return list.contains(bhkNum);
            } catch (NumberFormatException ignored) { }
        }
        return false;
    }

    private boolean matchesPrice(Property p, AiFilter f) {
        if (p.getPrice() == null) return true;
        // BUG FIX: was 1.30 — showing properties 30% over budget
        if (f.getMaxPrice() != null && p.getPrice() > f.getMaxPrice() * PRICE_TOLERANCE) return false;
        if (f.getMinPrice() != null && p.getPrice() < f.getMinPrice()) return false;
        return true;
    }

    private boolean matchesLocation(Property p, AiFilter f) {
        List<String> locs = f.getEffectiveLocations();
        if (locs.isEmpty() || p.getLocation() == null) return true;
        String stored = p.getLocation().toLowerCase();
        return locs.stream().anyMatch(l -> stored.contains(l.toLowerCase()));
    }

    private boolean matchesSqft(Property p, AiFilter f) {
        if (p.getSqft() == null) return true;
        if (f.getMinSqft() != null && p.getSqft() < f.getMinSqft()) return false;
        if (f.getMaxSqft() != null && p.getSqft() > f.getMaxSqft()) return false;
        return true;
    }

    private boolean matchesDeveloper(Property p, AiFilter f) {
        if (f.getDeveloperName() == null) return true;
        String dev = f.getDeveloperName().toLowerCase();
        if (p.getDeveloperName() != null && p.getDeveloperName().toLowerCase().contains(dev)) return true;
        if (p.getTitle()         != null && p.getTitle().toLowerCase().contains(dev))         return true;
        if (p.getDescription()   != null && p.getDescription().toLowerCase().contains(dev))   return true;
        if (p.getLocation()      != null && p.getLocation().toLowerCase().contains(dev))       return true;
        return false;
    }

    private boolean matchesVastu(Property p, AiFilter f) {
        if (!Boolean.TRUE.equals(f.getVastuCompliant())) return true;
        if (Boolean.TRUE.equals(p.getVastuCompliant())) return true;
        // Fallback: scan description and amenities text
        String desc = p.getDescription() != null ? p.getDescription().toLowerCase() : "";
        if (desc.contains("vastu")) return true;
        if (p.getAmenities() != null) {
            return p.getAmenities().stream().anyMatch(a -> a.toLowerCase().contains("vastu"));
        }
        return false;
    }

    private boolean matchesPossession(Property p, AiFilter f) {
        if (f.getPossessionStatus() != null && p.getPossessionStatus() != null) {
            String wanted = f.getPossessionStatus().replace("_", " ").toLowerCase();
            String actual = p.getPossessionStatus().replace("_", " ").toLowerCase();
            if (!actual.contains(wanted)) return false;
        }
        LocalDate deadline = parsePossessionDate(f.getPossessionBefore());
        if (deadline != null && p.getPossessionDate() != null) {
            if (p.getPossessionDate().isAfter(deadline)) return false;
        }
        return true;
    }

    private boolean matchesListingType(Property p, AiFilter f) {
        if (f.getListingType() == null) return true;
        // Property has no listing type set → include it unless filter is very specific
        if (p.getListingType() == null) {
            // "owner" is strict — only show owner-listed. Null = unknown → exclude.
            return !"owner".equalsIgnoreCase(f.getListingType());
        }
        return p.getListingType().toLowerCase().contains(f.getListingType().toLowerCase());
    }

    private boolean matchesRera(Property p, AiFilter f) {
        if (!Boolean.TRUE.equals(f.getReraApproved())) return true;
        return p.isReraApproved();
    }

    private boolean matchesNewProject(Property p, AiFilter f) {
        if (!Boolean.TRUE.equals(f.getNewProjectOnly())) return true;
        // null resale = treat as new project (safe default for unfilled rows)
        return !Boolean.TRUE.equals(p.getResale());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AMENITY HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private boolean hasAllAmenities(Property p, List<String> required) {
        return countMatchingAmenities(p, required) == required.size();
    }

    private int countMatchingAmenities(Property p, List<String> required) {
        String combined = "";
        if (p.getAmenities()   != null) combined += String.join(" ", p.getAmenities()).toLowerCase();
        if (p.getDescription() != null) combined += " " + p.getDescription().toLowerCase();
        final String text = combined;
        return (int) required.stream().filter(a -> text.contains(a.toLowerCase())).count();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  COORDINATE RESOLUTION — geocodes reference location from our own DB
    // ─────────────────────────────────────────────────────────────────────────

    private double[] resolveLocationCoords(String locationName) {
        try {
            List<Object[]> rows = repository.findCoordsByLocation(locationName);
            if (rows == null || rows.isEmpty()) return null;

            double lat = rows.stream().mapToDouble(r -> ((Number) r[0]).doubleValue()).average().orElse(0);
            double lng = rows.stream().mapToDouble(r -> ((Number) r[1]).doubleValue()).average().orElse(0);

            return (lat == 0 && lng == 0) ? null : new double[]{lat, lng};
        } catch (Exception e) {
            log.warn("Coord resolution failed for '{}': {}", locationName, e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FOLLOW-UP SUGGESTIONS
    // ─────────────────────────────────────────────────────────────────────────

    private List<String> generateFollowUps(AiFilter f,
                                           String amenity,
                                           int count,
                                           List<Property> results) {
        List<String> out = new ArrayList<>();

        // ── 1. Budget expansion — clean round numbers ──────────────────────
        if (f.getMaxPrice() != null) {
            double next = nextCleanBudget(f.getMaxPrice());
            String label = formatBudgetLabel(next);
            out.add("Show options under " + label);
        }

        // ── 2. BHK suggestions ─────────────────────────────────────────────
        List<Integer> bhks = f.getEffectiveBhkList();
        if (!bhks.isEmpty() && bhks.get(0) != null) {
            int currentBhk = bhks.get(0);
            if (currentBhk < 4) {
                out.add("Show " + (currentBhk + 1) + " BHK options");
            } else if (currentBhk > 1) {
                out.add("Show " + (currentBhk - 1) + " BHK for lower budget");
            }
        } else if (f.getType() != null && f.getType().equalsIgnoreCase("apartment")) {
            out.add("Show 2 BHK apartments");
        }

        // ── 3. Property-type switch ────────────────────────────────────────
        if (f.getType() != null) {
            switch (f.getType().toLowerCase()) {
                case "villa"     -> out.add("Show apartments in same area");
                case "apartment",
                     "flat"      -> out.add("Show villas in same budget");
                case "plot",
                     "land"      -> out.add("Show ready-to-move apartments instead");
                case "penthouse" -> out.add("Show premium 4 BHK apartments");
                default          -> {}
            }
        }

        // ── 4. Location expansion — suggest adjacent Bengaluru areas ──────
        String loc = f.getLocation() != null ? f.getLocation() : f.getCity();
        if (loc != null) {
            String nearby = getNearbyArea(loc);
            if (nearby != null) {
                out.add("Show properties in " + capitalize(nearby) + " too");
            } else {
                out.add("More properties in " + capitalize(loc));
            }
        }

        // ── 5. Sold-out filter — if any results are sold out ───────────────
        boolean hasSoldOut = results != null && results.stream().anyMatch(Property::isSoldOut);
        if (hasSoldOut) {
            out.add("Show only available properties");
        }

        // ── 6. RERA — if not already filtered, suggest it ─────────────────
        if (!Boolean.TRUE.equals(f.getReraApproved())) {
            out.add("Show only RERA approved properties");
        }

        // ── 7. Possession status suggestions ──────────────────────────────
        if (f.getPossessionStatus() == null) {
            if (f.getType() != null && !f.getType().equalsIgnoreCase("plot")) {
                out.add("Show ready to move options only");
            }
        } else if ("ready_to_move".equals(f.getPossessionStatus())) {
            out.add("Show under construction for lower price");
        }

        // ── 8. Vastu — if not filtered ─────────────────────────────────────
        if (!Boolean.TRUE.equals(f.getVastuCompliant()) && count > 0) {
            out.add("Show vastu compliant homes");
        }

        // ── 9. Amenity-related ─────────────────────────────────────────────
        if (amenity != null) {
            out.add("Remove " + amenity + " filter for more options");
        }

        // ── 10. Investment focus ───────────────────────────────────────────
        if (Boolean.TRUE.equals(f.getInvestmentFocus())) {
            out.add("Show properties with highest rental yield");
        } else if (count > 0) {
            out.add("Which of these has best rental yield?");
        }

        // Return max 4, deduplicated
        return out.stream()
                .distinct()
                .limit(4)
                .collect(Collectors.toList());
    }


    private double nextCleanBudget(double current) {
        // Steps in INR: 25L, 50L, 75L, 1Cr, 1.25Cr, 1.5Cr, 2Cr, 2.5Cr, 3Cr, 4Cr, 5Cr, 7Cr, 10Cr
        double[] steps = {
                2_500_000, 5_000_000, 7_500_000,
                10_000_000, 12_500_000, 15_000_000, 20_000_000,
                25_000_000, 30_000_000, 40_000_000, 50_000_000,
                70_000_000, 100_000_000
        };
        for (double step : steps) {
            if (step > current) return step;
        }
        return current * 1.5;
    }

    /** Formats a price as a clean label: "₹75 L", "₹1.5 Cr" etc. */
    private String formatBudgetLabel(double price) {
        if (price >= 10_000_000) {
            double cr = price / 10_000_000;
            // Show as integer if clean (1 Cr, 2 Cr) else 1 decimal (1.5 Cr)
            return cr == Math.floor(cr)
                    ? String.format("₹%.0f Cr", cr)
                    : String.format("₹%.1f Cr", cr);
        }
        double l = price / 100_000;
        return l == Math.floor(l)
                ? String.format("₹%.0f L", l)
                : String.format("₹%.1f L", l);
    }

    /** Returns a relevant nearby area for common Bengaluru locations. */
    private String getNearbyArea(String location) {
        if (location == null) return null;
        String loc = location.toLowerCase();
        return switch (loc) {
            case "whitefield"     -> "Marathahalli";
            case "koramangala"    -> "HSR Layout";
            case "hsr layout"     -> "Koramangala";
            case "marathahalli"   -> "Whitefield";
            case "indiranagar"    -> "Koramangala";
            case "electronic city" -> "Sarjapur Road";
            case "sarjapur road"  -> "Electronic City";
            case "hebbal"         -> "Yelahanka";
            case "yelahanka"      -> "Hebbal";
            case "jp nagar"       -> "Jayanagar";
            case "jayanagar"      -> "JP Nagar";
            case "bannerghatta"   -> "JP Nagar";
            case "hennur"         -> "Thanisandra";
            case "thanisandra"    -> "Hennur";
            case "devanahalli"    -> "Yelahanka";
            case "mg road"        -> "Indiranagar";
            default               -> null;
        };
    }
    // ─────────────────────────────────────────────────────────────────────────
    //  CARD MAPPING
    // ─────────────────────────────────────────────────────────────────────────

    private PropertyCardResponse toCard(Property p, AiFilter f) {
        List<String> highlights = new ArrayList<>();
        if (p.getAmenities() != null) {
            highlights.addAll(p.getAmenities().stream().limit(5).collect(Collectors.toList()));
        }

        // Compute distance label if GPS is available
        String distanceLabel = null;
        if (f.getUserLatitude() != null && f.getUserLongitude() != null
                && p.getLatitude() != null && p.getLongitude() != null) {
            double km = haversine(f.getUserLatitude(), f.getUserLongitude(),
                    p.getLatitude(), p.getLongitude());
            distanceLabel = String.format("%.1f km away", km);
        }

        // Resolve BHK display value: prefer bhk field, fall back to bedrooms
        String bhkDisplay = p.getBhk();
        if ((bhkDisplay == null || bhkDisplay.isBlank()) && p.getBedrooms() != null && p.getBedrooms() > 0) {
            bhkDisplay = p.getBedrooms() + " BHK";
        }

        return PropertyCardResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .price(p.getFormattedPrice())
                .location(p.getLocation())
                .type(p.getType())
                .bhk(bhkDisplay)
                .sqft(p.getSqft())
                .bedrooms(p.getBedrooms())
                .bathrooms(p.getBathrooms())
                .facing(p.getFacing())
                .furnishing(p.getFurnishing())
                .description(p.getDescription())
                .maintenance(p.getMaintenance())
                .developerName(p.getDeveloperName())
                .listingType(p.getListingType())
                .possessionStatus(p.getPossessionStatus())
                .vastuCompliant(Boolean.TRUE.equals(p.getVastuCompliant()))
                .primaryImage(prefixUrl(p.getPrimaryImage()))
                .gallery(prefixUrls(p.getImages()))
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .distanceLabel(distanceLabel)
                .googleMapsUrl(p.getGoogleMapsUrl())
                .reraApproved(p.isReraApproved())
                .soldOut(p.isSoldOut())
                .amenities(p.getAmenities() != null ? p.getAmenities() : Collections.emptyList())
                .highlights(highlights)
                .slug(p.getSlug())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILITIES
    // ─────────────────────────────────────────────────────────────────────────

    private int resolveLimit(AiFilter f) {
        if (f.getMaxResults() != null && f.getMaxResults() > 0) {
            return Math.min(f.getMaxResults(), HARD_CAP);
        }
        return DEFAULT_MAX;
    }

    private String primaryLocationToken(AiFilter f) {
        if (f.getLocation() != null && !f.getLocation().isBlank()) return f.getLocation();
        if (f.getCity()     != null && !f.getCity().isBlank())     return f.getCity();
        return null;
    }

    private LocalDate parsePossessionDate(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return null;
        try {
            return LocalDate.parse(isoDate);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private List<Property> deduplicateAndSort(List<Property> list) {
        Map<Long, Property> seen = new LinkedHashMap<>();
        for (Property p : list) seen.putIfAbsent(p.getId(), p);
        return new ArrayList<>(seen.values());
    }

    private AiFilter cloneFilter(AiFilter src) {
        AiFilter c = new AiFilter();
        c.setCity(src.getCity());
        c.setLocation(src.getLocation());
        c.setLocations(src.getLocations());
        c.setDistanceKm(src.getDistanceKm());
        c.setReferenceLocation(src.getReferenceLocation());
        c.setUseCurrentLocation(src.getUseCurrentLocation());
        c.setUserLatitude(src.getUserLatitude());
        c.setUserLongitude(src.getUserLongitude());
        c.setBhk(src.getBhk());
        c.setBhkList(src.getBhkList());
        c.setMinPrice(src.getMinPrice());
        c.setMaxPrice(src.getMaxPrice());
        c.setType(src.getType());
        c.setFacing(src.getFacing());
        c.setFurnishing(src.getFurnishing());
        c.setMinSqft(src.getMinSqft());
        c.setMaxSqft(src.getMaxSqft());
        c.setDeveloperName(src.getDeveloperName());
        c.setAmenities(src.getAmenities());
        c.setVastuCompliant(src.getVastuCompliant());
        c.setPossessionStatus(src.getPossessionStatus());
        c.setPossessionBefore(src.getPossessionBefore());
        c.setListingType(src.getListingType());
        c.setNewProjectOnly(src.getNewProjectOnly());
        c.setReraApproved(src.getReraApproved());
        c.setInvestmentFocus(src.getInvestmentFocus());
        c.setMaxResults(src.getMaxResults());
        c.setKeyword(src.getKeyword());
        return c;
    }

    private List<String> rawTokens(String query, List<String> alreadyUsed) {
        Set<String> stopWords = Set.of(
                "can", "you", "find", "me", "show", "i", "want", "need", "a", "an", "the",
                "with", "in", "at", "near", "for", "and", "or", "of", "to", "is", "are",
                "please", "send", "get", "give", "looking", "search", "any", "some",
                "property", "properties", "bhk", "under", "below", "above", "budget",
                "cr", "crore", "lakh", "lakhs", "lac"
        );
        Set<String> used = new HashSet<>(alreadyUsed);
        return Arrays.stream(query.toLowerCase().split("\\s+"))
                .filter(w -> w.length() > 2 && !stopWords.contains(w)
                        && !w.matches("\\d+") && !used.contains(w))
                .collect(Collectors.toList());
    }

    private String buildNoResultMessage(AiFilter f, List<String> amenities) {
        if (amenities != null && !amenities.isEmpty()) {
            return "I couldn't find properties with " + String.join(", ", amenities)
                    + ". Try removing some amenities or adjusting your budget.";
        }
        if (f.getDeveloperName() != null) {
            return "I couldn't find " + capitalize(f.getDeveloperName())
                    + " projects matching your criteria. Try a different location or adjust the budget.";
        }
        if (f.getType() != null) {
            return "I couldn't find " + f.getType() + "s matching your criteria. "
                    + "Try adjusting the budget or location.";
        }
        return "I couldn't find matching properties. "
                + "Try broadening the location, adjusting the budget, or relaxing some filters.";
    }

    private String toPgVector(List<Double> v) {
        return "[" + v.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
    }

    private AiChatResponse emptyResponse(String message) {
        return AiChatResponse.builder()
                .message(message)
                .hasResults(false)
                .properties(List.of())
                .followUps(List.of())
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
        return (s == null || s.isBlank()) ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /** Haversine distance in km between two GPS points. */
    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        final double R    = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a    = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
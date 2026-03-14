package com.ogm.market.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Structured filters extracted from the user's natural-language query by Gemini.
 *
 * Covers all 19 buyer search scenarios:
 *  1.  bhkList            — multiple BHK values   ("2 BHK and 3 BHK")
 *  2.  locations          — multiple areas         ("Koramangala, Whitefield, HSR")
 *  3.  distanceKm         — radius search          ("within 10 km from Indiranagar")
 *  4.  useCurrentLocation — GPS radius             ("from my current location")
 *  5.  developerName      — builder search         ("Prestige Developers")
 *  6.  minSqft / maxSqft  — area range             ("1100 to 1250 sqft")
 *  7.  amenities          — list of required amenities
 *  8.  vastuCompliant     — Vastu filter
 *  9.  possessionStatus   — ready_to_move / new_launch / under_construction
 *  10. possessionBefore   — ISO date string        ("2029-12-31")
 *  11. listingType        — owner / developer / builder
 *  12. newProjectOnly     — exclude resale
 *  13. investmentFocus    — flag for rental-yield queries
 *  14. maxResults         — "top 10 properties"
 *  15. reraApproved       — RERA filter
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiFilter {

    // ── Location ──────────────────────────────────────────────────────────
    /** Broader city (e.g. "bangalore", "mumbai") */
    private String city;

    /**
     * Primary neighbourhood / area (e.g. "whitefield").
     * For single-location queries; see {@link #locations} for multi-location.
     */
    private String location;

    /**
     * Multiple neighbourhoods for queries like
     * "2 BHKs in Koramangala, HSR Layout, and Whitefield".
     * When populated, supersedes {@link #location}.
     */
    private List<String> locations;

    /** Distance radius in km — "within 10 km from Indiranagar" */
    private Double distanceKm;

    /**
     * Named reference point for radius search
     * — "within 10 km from <referenceLocation>".
     * Null when the user says "my current location" (use GPS instead).
     */
    private String referenceLocation;

    /**
     * True when the user says "from my current location / near me".
     * The controller injects userLatitude/userLongitude from AiRequest.
     */
    private Boolean useCurrentLocation;

    // ── Property basics ───────────────────────────────────────────────────
    /** Single BHK value — kept for backwards compat; prefer bhkList. */
    private String bhk;

    /**
     * Multiple BHK values — "2 BHK and 3 BHK", "2 or 3 BHK".
     * When size == 1, behaves like bhk.
     */
    private List<Integer> bhkList;

    private Double minPrice;
    private Double maxPrice;

    /** villa / apartment / flat / plot / land / penthouse / duplex / farmhouse / commercial / holiday */
    private String type;

    /** north / south / east / west / north-east / north-west / south-east / south-west */
    private String facing;

    /** furnished / semi-furnished / unfurnished */
    private String furnishing;

    private Boolean reraApproved;

    // ── Size ──────────────────────────────────────────────────────────────
    private Integer minSqft;
    private Integer maxSqft;

    // ── Developer / builder ───────────────────────────────────────────────
    /**
     * Builder/developer name — "Prestige Developers", "Brigade", "Sobha".
     * Matched against a developer/builder column in the DB.
     */
    private String developerName;

    // ── Amenities ─────────────────────────────────────────────────────────
    /**
     * List of amenities that MUST be present.
     * e.g. ["swimming pool", "cricket practice net", "badminton court"]
     */
    private List<String> amenities;

    // ── Project / listing attributes ──────────────────────────────────────
    /** true → only Vastu-compliant properties */
    private Boolean vastuCompliant;

    /**
     * Possession / project status:
     *   ready_to_move | new_launch | under_construction | pre_launch
     */
    private String possessionStatus;

    /**
     * Earliest possession deadline (ISO date "YYYY-MM-DD").
     * e.g. "2029-12-31" for "possession before 2029 December"
     */
    private String possessionBefore;

    /**
     * Listing source filter:
     *   owner | developer | builder | any (default)
     */
    private String listingType;

    /** true → exclude resale; only new/direct-from-developer listings */
    private Boolean newProjectOnly;

    /** true → query is investment-oriented (rental yield, ROI focus) */
    private Boolean investmentFocus;

    // ── Result tuning ─────────────────────────────────────────────────────
    /**
     * Requested result count — "top 10 properties".
     * Defaults to 6 in AISearchServiceImpl when null.
     */
    private Integer maxResults;

    /** Generic fallback search term for anything not covered above */
    private String keyword;

    // ─────────────────────────────────────────────────────────────────────
    //  Runtime fields (set by controller — NOT extracted by Gemini)
    // ─────────────────────────────────────────────────────────────────────

    /** Injected from AiRequest when useCurrentLocation == true */
    private Double userLatitude;
    private Double userLongitude;

    // ─────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────

    /** Returns the primary BHK as an Integer, sourcing bhkList[0] or bhk field. */
    public Integer getPrimaryBhk() {
        if (bhkList != null && !bhkList.isEmpty()) return bhkList.get(0);
        return getBhkAsInteger();
    }

    /** True when the query covers multiple BHK types. */
    public boolean isMultiBhk() {
        return bhkList != null && bhkList.size() > 1;
    }

    /** True when the query covers multiple locations. */
    public boolean isMultiLocation() {
        return locations != null && locations.size() > 1;
    }

    /** True when a radius search is requested. */
    public boolean isDistanceSearch() {
        return distanceKm != null && distanceKm > 0;
    }

    /** Parse single bhk string to Integer safely */
    public Integer getBhkAsInteger() {
        if (bhk == null || bhk.isBlank()) return null;
        try {
            return Integer.parseInt(bhk.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Returns all effective BHK values as a list (normalises single vs multi). */
    public List<Integer> getEffectiveBhkList() {
        if (bhkList != null && !bhkList.isEmpty()) return bhkList;
        Integer single = getBhkAsInteger();
        return single != null ? List.of(single) : List.of();
    }

    /** Returns all effective locations as a list (normalises single vs multi). */
    public List<String> getEffectiveLocations() {
        if (locations != null && !locations.isEmpty()) return locations;
        if (location != null && !location.isBlank()) return List.of(location);
        if (city    != null && !city.isBlank())     return List.of(city);
        return List.of();
    }
}
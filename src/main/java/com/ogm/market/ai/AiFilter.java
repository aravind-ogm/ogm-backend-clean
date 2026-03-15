package com.ogm.market.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiFilter {

    private String city;

    private String location;

    private List<String> locations;

    private Double distanceKm;

    private String referenceLocation;

    private Boolean useCurrentLocation;

    private String bhk;

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

    private Integer minSqft;

    private Integer maxSqft;

    private String developerName;

    private List<String> amenities;

    private Boolean vastuCompliant;

    /**
     * Possession / project status:
     *   ready_to_move | new_launch | under_construction | pre_launch
     */
    private String possessionStatus;

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

    private Integer maxResults;

    /** Generic fallback search term for anything not covered above */
    private String keyword;

    /** Injected from AiRequest when useCurrentLocation == true */
    private Double userLatitude;

    private Double userLongitude;


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
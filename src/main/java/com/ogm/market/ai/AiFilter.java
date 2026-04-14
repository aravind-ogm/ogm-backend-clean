package com.ogm.market.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
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
    private String type;
    private String facing;
    private String furnishing;
    private Boolean reraApproved;
    private Integer minSqft;
    private Integer maxSqft;
    private String developerName;
    private List<String> amenities;
    private Boolean vastuCompliant;
    private String possessionStatus;
    private String possessionBefore;
    private String listingType;
    private Boolean newProjectOnly;
    private Boolean investmentFocus;
    private Integer maxResults;
    private String keyword;
    private Double userLatitude;
    private Double userLongitude;

    public Integer getPrimaryBhk() {
        if (bhkList != null && !bhkList.isEmpty()) return bhkList.get(0);
        return getBhkAsInteger();
    }

    public boolean isMultiBhk() {
        return bhkList != null && bhkList.size() > 1;
    }

    public boolean isMultiLocation() {
        return locations != null && locations.size() > 1;
    }

    public boolean isDistanceSearch() {
        return distanceKm != null && distanceKm > 0;
    }

    public Integer getBhkAsInteger() {
        if (bhk == null || bhk.isBlank()) return null;
        try {
            return Integer.parseInt(bhk.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<Integer> getEffectiveBhkList() {
        if (bhkList != null && !bhkList.isEmpty()) return bhkList;
        Integer single = getBhkAsInteger();
        return single != null ? List.of(single) : List.of();
    }

    public List<String> getEffectiveLocations() {
        if (locations != null && !locations.isEmpty()) return locations;
        if (location != null && !location.isBlank()) return List.of(location);
        if (city != null && !city.isBlank()) return List.of(city);
        return List.of();
    }
}
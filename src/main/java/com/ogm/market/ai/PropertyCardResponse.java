package com.ogm.market.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PropertyCardResponse {
    private Long id;
    private String title;
    private String slug;
    private String price;
    private String location;
    private String type;
    private String bhk;
    private Integer sqft;
    private Integer bedrooms;
    private Integer bathrooms;
    private String facing;
    private String furnishing;
    private String maintenance;
    private String description;
    private String developerName;
    private String listingType;
    private String possessionStatus;
    private String primaryImage;
    private List<String> gallery;
    private Double latitude;
    private Double longitude;
    private String distanceLabel;
    private String googleMapsUrl;
    private boolean reraApproved;
    private boolean vastuCompliant;
    private boolean soldOut;
    private List<String> amenities;
    private List<String> highlights;
}
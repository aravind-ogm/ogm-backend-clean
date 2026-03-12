package com.ogm.market.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PropertyCardResponse {

    private Long id;
    private String title;
    private String price;
    private String location;

    // ── Property details ──
    private String type;
    private Integer sqft;
    private Integer bedrooms;
    private Integer bathrooms;
    private String facing;
    private String furnishing;
    private String description;

    // ── Images ──
    private String primaryImage;
    private List<String> gallery;

    // ── Location / Map ──
    private Double latitude;
    private Double longitude;
    private String googleMapsUrl;

    // ── Status ──
    private boolean reraApproved;
    private boolean soldOut;

    // ── Amenities ──
    private List<String> amenities;

    // ── Navigation ──
    private String slug;
}
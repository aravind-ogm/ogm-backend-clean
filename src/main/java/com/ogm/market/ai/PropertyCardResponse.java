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

    private String type;
    private Integer sqft;
    private Integer bedrooms;
    private Integer bathrooms;
    private String facing;
    private String furnishing;
    private String description;

    private String primaryImage;
    private List<String> gallery;

    private Double latitude;
    private Double longitude;
    private String googleMapsUrl;

    private boolean reraApproved;
    private boolean soldOut;

    private List<String> amenities;
    private List<String> highlights;

    private String slug;
}
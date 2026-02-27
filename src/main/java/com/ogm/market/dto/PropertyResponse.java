package com.ogm.market.dto;

import com.ogm.market.model.NearbyLocation;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PropertyResponse {

    private Long id;
    private String title;
    private String location;
    private String price;

    private String image;
    private String type;
    private String sqft;
    private boolean reraApproved;
    private boolean soldOut;

    private List<String> mainImages;
    private List<String> amenities;

    private List<String> images;

    private String bedrooms;
    private String bathrooms;

    private String carpetArea;
    private String landArea;
    private String builtupArea;
    private String parking;
    private String maintenance;
    private String facing;
    private String furnishing;

    private String description;
    private String videoUrl;
    private List<NearbyLocation> nearby;

    private String slug;

    private Double latitude;
    private Double longitude;
}

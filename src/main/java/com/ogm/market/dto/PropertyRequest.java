package com.ogm.market.dto;

import com.ogm.market.model.NearbyLocation;
import lombok.Data;

import java.util.List;

@Data
public class PropertyRequest {

    private String title;
    private String location;
    private String price;

    private String image;
    private String type;
    private String sqft;
    private boolean reraApproved;

    private List<String> mainImages;
    private List<String> amenities;

    private List<String> images;

    private Integer bedrooms;
    private Integer bathrooms;

    private String carpetArea;
    private String builtupArea;
    private String parking;
    private String maintenance;
    private String facing;
    private String furnishing;

    private String description;
    private String videoUrl;
    private List<NearbyLocation> nearby;


}

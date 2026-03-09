package com.ogm.market.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PropertyCardResponse {

    private String type;   // Always "property_card"

    private Long id;
    private String title;
    private String price;
    private String location;
    private Integer sqft;

    private String primaryImage;
    private List<String> gallery;

    private Double latitude;
    private Double longitude;
    private String googleMapsUrl;

    private boolean reraApproved;
    private boolean soldOut;

    private String slug;
}
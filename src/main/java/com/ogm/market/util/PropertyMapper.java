package com.ogm.market.util;

import com.ogm.market.dto.PropertyResponse;
import com.ogm.market.model.Property;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PropertyMapper {

    @Value("${storage.base-url:http://localhost:8080}")
    private String storageBaseUrl;

    public PropertyResponse toResponse(Property p) {

        if (p == null) {
            return null;
        }

        // Ensure lazy collections are initialized
        Hibernate.initialize(p.getImages());
        Hibernate.initialize(p.getMainImages());
        Hibernate.initialize(p.getAmenities());
        Hibernate.initialize(p.getNearby());

        return PropertyResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .location(p.getLocation())
                .price(p.getPrice() == null ? null : String.valueOf(p.getPrice()))
                .type(p.getType())
                .sqft(p.getSqft())

                .bedrooms(p.getBedrooms())
                .bathrooms(p.getBathrooms())

                .description(p.getDescription())

                .image(prefix(p.getImage()))

                .images(mapUrls(p.getImages()))
                .mainImages(mapUrls(p.getMainImages()))

                .amenities(p.getAmenities() == null ? Collections.emptyList() : p.getAmenities())
                .nearby(p.getNearby() == null ? Collections.emptyList() : p.getNearby())

                .facing(p.getFacing())
                .furnishing(p.getFurnishing())
                .parking(p.getParking())
                .maintenance(p.getMaintenance())

                .reraApproved(p.isReraApproved())
                .soldOut(p.isSoldOut())

                .videoUrl(prefix(p.getVideoUrl()))
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())

                .build();
    }

    private List<String> mapUrls(List<String> urls) {

        if (urls == null || urls.isEmpty()) {
            return Collections.emptyList();
        }

        return urls.stream()
                .map(this::prefix)
                .toList();
    }

    private String prefix(String url) {

        if (url == null || url.isBlank()) {
            return null;
        }

        if (url.startsWith("http")) {
            return url;
        }

        if (url.startsWith("/")) {
            return storageBaseUrl + url;
        }

        return storageBaseUrl + "/" + url;
    }

}

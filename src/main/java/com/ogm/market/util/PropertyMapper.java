package com.ogm.market.util;

import com.ogm.market.dto.PropertyResponse;
import com.ogm.market.model.Property;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class PropertyMapper {

    @Value("${storage.base-url:http://localhost:8080}")
    private String storageBaseUrl;

    public PropertyResponse toResponse(Property p) {

        // ✅ FORCE INITIALIZATION of lazy collections
        Hibernate.initialize(p.getImages());
        Hibernate.initialize(p.getMainImages());
        Hibernate.initialize(p.getAmenities());
        Hibernate.initialize(p.getNearby());

        return PropertyResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .location(p.getLocation())
                .price(String.valueOf(p.getPrice()))
                .type(p.getType())
                .sqft(p.getSqft())

                .bedrooms(p.getBedrooms())
                .bathrooms(p.getBathrooms())

                .description(p.getDescription())

                .image(prefix(p.getImage()))

                .images(p.getImages() == null ? null :
                        p.getImages().stream()
                                .map(this::prefix)
                                .collect(Collectors.toList()))

                .mainImages(p.getMainImages() == null ? null :
                        p.getMainImages().stream()
                                .map(this::prefix)
                                .collect(Collectors.toList()))

                .amenities(p.getAmenities())
                .nearby(p.getNearby())

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

    private String prefix(String url) {
        if (url == null || url.isBlank()) return null;
        if (url.startsWith("http")) return url;
        return storageBaseUrl + (url.startsWith("/") ? url : "/" + url);
    }
}

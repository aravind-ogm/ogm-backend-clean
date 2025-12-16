package com.ogm.market.service;

import com.ogm.market.dto.PropertyRequest;
import com.ogm.market.dto.PropertyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PropertyService {

    Page<PropertyResponse> listProperties(
            String q,
            String type,
            Long minPrice,
            Long maxPrice,
            Integer minSqft,
            Integer maxSqft,
            Boolean rera,
            Integer bhk,
            String facing,
            String furnishing,
            Pageable pageable
    );

    PropertyResponse getProperty(Long id);


    PropertyResponse createProperty(PropertyRequest request);

    PropertyResponse updateProperty(Long id, PropertyRequest request);

    void deleteProperty(Long id);
}

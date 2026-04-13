package com.ogm.market.service;

import com.ogm.market.dto.PropertyRequest;
import com.ogm.market.dto.PropertyResponse;
import com.ogm.market.model.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PropertyService {

    Page<PropertyResponse> listProperties(
            String  q,
            String  type,
            Double  minPrice,
            Double  maxPrice,
            Boolean rera,
            Integer bhk,
            String  facing,
            String  furnishing,
            Pageable pageable
    );

    PropertyResponse getProperty(Long id);

    PropertyResponse createProperty(PropertyRequest request);

    PropertyResponse updateProperty(Long id, PropertyRequest request);

    void deleteProperty(Long id);

    /**
     * BUG FIX: This method was private in PropertyServiceImpl but
     * PropertyController.getPropertyBySlug() calls propertyService.toResponse(p)
     * directly — which caused a compile error since it wasn't in the interface.
     *
     * Exposed here so the controller can map a raw Property (from slug lookup)
     * to a PropertyResponse DTO without duplicating the mapping logic.
     */
    PropertyResponse toResponse(Property property);
}
package com.ogm.market.service;

import com.ogm.market.dto.PropertyRequest;
import com.ogm.market.dto.PropertyResponse;
import com.ogm.market.exception.ResourceNotFoundException;
import com.ogm.market.model.Property;
import com.ogm.market.repository.PropertyRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository repository;
    private final String storageBaseUrl;

    public PropertyServiceImpl(
            PropertyRepository repository,
            @Value("${storage.base-url:}") String storageBaseUrl
    ) {
        this.repository = repository;
        this.storageBaseUrl = storageBaseUrl;
    }

    /*
     =========================================================
     ADVANCED SEARCH
     =========================================================
     */
    @Override
    @Transactional(readOnly = true)
    public Page<PropertyResponse> listProperties(
            String q,
            String type,
            Double minPrice,
            Double maxPrice,
            Boolean rera,
            Integer bhk,
            String facing,
            String furnishing,
            Pageable pageable
    ) {

        return repository.advancedSearch(
                q,
                type,
                minPrice,
                maxPrice,
                rera,
                bhk,
                facing,
                furnishing,
                pageable
        ).map(this::toResponse);
    }


    /*
     =========================================================
     GET PROPERTY BY ID
     =========================================================
     */
    @Override
    @Transactional(readOnly = true)
    public PropertyResponse getProperty(Long id) {

        Property p = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Property not found: " + id)
                );

        return toResponse(p);
    }


    /*
     =========================================================
     CREATE PROPERTY
     =========================================================
     */
    @Override
    @Transactional
    public PropertyResponse createProperty(PropertyRequest request) {

        Property p = new Property();

        BeanUtils.copyProperties(request, p);

        if (request.getImages() != null) p.setImages(request.getImages());
        if (request.getMainImages() != null) p.setMainImages(request.getMainImages());
        if (request.getAmenities() != null) p.setAmenities(request.getAmenities());
        if (request.getNearby() != null) p.setNearby(request.getNearby());

        return toResponse(repository.save(p));
    }


    /*
     =========================================================
     UPDATE PROPERTY
     =========================================================
     */
    @Override
    @Transactional
    public PropertyResponse updateProperty(Long id, PropertyRequest request) {

        Property existing = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Property not found: " + id)
                );

        BeanUtils.copyProperties(request, existing, "id", "slug");

        if (request.getImages() != null) existing.setImages(request.getImages());
        if (request.getMainImages() != null) existing.setMainImages(request.getMainImages());
        if (request.getAmenities() != null) existing.setAmenities(request.getAmenities());
        if (request.getNearby() != null) existing.setNearby(request.getNearby());

        return toResponse(repository.save(existing));
    }


    /*
     =========================================================
     DELETE PROPERTY
     =========================================================
     */
    @Override
    @Transactional
    public void deleteProperty(Long id) {

        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Property not found: " + id);
        }

        repository.deleteById(id);
    }


    /*
     =========================================================
     ENTITY → RESPONSE MAPPER
     =========================================================
     */
    private PropertyResponse toResponse(Property p) {

        Hibernate.initialize(p.getAmenities());
        Hibernate.initialize(p.getMainImages());
        Hibernate.initialize(p.getImages());
        Hibernate.initialize(p.getNearby());

        return PropertyResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .location(p.getLocation())
                .price(p.getPrice() == null ? null : String.valueOf(p.getPrice()))
                .slug(p.getSlug())
                .image(prefix(p.getImage()))
                .type(p.getType())
                .sqft(p.getSqft())
                .reraApproved(p.isReraApproved())
                .soldOut(p.isSoldOut())

                .mainImages(
                        p.getMainImages() == null ? null :
                                p.getMainImages().stream().map(this::prefix).toList()
                )

                .images(
                        p.getImages() == null ? null :
                                p.getImages().stream().map(this::prefix).toList()
                )

                .amenities(p.getAmenities())
                .nearby(p.getNearby())

                .bedrooms(p.getBedrooms())
                .bathrooms(p.getBathrooms())

                .carpetArea(p.getCarpetArea())
                .landArea(p.getLandArea())
                .builtupArea(p.getBuiltupArea())

                .parking(p.getParking())
                .maintenance(p.getMaintenance())
                .facing(p.getFacing())
                .furnishing(p.getFurnishing())

                .description(p.getDescription())

                .videoUrl(prefix(p.getVideoUrl()))

                .build();
    }


    /*
     =========================================================
     STORAGE URL PREFIX
     =========================================================
     */
    private String prefix(String url) {

        if (url == null) return null;

        if (url.startsWith("http")) return url;

        if (storageBaseUrl == null || storageBaseUrl.isBlank()) return url;

        return url.startsWith("/")
                ? storageBaseUrl + url
                : storageBaseUrl + "/" + url;
    }
}
package com.ogm.market.service;

import com.ogm.market.ai.EmbeddingService;
import com.ogm.market.dto.PropertyRequest;
import com.ogm.market.dto.PropertyResponse;
import com.ogm.market.exception.ResourceNotFoundException;
import com.ogm.market.model.Property;
import com.ogm.market.repository.PropertyRepository;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PropertyServiceImpl implements PropertyService {

    private static final Logger log = LoggerFactory.getLogger(PropertyServiceImpl.class);

    private final PropertyRepository repository;
    private final EmbeddingService   embeddingService;
    private final String             storageBaseUrl;

    public PropertyServiceImpl(
            PropertyRepository repository,
            EmbeddingService embeddingService,
            // BUG FIX: default was "" — prefix() silently returned bare paths like
            // "/images/p9/..." with no host. Default to localhost for local dev.
            @Value("${storage.base-url:http://localhost:8080}") String storageBaseUrl) {
        this.repository       = repository;
        this.embeddingService = embeddingService;
        this.storageBaseUrl   = storageBaseUrl;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LIST / SEARCH
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<PropertyResponse> listProperties(
            String q, String type,
            Double minPrice, Double maxPrice,
            Boolean rera, Integer bhk,
            String facing, String furnishing,
            Pageable pageable) {

        return repository.advancedSearch(
                q, type, minPrice, maxPrice,
                rera, bhk, facing, furnishing,
                pageable
        ).map(this::toResponse);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET BY ID
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PropertyResponse getProperty(Long id) {
        Property p = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));
        return toResponse(p);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PropertyResponse createProperty(PropertyRequest request) {
        Property p = mapRequestToNewProperty(request);
        Property saved = repository.save(p);

        // Generate and store embedding asynchronously after save
        generateAndStoreEmbeddingAsync(saved);

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PropertyResponse updateProperty(Long id, PropertyRequest request) {
        Property existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));

        // BUG FIX: original used BeanUtils.copyProperties(request, existing, "id", "slug")
        // which copies NULL values from request → existing, wiping fields the client
        // didn't include in the update payload.
        // Fix: manually map only non-null fields (patch semantics).
        applyNonNullUpdates(request, existing);

        Property saved = repository.save(existing);

        // Regenerate embedding when content changes
        generateAndStoreEmbeddingAsync(saved);

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteProperty(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Property not found: " + id);
        }
        repository.deleteById(id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ENTITY → DTO MAPPER  (public — exposed via PropertyService interface)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * BUG FIX 1: Was private — controller couldn't call it (compile error).
     * BUG FIX 2: Was using String.valueOf(p.getPrice()) → "38500000.0"
     *            Now uses p.getFormattedPrice() → "₹3.85 Cr"
     * BUG FIX 3: Many fields were missing from the DTO:
     *            bhk, latitude, longitude, developerName, listingType,
     *            possessionStatus, vastuCompliant, googleMapsUrl, formattedPrice
     */
    @Override
    @Transactional(readOnly = true)
    public PropertyResponse toResponse(Property p) {
        // Initialise lazy collections within the transaction
        Hibernate.initialize(p.getAmenities());
        Hibernate.initialize(p.getMainImages());
        Hibernate.initialize(p.getImages());
        Hibernate.initialize(p.getNearby());

        return PropertyResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .location(p.getLocation())
                .slug(p.getSlug())
                .type(p.getType())
                .sqft(p.getSqft())

                // BUG FIX: bhk was missing — frontend property cards showed no BHK info
                .bhk(p.getBhkDisplay())

                // BUG FIX: was String.valueOf(price) → "38500000.0"
                // Now returns formatted string: "₹3.85 Cr"
                .price(p.getFormattedPrice())
                // Also expose raw numeric price for sorting/filtering on frontend
                .priceRaw(p.getPrice())

                .reraApproved(p.isReraApproved())
                .soldOut(p.isSoldOut())

                // BUG FIX: vastuCompliant was missing from DTO
                .vastuCompliant(Boolean.TRUE.equals(p.getVastuCompliant()))

                // Images
                .image(prefix(p.getImage()))
                .mainImages(prefixList(p.getMainImages()))
                .images(prefixList(p.getImages()))

                // Specs
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

                // BUG FIX: developerName was missing
                .developerName(p.getDeveloperName())
                // BUG FIX: listingType was missing
                .listingType(p.getListingType())
                // BUG FIX: possessionStatus was missing
                .possessionStatus(p.getPossessionStatus())
                .possessionDate(p.getPossessionDate() != null
                        ? p.getPossessionDate().toString() : null)

                // BUG FIX: latitude/longitude were missing — map integration broken
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                // BUG FIX: googleMapsUrl was missing
                .googleMapsUrl(p.getGoogleMapsUrl())

                .videoUrl(prefix(p.getVideoUrl()))
                .brochureFile(p.getBrochureFile() != null ? prefix(p.getBrochureFile()) : null)

                .amenities(p.getAmenities())
                .nearby(p.getNearby())

                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EMBEDDING GENERATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a vector embedding for the property and stores it in the DB.
     * Runs asynchronously so create/update responses are not delayed.
     *
     * Requires @EnableAsync on a @Configuration class.
     */
    @Async("embeddingExecutor")
    public void generateAndStoreEmbeddingAsync(Property p) {
        try {
            List<Double> vector = embeddingService.generateEmbedding(p.getSearchableText());
            if (vector.isEmpty()) {
                log.warn("Empty embedding returned for property id={}", p.getId());
                return;
            }
            String pgVector = "[" + vector.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")) + "]";
            repository.updateEmbedding(p.getId(), pgVector);
            log.info("Embedding stored for property id={}", p.getId());
        } catch (Exception e) {
            log.error("Embedding generation failed for property id={}: {}", p.getId(), e.getMessage());
            // Non-fatal — property is saved; semantic search just won't include it until next update
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Maps a PropertyRequest to a new Property entity.
     * Does NOT use BeanUtils.copyProperties to avoid copying unexpected nulls.
     */
    private Property mapRequestToNewProperty(PropertyRequest req) {
        Property p = new Property();
        applyNonNullUpdates(req, p);
        return p;
    }

    /**
     * Applies only non-null fields from request to the target property.
     * This gives PATCH semantics: only fields present in the request are updated;
     * existing values are preserved for absent/null fields.
     *
     * BUG FIX: original used BeanUtils.copyProperties which copies null values,
     * wiping DB data for any field the client didn't include in the request body.
     */
    private void applyNonNullUpdates(PropertyRequest req, Property p) {
        if (req.getTitle()           != null) p.setTitle(req.getTitle());
        if (req.getLocation()        != null) p.setLocation(req.getLocation());
        if (req.getPrice()           != null) p.setPrice(req.getPrice());
        if (req.getType()            != null) p.setType(req.getType());
        if (req.getSqft()            != null) p.setSqft(req.getSqft());
        if (req.getBhk()             != null) p.setBhk(req.getBhk());
        if (req.getBedrooms()        != null) p.setBedrooms(req.getBedrooms());
        if (req.getBathrooms()       != null) p.setBathrooms(req.getBathrooms());
        if (req.getDescription()     != null) p.setDescription(req.getDescription());
        if (req.getFacing()          != null) p.setFacing(req.getFacing());
        if (req.getFurnishing()      != null) p.setFurnishing(req.getFurnishing());
        if (req.getParking()         != null) p.setParking(req.getParking());
        if (req.getMaintenance()     != null) p.setMaintenance(req.getMaintenance());
        if (req.getCarpetArea()      != null) p.setCarpetArea(req.getCarpetArea());
        if (req.getBuiltupArea()     != null) p.setBuiltupArea(req.getBuiltupArea());
        if (req.getLandArea()        != null) p.setLandArea(req.getLandArea());
        if (req.getLatitude()        != null) p.setLatitude(req.getLatitude());
        if (req.getLongitude()       != null) p.setLongitude(req.getLongitude());
        if (req.getVideoUrl()        != null) p.setVideoUrl(req.getVideoUrl());
        if (req.getBrochureFile()    != null) p.setBrochureFile(req.getBrochureFile());
        if (req.getSlug()            != null) p.setSlug(req.getSlug());
        if (req.getDeveloperName()   != null) p.setDeveloperName(req.getDeveloperName());
        if (req.getListingType()     != null) p.setListingType(req.getListingType());
        if (req.getPossessionStatus()!= null) p.setPossessionStatus(req.getPossessionStatus());
        if (req.getPossessionDate()  != null) p.setPossessionDate(req.getPossessionDate());
        if (req.getVastuCompliant()  != null) p.setVastuCompliant(req.getVastuCompliant());
        if (req.getResale()          != null) p.setResale(req.getResale());
        // Boolean primitives — always apply (they have explicit defaults)
        p.setReraApproved(req.isReraApproved());
        p.setSoldOut(req.isSoldOut());
        // Collections — only overwrite if explicitly provided
        if (req.getImages()     != null) p.setImages(req.getImages());
        if (req.getMainImages() != null) p.setMainImages(req.getMainImages());
        if (req.getAmenities()  != null) p.setAmenities(req.getAmenities());
        if (req.getNearby()     != null) p.setNearby(req.getNearby());
        // Legacy image field
        if (req.getImage()      != null) p.setImage(req.getImage());
    }

    /** Prefixes a single URL with storageBaseUrl if it's a relative path. */
    private String prefix(String url) {
        if (url == null || url.isBlank()) return null;
        if (url.startsWith("http"))       return url;
        return storageBaseUrl + (url.startsWith("/") ? url : "/" + url);
    }

    /** Prefixes a list of URLs, filtering out nulls. */
    private List<String> prefixList(List<String> urls) {
        if (urls == null) return List.of();
        return urls.stream()
                .filter(u -> u != null && !u.isBlank())
                .map(this::prefix)
                .collect(Collectors.toList());
    }
}
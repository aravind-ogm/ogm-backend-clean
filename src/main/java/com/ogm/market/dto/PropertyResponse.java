package com.ogm.market.dto;

import com.ogm.market.model.NearbyLocation;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO returned to the frontend for all property endpoints.
 * Never exposes the raw Property entity (which contains embedding float[],
 * Hibernate proxies, and internal DB fields).
 */
@Data
@Builder
public class PropertyResponse {

    // ── Identity ──────────────────────────────────────────────────────────────
    private Long   id;
    private String title;
    private String slug;

    // ── Location ──────────────────────────────────────────────────────────────
    private String location;
    private Double latitude;
    private Double longitude;
    private String googleMapsUrl;

    // ── Price ────────────────────────────────────────────────────────────────
    /** Formatted: "₹3.85 Cr", "₹56.00 L" */
    private String price;
    /** Raw numeric for frontend sorting/filtering */
    private Double priceRaw;

    // ── Type & size ───────────────────────────────────────────────────────────
    private String  type;
    private String  bhk;           // "3 BHK", "4 BHK"
    private Integer sqft;
    private Integer bedrooms;
    private Integer bathrooms;
    private String  carpetArea;
    private String  builtupArea;
    private String  landArea;

    // ── Details ───────────────────────────────────────────────────────────────
    private String facing;
    private String furnishing;
    private String parking;
    private String maintenance;
    private String description;

    // ── Developer & listing ───────────────────────────────────────────────────
    private String developerName;
    private String listingType;      // "owner" | "developer"
    private String possessionStatus; // "ready_to_move" | "under_construction" etc.
    private String possessionDate;   // ISO string "2025-06-01"

    // ── Status flags ──────────────────────────────────────────────────────────
    private boolean reraApproved;
    private boolean vastuCompliant;
    private boolean soldOut;

    // ── Media ─────────────────────────────────────────────────────────────────
    private String       image;        // legacy single image
    private List<String> mainImages;
    private List<String> images;
    private String       videoUrl;
    private String       brochureFile;

    // ── Collections ───────────────────────────────────────────────────────────
    private List<String>       amenities;
    private List<NearbyLocation> nearby;
}
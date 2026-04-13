package com.ogm.market.dto;

import com.ogm.market.model.NearbyLocation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PropertyRequest {

    // ── Core ─────────────────────────────────────────────────────────────────
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Location is required")
    private String location;

    // BUG FIX #1: was String — but Property.price is Double.
    // BeanUtils.copyProperties silently failed, price was never saved.
    @Positive(message = "Price must be positive")
    private Double price;

    private String type;

    // BUG FIX #2: was String — but Property.sqft is Integer.
    // Caused a type mismatch, sqft was never saved correctly.
    @Positive(message = "Sqft must be positive")
    private Integer sqft;

    // ── BHK & rooms ──────────────────────────────────────────────────────────
    // BUG FIX #3: bhk field was completely missing from the request DTO.
    // Without it, the bhk column was never populated via the API.
    private String  bhk;
    private Integer bedrooms;
    private Integer bathrooms;

    // ── Status flags ──────────────────────────────────────────────────────────
    private boolean reraApproved;
    private boolean soldOut;

    // BUG FIX: vastuCompliant, resale were missing
    private Boolean vastuCompliant;
    private Boolean resale;

    // ── Media ─────────────────────────────────────────────────────────────────
    private String       image;        // legacy single image
    private List<String> mainImages;
    private List<String> images;
    private String       videoUrl;
    private String       brochureFile; // BUG FIX: was missing

    // ── Specs ──────────────────────────────────────────────────────────────────
    private String carpetArea;
    private String builtupArea;
    private String landArea;
    private String parking;
    private String maintenance;
    private String facing;
    private String furnishing;

    // ── Description & amenities ───────────────────────────────────────────────
    private String       description;
    private List<String> amenities;
    private List<NearbyLocation> nearby;

    // ── Developer & listing ───────────────────────────────────────────────────
    // BUG FIX: all 5 fields were missing — could never be set via API
    private String    developerName;
    private String    listingType;      // "owner" | "developer"
    private String    possessionStatus; // "ready_to_move" | "under_construction" etc.
    private LocalDate possessionDate;

    // ── SEO ───────────────────────────────────────────────────────────────────
    private String slug; // BUG FIX: was missing — auto-generated if null

    // ── Location ──────────────────────────────────────────────────────────────
    // BUG FIX: latitude/longitude were missing — map pins never saved
    private Double latitude;
    private Double longitude;
}
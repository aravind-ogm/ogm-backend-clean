package com.ogm.market.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PropertyCardResponse {

    // ── Core identity ────────────────────────────────────────────────────────
    private Long   id;
    private String title;
    private String slug;

    // ── Price & location ─────────────────────────────────────────────────────
    private String price;         // formatted: "₹3.85 Crores"
    private String location;

    // ── Property specs ───────────────────────────────────────────────────────
    private String  type;
    private String  bhk;          // BUG FIX: was missing — "3 BHK", "4 BHK" etc.
    private Integer sqft;
    private Integer bedrooms;
    private Integer bathrooms;
    private String  facing;
    private String  furnishing;
    private String  maintenance;  // BUG FIX: was missing — "₹3,500/month"
    private String  description;

    // ── Developer & listing ──────────────────────────────────────────────────
    private String  developerName;  // BUG FIX: was missing
    private String  listingType;    // BUG FIX: was missing — "owner" | "developer"
    private String  possessionStatus; // BUG FIX: was missing — "ready_to_move" etc.

    // ── Media ────────────────────────────────────────────────────────────────
    private String       primaryImage;
    private List<String> gallery;

    // ── Location & map ───────────────────────────────────────────────────────
    private Double latitude;
    private Double longitude;
    private String distanceLabel;   // BUG FIX: was missing — "2.4 km away"
    private String googleMapsUrl;

    // ── Status flags ─────────────────────────────────────────────────────────
    private boolean reraApproved;
    private boolean vastuCompliant; // BUG FIX: was missing
    private boolean soldOut;

    // ── Amenities & highlights ────────────────────────────────────────────────
    private List<String> amenities;
    private List<String> highlights;
}
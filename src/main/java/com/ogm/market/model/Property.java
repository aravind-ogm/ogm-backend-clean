package com.ogm.market.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "properties",
        indexes = {
                @Index(name = "idx_property_slug",      columnList = "slug"),
                @Index(name = "idx_property_location",  columnList = "location"),
                @Index(name = "idx_property_type",      columnList = "type"),
                @Index(name = "idx_property_price",     columnList = "price"),
                @Index(name = "idx_property_bedrooms",  columnList = "bedrooms"),
                @Index(name = "idx_property_developer", columnList = "developer_name"),
                @Index(name = "idx_property_possession",columnList = "possession_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 150)
    private String location;

    @Column
    private Double price;

    @Column(length = 1000)
    private String image;

    @Column(length = 100)
    private String type;

    private Integer sqft;

    @Builder.Default
    private boolean reraApproved = false;

    @Builder.Default
    private boolean soldOut = false;

    @Column(name = "brochure_file", length = 1000)
    private String brochureFile;

    /* ─── MAIN IMAGES ─────────────────────────────────────────────── */
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "property_main_images", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "main_image_url", length = 1000)
    private List<String> mainImages = new ArrayList<>();

    /* ─── GALLERY IMAGES ──────────────────────────────────────────── */
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "property_images", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "image_url", length = 1000)
    private List<String> images = new ArrayList<>();

    /* ─── AMENITIES ───────────────────────────────────────────────── */
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "property_amenities", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "amenity")
    private List<String> amenities = new ArrayList<>();

    /* ─── PROPERTY DETAILS ────────────────────────────────────────── */
    private Integer bedrooms;
    private Integer bathrooms;

    @Column(length = 50) private String carpetArea;
    @Column(length = 50) private String landArea;
    @Column(length = 50) private String builtupArea;
    @Column(length = 50) private String parking;
    @Column(length = 50) private String maintenance;
    @Column(length = 50) private String facing;
    @Column(length = 50) private String furnishing;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 1000)
    private String videoUrl;

    private Double latitude;
    private Double longitude;

    /* ─── NEARBY LOCATIONS ────────────────────────────────────────── */
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "property_nearby", joinColumns = @JoinColumn(name = "property_id"))
    private List<NearbyLocation> nearby = new ArrayList<>();

    /* ─── SEO SLUG ────────────────────────────────────────────────── */
    @Column(unique = true, nullable = false, length = 150)
    private String slug;

    /* ═══════════════════════════════════════════════════════════════
       NEW FIELDS — added for AI search (scenarios 7–19)
       Existing data rows will default to NULL / false automatically.
       ═══════════════════════════════════════════════════════════════ */

    /**
     * Builder / developer name (e.g. "Prestige", "Brigade", "Sobha").
     * Case 7: "Find Prestige Developers projects in Sarjapur"
     */
    @Column(name = "developer_name", length = 100)
    private String developerName;

    /**
     * Whether the property is Vastu-compliant.
     * Case 14: "Find Vastu compliant homes"
     * Boxed Boolean (not primitive) so existing rows with NULL don't crash.
     */
    @Column(name = "vastu_compliant")
    private Boolean vastuCompliant;

    /**
     * Project possession status.
     * Values: ready_to_move | new_launch | under_construction | pre_launch
     * Cases 16, 18.
     */
    @Column(name = "possession_status", length = 50)
    private String possessionStatus;

    /**
     * Expected / actual possession date.
     * Case 18: "possession before 2029 December"
     */
    @Column(name = "possession_date")
    private LocalDate possessionDate;

    /**
     * Who listed the property.
     * Values: owner | developer | builder
     * Case 17: "properties listed by owner only"
     */
    @Column(name = "listing_type", length = 30)
    private String listingType;

    /**
     * True = resale property; false = new / direct from developer.
     * Case 16: "avoid any resale projects"
     * Boxed Boolean (not primitive) so existing rows with NULL don't crash.
     */
    @Column(name = "resale")
    private Boolean resale;

    /* ─── AUTO SLUG ───────────────────────────────────────────────── */
    @PrePersist
    @PreUpdate
    public void prepareData() {
        if (this.title != null) this.title = this.title.trim();
        if ((this.slug == null || this.slug.isBlank()) && this.title != null) {
            this.slug = this.title.toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("(^-|-$)", "");
        }
    }

    /* ─── COMPUTED HELPERS ────────────────────────────────────────── */
    public String getGoogleMapsUrl() {
        if (latitude != null && longitude != null) {
            return "https://www.google.com/maps?q=" + latitude + "," + longitude;
        }
        return null;
    }

    public String getPrimaryImage() {
        if (mainImages != null && !mainImages.isEmpty()) return mainImages.get(0);
        if (images     != null && !images.isEmpty())     return images.get(0);
        return image;
    }

    public String getFormattedPrice() {
        if (price == null) return null;
        if (price >= 10_000_000) return String.format("₹%.2f Cr", price / 10_000_000);
        if (price >= 100_000)    return String.format("₹%.2f L",  price / 100_000);
        return "₹" + price;
    }

    /* ─── AI EMBEDDING (pgvector) ─────────────────────────────────── */
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "embedding", columnDefinition = "vector(768)")
    private float[] embedding;
}
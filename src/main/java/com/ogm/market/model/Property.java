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
                @Index(name = "idx_property_slug", columnList = "slug"),
                @Index(name = "idx_property_location", columnList = "location"),
                @Index(name = "idx_property_type", columnList = "type"),
                @Index(name = "idx_property_price", columnList = "price"),
                @Index(name = "idx_property_bedrooms", columnList = "bedrooms"),
                @Index(name = "idx_property_bhk", columnList = "bhk"),
                @Index(name = "idx_property_developer", columnList = "developer_name"),
                @Index(name = "idx_property_possession", columnList = "possession_status"),
                @Index(name = "idx_property_rera", columnList = "rera_approved"),
                @Index(name = "idx_property_vastu", columnList = "vastu_compliant"),
                @Index(name = "idx_property_listing_type", columnList = "listing_type"),
                @Index(name = "idx_property_resale", columnList = "resale"),
                @Index(name = "idx_property_loc_price", columnList = "location, price")
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

    @Column(name = "bhk", length = 20)
    private String bhk;

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "property_main_images",
            joinColumns = @JoinColumn(name = "property_id"),
            indexes = @Index(name = "idx_main_images_prop", columnList = "property_id")
    )
    @Column(name = "main_image_url", length = 1000)
    private List<String> mainImages = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "property_images",
            joinColumns = @JoinColumn(name = "property_id"),
            indexes = @Index(name = "idx_gallery_prop", columnList = "property_id")
    )
    @Column(name = "image_url", length = 1000)
    private List<String> images = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "property_amenities",
            joinColumns = @JoinColumn(name = "property_id"),
            indexes = @Index(name = "idx_amenities_prop", columnList = "property_id")
    )
    @Column(name = "amenity")
    private List<String> amenities = new ArrayList<>();

    private Integer bedrooms;
    private Integer bathrooms;

    @Column(length = 50)
    private String carpetArea;
    @Column(length = 50)
    private String landArea;
    @Column(length = 50)
    private String builtupArea;
    @Column(length = 50)
    private String parking;
    @Column(length = 50)
    private String maintenance;
    @Column(length = 50)
    private String facing;
    @Column(length = 50)
    private String furnishing;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 1000)
    private String videoUrl;

    private Double latitude;
    private Double longitude;

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "property_nearby",
            joinColumns = @JoinColumn(name = "property_id"),
            indexes = @Index(name = "idx_nearby_prop", columnList = "property_id")
    )
    private List<NearbyLocation> nearby = new ArrayList<>();

    @Column(unique = true, nullable = false, length = 150)
    private String slug;

    @Column(name = "developer_name", length = 100)
    private String developerName;

    @Column(name = "vastu_compliant")
    private Boolean vastuCompliant;

    @Column(name = "possession_status", length = 50)
    private String possessionStatus;

    @Column(name = "possession_date")
    private LocalDate possessionDate;

    @Column(name = "listing_type", length = 30)
    private String listingType;

    @Column(name = "resale")
    private Boolean resale;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "embedding", columnDefinition = "vector(768)")
    private float[] embedding;

    @PrePersist
    @PreUpdate
    public void prepareData() {
        if (this.title != null) {
            this.title = this.title.trim();
        }
        if ((this.slug == null || this.slug.isBlank()) && this.title != null) {
            this.slug = this.title.toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("(^-|-$)", "");
        }
        if ((this.bhk == null || this.bhk.isBlank())
                && this.bedrooms != null
                && this.bedrooms > 0) {
            this.bhk = String.valueOf(this.bedrooms);
        }
    }

    public String getGoogleMapsUrl() {
        if (latitude != null && longitude != null) {
            return "https://www.google.com/maps?q=" + latitude + "," + longitude;
        }
        return null;
    }

    public String getPrimaryImage() {
        if (mainImages != null && !mainImages.isEmpty()) return mainImages.get(0);
        if (images != null && !images.isEmpty()) return images.get(0);
        return image;
    }

    public String getFormattedPrice() {
        if (price == null) return null;
        if (price >= 10_000_000) return String.format("₹%.2f Cr", price / 10_000_000);
        if (price >= 100_000) return String.format("₹%.2f L", price / 100_000);
        return "₹" + price.longValue();
    }

    public String getBhkDisplay() {
        if (bhk != null && !bhk.isBlank()) {
            return bhk.contains("BHK") || bhk.contains("bhk") ? bhk : bhk + " BHK";
        }
        if (bedrooms != null && bedrooms > 0) {
            return bedrooms + " BHK";
        }
        return null;
    }

    public String getSearchableText() {
        StringBuilder sb = new StringBuilder();
        appendIfNotNull(sb, title);
        appendIfNotNull(sb, location);
        appendIfNotNull(sb, type);
        appendIfNotNull(sb, description);
        appendIfNotNull(sb, furnishing);
        appendIfNotNull(sb, facing);
        appendIfNotNull(sb, possessionStatus);
        appendIfNotNull(sb, developerName);
        if (Boolean.TRUE.equals(vastuCompliant)) sb.append("vastu compliant ");
        if (reraApproved) sb.append("rera approved ");
        if (amenities != null) {
            amenities.forEach(a -> sb.append(a).append(" "));
        }
        return sb.toString().trim();
    }

    private void appendIfNotNull(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) sb.append(value).append(" ");
    }
}
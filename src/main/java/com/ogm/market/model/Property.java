package com.ogm.market.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "properties",
        indexes = {
                @Index(name = "idx_property_slug", columnList = "slug"),
                @Index(name = "idx_property_location", columnList = "location"),
                @Index(name = "idx_property_type", columnList = "type")
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

    @Column(nullable = false)
    private String title;

    private String location;

    private Double price;

    private String image;

    private String type;

    private String sqft;

    @Builder.Default
    private boolean reraApproved = false;

    @Builder.Default
    private boolean soldOut = false;

    @Column(name = "brochure_file")
    private String brochureFile;

    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "property_main_images",
            joinColumns = @JoinColumn(name = "property_id")
    )
    @Column(name = "main_image_url", length = 1000)
    private List<String> mainImages = new ArrayList<>();


    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "property_images",
            joinColumns = @JoinColumn(name = "property_id")
    )
    @Column(name = "image_url", length = 1000)
    private List<String> images = new ArrayList<>();
    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "property_amenities",
            joinColumns = @JoinColumn(name = "property_id")
    )
    @Column(name = "amenity")
    private List<String> amenities = new ArrayList<>();
    private String bedrooms;
    private String bathrooms;
    private String carpetArea;
    private String landArea;
    private String builtupArea;
    private String parking;
    private String maintenance;
    private String facing;
    private String furnishing;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String videoUrl;
    private Double latitude;
    private Double longitude;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "property_nearby",
            joinColumns = @JoinColumn(name = "property_id")
    )
    private List<NearbyLocation> nearby = new ArrayList<>();

    @Column(unique = true, nullable = false, length = 150)
    private String slug;

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
    }
}
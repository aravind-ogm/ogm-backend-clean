package com.ogm.market.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String location;
    private String price;
    private String image;
    private String type;
    private String sqft;
    private boolean reraApproved;
    private boolean soldOut;
    @Column(name = "brochure_file")
    private String brochureFile;
    @ElementCollection
    @CollectionTable(name = "property_main_images", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "main_image_url")
    private List<String> mainImages;
    @ElementCollection
    private List<String> amenities;
    @ElementCollection
    @CollectionTable(name = "property_images", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "image_url", length = 1000)
    private List<String> images = new ArrayList<>();
    private Integer bedrooms;
    private Integer bathrooms;
    private String carpetArea;
    private String builtupArea;
    private String parking;
    private String maintenance;
    private String facing;
    private String furnishing;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String videoUrl;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "property_nearby",
            joinColumns = @JoinColumn(name = "property_id")
    )
    private List<NearbyLocation> nearby;
}

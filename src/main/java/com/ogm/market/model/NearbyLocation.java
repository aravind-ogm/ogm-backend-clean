package com.ogm.market.model;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyLocation {

    private String name;       // e.g., "Bier Library Restaurant"
    private String distance;   // e.g., "1.2 km"
    private String category;   // e.g., "Restaurant"
    private String image;      // URL or relative path
}

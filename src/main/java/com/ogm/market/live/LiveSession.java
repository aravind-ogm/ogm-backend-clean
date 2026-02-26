package com.ogm.market.live;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long propertyId;

    private Long agentId;

    private String customerName;

    private String customerMobile;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Long durationSeconds;
}
package com.ogm.market.live;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_call")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduledCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long agentId;
    private Long propertyId;
    private String customerName;
    private String customerMobile;
    private String note;
    private LocalDateTime scheduledAt;

    /** UPCOMING | COMPLETED | CANCELLED */
    @Builder.Default
    private String status = "UPCOMING";

    /** CUSTOMER_BOOKING | AGENT_SCHEDULED | AUTO_QUEUE */
    private String source;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status    == null) status    = "UPCOMING";
    }
}
package com.ogm.market.live;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agent_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long agentId;

    /**
     * Day of week: MON, TUE, WED, THU, FRI, SAT, SUN
     */
    private String dayOfWeek;

    /**
     * Start time in HH:mm format, e.g. "09:00"
     */
    private String startTime;

    /**
     * End time in HH:mm format, e.g. "18:00"
     */
    private String endTime;

    /**
     * Whether this day is active / enabled
     */
    private boolean active;
}
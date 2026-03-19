package com.ogm.market.live;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CallHistoryDto {

    private Long sessionId;
    private String customerName;
    private String customerMobile;
    private Long propertyId;
    private String propertyTitle;

    /**
     * COMPLETED | MISSED | UPCOMING
     */
    private String status;

    private Long durationSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    /** Formatted duration string e.g. "12 min 34 sec" */
    private String durationFormatted;
}
package com.ogm.market.live;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ScheduledCallDto {
    private Long   id;
    private Long   agentId;
    private Long   propertyId;
    private String propertyTitle;
    private String customerName;
    private String customerMobile;
    private String note;
    private LocalDateTime scheduledAt;
    private String scheduledAtFormatted;  // "Mar 20 • 10:30 AM"
    private String status;
    private String source;
}
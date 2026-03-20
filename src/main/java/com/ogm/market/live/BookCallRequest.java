package com.ogm.market.live;

import lombok.Data;

/**
 * Used by:
 *  1. Customer booking from property page  (source = CUSTOMER_BOOKING)
 *  2. Agent manually scheduling             (source = AGENT_SCHEDULED)
 */
@Data
public class BookCallRequest {
    private Long   agentId;
    private Long   propertyId;
    private String customerName;
    private String customerMobile;
    private String scheduledAt;   // ISO string: "2026-03-20T10:30:00"
    private String note;
    private String source;        // CUSTOMER_BOOKING | AGENT_SCHEDULED
}
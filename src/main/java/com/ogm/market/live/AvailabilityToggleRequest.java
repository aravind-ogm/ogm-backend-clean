package com.ogm.market.live;

import lombok.Data;

@Data
public class AvailabilityToggleRequest {
    private Long agentId;
    private Long propertyId;
    private boolean online;
    private boolean busy;
}
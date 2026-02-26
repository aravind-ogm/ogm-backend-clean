package com.ogm.market.live;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AvailabilityResponse {

    private boolean online;
    private boolean busy;
    private Long queueCount;
}
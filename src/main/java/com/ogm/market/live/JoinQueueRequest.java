package com.ogm.market.live;

import lombok.*;

@Getter
@Setter
public class JoinQueueRequest {

    private Long propertyId;
    private String name;
    private String mobile;
    private String roomName;
}
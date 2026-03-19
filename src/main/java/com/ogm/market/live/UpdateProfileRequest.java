package com.ogm.market.live;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private Long agentId;
    private String name;
    private String phone;
}
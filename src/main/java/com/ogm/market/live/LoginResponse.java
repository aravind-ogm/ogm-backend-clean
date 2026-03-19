package com.ogm.market.live;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private Long agentId;
    private String name;
    private String email;
    private String phone;
    private String photoUrl;
    private String designation;
    private String token; // JWT in production; session token for now
}
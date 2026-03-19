package com.ogm.market.live;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
package com.ogm.market.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified auth response.
 * token and email are null when otpRequired = true.
 * JsonInclude omits null fields from the JSON output.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String token;
    private String email;
    private Boolean otpRequired;

    /** Convenience factory for a successful login. */
    public static AuthResponse success(String token, String email) {
        return AuthResponse.builder().token(token).email(email).build();
    }

    /** Convenience factory for OTP-required response. */
    public static AuthResponse otpRequired() {
        return AuthResponse.builder().otpRequired(true).build();
    }
}
package com.ogm.market.broker;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class BrokerDto {

    // ── REQUEST DTOs ──────────────────────────────────────────────────

    /** POST /api/broker/verify-phone */
    @Data
    public static class VerifyPhoneRequest {
        @NotBlank(message = "Verification URL is required")
        private String userJsonUrl;
    }

    /** POST /api/broker/verify-email */
    @Data
    public static class VerifyEmailRequest {
        @NotBlank(message = "Verification URL is required")
        private String userJsonUrl;
    }

    /** POST /api/broker/register */
    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Full name is required")
        @Size(min = 3, max = 150)
        private String fullName;

        @NotBlank(message = "Company name is required")
        @Size(max = 200)
        private String companyName;

        private String reraNumber;

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        private String mobile;

        @NotBlank(message = "Email address is required")
        @Email(message = "Enter a valid email address")
        private String email;

        @NotBlank(message = "Office address is required")
        @Size(min = 10, message = "Please enter a complete address")
        private String officeAddress;

        @NotEmpty(message = "Select at least one operating area")
        private List<String> operatingAreas;

        @NotEmpty(message = "Select at least one property type")
        private List<String> propertyTypes;

        private Integer numberOfProperties;

        @AssertTrue(message = "You must agree to the Terms & Conditions")
        private boolean agreedToTerms;

        // Verification flags set by frontend after phone.email verification
        private boolean mobileVerified;
        private boolean emailVerified;
    }

    /** POST /api/broker/login */
    @Data
    public static class LoginRequest {
        @NotBlank(message = "Verification URL is required")
        private String userJsonUrl; // from phone.email phone widget
    }

    /** PUT /api/broker/profile */
    @Data
    public static class UpdateProfileRequest {
        @Size(min = 3, max = 150)
        private String fullName;
        @Size(max = 200)
        private String companyName;
        private String reraNumber;
        private String officeAddress;
        private List<String> operatingAreas;
        private List<String> propertyTypes;
        private Integer numberOfProperties;
    }

    // ── RESPONSE DTOs ─────────────────────────────────────────────────

    @Data
    @lombok.AllArgsConstructor
    public static class MessageResponse {
        private String message;
    }

    @Data
    @lombok.AllArgsConstructor
    public static class VerifyPhoneResponse {
        private boolean verified;
        private String mobile;  // 10-digit, without country code
    }

    @Data
    @lombok.AllArgsConstructor
    public static class VerifyEmailResponse {
        private boolean verified;
        private String email;
    }

    @Data
    @lombok.AllArgsConstructor
    public static class RegisterResponse {
        private boolean success;
        private UUID brokerId;
        private String message;
    }

    @Data
    @lombok.AllArgsConstructor
    public static class LoginResponse {
        private String token;
        private BrokerProfileResponse broker;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BrokerProfileResponse {
        private UUID id;
        private String fullName;
        private String companyName;
        private String reraNumber;
        private String mobile;
        private String email;
        private String officeAddress;
        private Integer numberOfProperties;
        private String status;
        private String authProvider;
        private String profileImageUrl;
        private boolean mobileVerified;
        private boolean emailVerified;
        private List<String> operatingAreas;
        private List<String> propertyTypes;
        private OffsetDateTime createdAt;
        private OffsetDateTime lastLoginAt;
    }
}
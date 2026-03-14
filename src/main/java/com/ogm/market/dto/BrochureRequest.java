package com.ogm.market.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for /api/brochure/request
 * Validation annotations ensure the controller's @Valid fires correctly.
 */
@Data
public class BrochureRequest {

    @NotNull(message = "Property ID is required")
    private Long propertyId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Mobile number is required")
    private String mobile;

    private String email;   // optional
}
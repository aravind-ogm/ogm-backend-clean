package com.ogm.market.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiRequest {

    @NotBlank(message = "Question cannot be empty")
    private String question;

    private String chatId;

    /**
     * User's current GPS latitude.
     * Required only when the query contains "near me" / "my current location".
     * The frontend should send this via the browser Geolocation API.
     */
    private Double userLatitude;

    /**
     * User's current GPS longitude.
     * Required only when the query contains "near me" / "my current location".
     */
    private Double userLongitude;

    /**
     * Human-readable location name reverse-geocoded on the frontend
     * using the free OpenStreetMap Nominatim API (no API key required).
     * e.g. "Koramangala, Bengaluru, Karnataka, India"
     *
     * Injected into the Gemini prompt so the AI can answer
     * "what is my location?" correctly without any backend geocoding call.
     */
    private String userLocationName;
}
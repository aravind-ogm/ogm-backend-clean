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
     */
    private Double userLatitude;

    /**
     * User's current GPS longitude.
     * Required only when the query contains "near me" / "my current location".
     */
    private Double userLongitude;

    /**
     * Human-readable location name reverse-geocoded on the frontend.
     * e.g. "Koramangala, Bengaluru, Karnataka, India"
     * Injected into the Gemini prompt so the AI can answer
     * "what is my location?" correctly without any backend geocoding call.
     */
    private String userLocationName;

    /**
     * Set to true by the frontend when the user's question is a distance /
     * directions query (e.g. "distance between X and Y", "route from X to Y").
     *
     * When true the controller skips Gemini + property search entirely and
     * returns an empty response — the map panel handles the route visually.
     */
    private boolean isRouteQuery;
}

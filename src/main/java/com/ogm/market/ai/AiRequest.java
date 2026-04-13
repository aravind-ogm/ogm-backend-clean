package com.ogm.market.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AiRequest {

    @NotBlank(message = "Question cannot be empty")
    @Size(max = 1000, message = "Question must not exceed 1000 characters")
    private String question;

    /** Session ID for multi-turn chat history. Generate UUID on frontend per session. */
    private String chatId;

    /**
     * User's current GPS latitude.
     * Only required when the query contains "near me" / "my current location".
     * Reverse-geocoded location name should also be sent via userLocationName.
     */
    private Double userLatitude;

    /**
     * User's current GPS longitude.
     * Only required when the query contains "near me" / "my current location".
     */
    private Double userLongitude;

    /**
     * Human-readable location name reverse-geocoded on the frontend.
     * e.g. "Koramangala, Bengaluru, Karnataka, India"
     *
     * Injected into the Gemini prompt so the AI can answer
     * "what is my location?" correctly without any backend geocoding call.
     * This avoids an extra reverse-geocoding API call on the server.
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
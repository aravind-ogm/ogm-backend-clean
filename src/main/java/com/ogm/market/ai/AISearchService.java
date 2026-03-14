package com.ogm.market.ai;

public interface AISearchService {

    /**
     * Search for properties when the user's intent is property-related.
     *
     * The full {@link AiRequest} is passed so the service can access optional
     * GPS coordinates for "near me" / radius-based searches.
     *
     * @param request the validated request from the controller
     * @return populated AiChatResponse; never null
     */
    AiChatResponse search(AiRequest request);
}
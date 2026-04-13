package com.ogm.market.ai;

/**
 * Contract for AI-powered property search.
 *
 * Implementations must:
 *  - Extract structured filters from the natural-language question via Gemini
 *  - Run a multi-strategy search cascade (structured → hybrid → semantic)
 *  - Return a populated AiChatResponse; never return null
 */
public interface AISearchService {

    /**
     * Search for properties based on the user's natural-language request.
     *
     * The full {@link AiRequest} is passed so implementations can access:
     *  - GPS coordinates for "near me" / radius-based searches
     *  - The raw question for embedding generation
     *  - Chat ID for future session-scoped personalisation
     *
     * @param request validated request from the controller
     * @return populated AiChatResponse; never null
     */
    AiChatResponse search(AiRequest request);
}
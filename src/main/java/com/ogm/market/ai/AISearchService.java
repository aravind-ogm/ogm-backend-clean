package com.ogm.market.ai;

public interface AISearchService {

    /**
     * Search for properties ONLY when the user's intent is property-related.
     * Returns null if the query doesn't warrant a property search.
     */
    AiChatResponse search(String prompt);
}
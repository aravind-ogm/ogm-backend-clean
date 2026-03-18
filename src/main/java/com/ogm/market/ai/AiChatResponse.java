package com.ogm.market.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiChatResponse {

    private String message;
    private List<PropertyCardResponse> properties;
    private boolean hasResults;

    /** Follow-up suggestions shown as clickable chips in the frontend */
    @Builder.Default
    private List<String> followUps = List.of();

    /**
     * True when the original request was a route/distance query.
     * The frontend uses this to skip rendering property cards and
     * show only the route panel on the right-hand side.
     */
    @Builder.Default
    private boolean isRouteQuery = false;
}

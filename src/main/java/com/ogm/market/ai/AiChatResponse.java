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

    @Builder.Default
    private List<String> followUps = List.of();

    @Builder.Default
    private boolean isRouteQuery = false;
}

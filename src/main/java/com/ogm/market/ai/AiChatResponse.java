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

    /** Follow-up suggestions shown as clickable chips in frontend */
    @Builder.Default
    private List<String> followUps = List.of();
}
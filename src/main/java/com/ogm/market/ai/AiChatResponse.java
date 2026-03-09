package com.ogm.market.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiChatResponse {

    private String message;              // Conversational AI text
    private List<PropertyCardResponse> properties;  // Structured property cards
    private boolean hasResults;          // Frontend rendering control

}
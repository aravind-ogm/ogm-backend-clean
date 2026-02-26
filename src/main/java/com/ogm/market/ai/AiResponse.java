package com.ogm.market.ai;

import com.ogm.market.dto.PropertyResponse;
import java.util.List;

public class AiResponse {

    private String summary;
    private List<PropertyResponse> properties;

    public AiResponse(String summary, List<PropertyResponse> properties) {
        this.summary = summary;
        this.properties = properties;
    }

    public String getSummary() {
        return summary;
    }

    public List<PropertyResponse> getProperties() {
        return properties;
    }
}
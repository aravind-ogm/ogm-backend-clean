package com.ogm.market.ai;

import com.ogm.market.dto.PropertyResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;


@Getter
@AllArgsConstructor
@Deprecated(since = "2.0", forRemoval = true)
public class AiResponse {

    private final String               summary;
    private final List<PropertyResponse> properties;
}
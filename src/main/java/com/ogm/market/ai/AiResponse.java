package com.ogm.market.ai;

import com.ogm.market.dto.PropertyResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * @deprecated Use {@link AiChatResponse} instead.
 *             This class predates the current AI chat architecture and is no longer
 *             used by any controller or service. Kept temporarily to avoid breaking
 *             any downstream references, but should be removed in the next cleanup sprint.
 */
@Getter
@AllArgsConstructor
@Deprecated(since = "2.0", forRemoval = true)
public class AiResponse {

    private final String               summary;
    private final List<PropertyResponse> properties;
}
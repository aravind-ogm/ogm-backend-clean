package com.ogm.market.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AiRequest {

    @NotBlank(message = "Question cannot be empty")
    @Size(max = 1000, message = "Question must not exceed 1000 characters")
    private String question;

    private String chatId;

    private Double userLatitude;

    private Double userLongitude;

    private String userLocationName;

    private boolean isRouteQuery;
}
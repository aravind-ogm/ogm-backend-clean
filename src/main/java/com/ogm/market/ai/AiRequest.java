package com.ogm.market.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiRequest {

    @NotBlank(message = "Question cannot be empty")
    private String question;

    /** Optional — for chat history persistence */
    private String chatId;
}
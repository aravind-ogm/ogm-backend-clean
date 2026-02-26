package com.ogm.market.ai;

import jakarta.validation.constraints.NotBlank;

public class AiRequest {

    @NotBlank
    private String question;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
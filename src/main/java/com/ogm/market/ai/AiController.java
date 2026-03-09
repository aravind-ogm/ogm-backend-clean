package com.ogm.market.ai;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    private final GeminiService geminiService;
    private final AISearchService aiSearchService;

    public AiController(GeminiService geminiService,
                        AISearchService aiSearchService) {
        this.geminiService = geminiService;
        this.aiSearchService = aiSearchService;
    }

    @PostMapping("/ask")
    public ResponseEntity<AiChatResponse> askAi(
            @Valid @RequestBody AiRequest request
    ) {

        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(
                            AiChatResponse.builder()
                                    .message("Question cannot be empty.")
                                    .hasResults(false)
                                    .build()
                    );
        }

        String userQuestion = request.getQuestion();

        // 1️⃣ Structured property search
        AiChatResponse searchResponse =
                aiSearchService.search(userQuestion);

        // 2️⃣ If properties found, optionally enhance with Gemini summary
        if (searchResponse.isHasResults()) {

            String enhancedMessage = geminiService.askGemini(
                    "User asked: " + userQuestion +
                            ". Write a short professional real estate response introducing the matching properties."
            );

            searchResponse.setMessage(enhancedMessage);
        }

        return ResponseEntity.ok(searchResponse);
    }
}
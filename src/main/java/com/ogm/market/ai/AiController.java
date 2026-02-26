package com.ogm.market.ai;

import com.ogm.market.dto.PropertyResponse;
import com.ogm.market.service.AISearchService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<AiResponse> askAi(
            @Valid @RequestBody AiRequest request
    ) {

        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new AiResponse("Question cannot be empty.", null));
        }

        // 1️⃣ Get AI summary
        String reply = geminiService.askGemini(request.getQuestion());

        // 2️⃣ Search database
        Page<PropertyResponse> results =
                aiSearchService.search(
                        request.getQuestion(),
                        PageRequest.of(0, 10)
                );

        List<PropertyResponse> properties = results.getContent();

        return ResponseEntity.ok(
                new AiResponse(reply, properties)
        );
    }
}
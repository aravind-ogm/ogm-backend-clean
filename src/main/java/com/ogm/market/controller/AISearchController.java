package com.ogm.market.controller;

import com.ogm.market.dto.PropertyResponse;
import com.ogm.market.service.AISearchService;
import com.ogm.market.service.SearchSuggestionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class AISearchController {

    private final AISearchService aiSearchService;
    private final SearchSuggestionService suggestionService;

    public AISearchController(
            AISearchService aiSearchService,
            SearchSuggestionService suggestionService
    ) {
        this.aiSearchService = aiSearchService;
        this.suggestionService = suggestionService;
    }

    @GetMapping("/search")
    public Page<PropertyResponse> search(
            @RequestParam String prompt,
            Pageable pageable
    ) {
        System.out.println("AI SEARCH HIT: " + prompt);
        return aiSearchService.search(prompt, pageable);
    }

    @GetMapping("/suggestions")
    public List<String> suggestions(@RequestParam String q) {
        return suggestionService.suggest(q);
    }
}

package com.ogm.market.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchSuggestionService {

    private static final List<String> BASE_SUGGESTIONS = List.of(
            "Find me 2 BHKs in Sarjapur Road",
            "2 BHK near Wipro Kodathi Office",
            "Villas near Electronic City",
            "Apartments under 80 Lakhs"
    );

    public List<String> suggest(String query) {
        return BASE_SUGGESTIONS.stream()
                .filter(s -> s.toLowerCase().contains(query.toLowerCase()))
                .limit(5)
                .toList();
    }
}


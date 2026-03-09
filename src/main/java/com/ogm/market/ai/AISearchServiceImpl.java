package com.ogm.market.ai;

import com.ogm.market.model.Property;
import com.ogm.market.repository.PropertyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AISearchServiceImpl implements AISearchService {

    private static final int MAX_RESULTS = 5;

    private final PropertyRepository repository;
    private final EmbeddingService embeddingService;

    public AISearchServiceImpl(PropertyRepository repository,
                               EmbeddingService embeddingService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
    }

    @Override
    public AiChatResponse search(String prompt) {

        if (prompt == null || prompt.isBlank()) {
            return emptyResponse("Please tell me what kind of property you're looking for.");
        }

        String query = prompt.toLowerCase();

        Integer bhk = extractBhk(query);
        Double maxPrice = extractPrice(query);
        String location = extractLocation(query);

        List<Double> vector = embeddingService.generateEmbedding(prompt);

        String pgVector = toPgVector(vector);

        List<Property> properties = repository.hybridSearch(
                pgVector,
                location,
                bhk,
                maxPrice,
                MAX_RESULTS
        );

        if (properties.isEmpty()) {
            return emptyResponse("I couldn't find matching properties. Try adjusting budget or location.");
        }

        List<PropertyCardResponse> cards = properties.stream()
                .map(this::toCard)
                .collect(Collectors.toList());

        return AiChatResponse.builder()
                .message("Here are the best matches I found:")
                .properties(cards)
                .hasResults(true)
                .build();
    }

    private String toPgVector(List<Double> vector) {
        return "[" + vector.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + "]";
    }

    private Integer extractBhk(String query) {
        Pattern pattern = Pattern.compile("(\\d+)\\s*bhk");
        Matcher matcher = pattern.matcher(query);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private Double extractPrice(String query) {

        Pattern crore = Pattern.compile("(\\d+(\\.\\d+)?)\\s*cr");
        Matcher crMatch = crore.matcher(query);

        if (crMatch.find()) {
            return Double.parseDouble(crMatch.group(1)) * 10000000;
        }

        Pattern lakh = Pattern.compile("(\\d+(\\.\\d+)?)\\s*lakh");
        Matcher lakhMatch = lakh.matcher(query);

        if (lakhMatch.find()) {
            return Double.parseDouble(lakhMatch.group(1)) * 100000;
        }

        return null;
    }

    private String extractLocation(String query) {

        String[] locations = {
                "sarjapur",
                "whitefield",
                "electronic city",
                "bellandur",
                "varthur",
                "marathahalli"
        };

        for (String loc : locations) {
            if (query.contains(loc)) {
                return loc;
            }
        }

        return null;
    }

    private AiChatResponse emptyResponse(String message) {
        return AiChatResponse.builder()
                .message(message)
                .hasResults(false)
                .properties(List.of())
                .build();
    }

    private PropertyCardResponse toCard(Property p) {
        return PropertyCardResponse.builder()
                .type("property_card")
                .id(p.getId())
                .title(p.getTitle())
                .price(p.getFormattedPrice())
                .location(p.getLocation())
                .sqft(p.getSqft())
                .primaryImage(p.getPrimaryImage())
                .gallery(p.getImages())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .googleMapsUrl(p.getGoogleMapsUrl())
                .reraApproved(p.isReraApproved())
                .soldOut(p.isSoldOut())
                .slug(p.getSlug())
                .build();
    }
}
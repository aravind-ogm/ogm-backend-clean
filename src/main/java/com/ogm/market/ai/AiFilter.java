package com.ogm.market.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Structured filters extracted from user's natural language query
 * by Gemini. Used to build the database search query.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiFilter {

    private String city;
    private String location;
    private String bhk;           // e.g. "2", "3"
    private Double minPrice;
    private Double maxPrice;
    private String type;          // e.g. "villa", "apartment", "plot"
    private String facing;
    private String furnishing;
    private Boolean reraApproved;
    private String keyword;       // fallback search term

    /**
     * Parse BHK string to Integer safely
     */
    public Integer getBhkAsInteger() {
        if (bhk == null || bhk.isBlank()) return null;
        try {
            return Integer.parseInt(bhk.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
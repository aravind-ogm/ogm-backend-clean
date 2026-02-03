package com.ogm.market.service;

import com.ogm.market.dto.PropertyResponse;
import com.ogm.market.model.Property;
import com.ogm.market.repository.PropertyRepository;
import com.ogm.market.util.PropertyMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AISearchServiceImpl implements AISearchService {

    private final PropertyRepository repository;
    private final PropertyMapper mapper;

    public AISearchServiceImpl(PropertyRepository repository,
                               PropertyMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)   // ✅ THIS IS THE KEY FIX
    public Page<PropertyResponse> search(String prompt, Pageable pageable) {

        if (prompt == null || prompt.isBlank()) {
            return Page.empty(pageable);
        }

        String q = prompt.toLowerCase();

        Integer bhk = extractBhk(q);
        String location = extractLocation(q);
        String type = extractType(q);

        Page<Property> page = repository.advancedSearch(
                location,   // q
                type,
                null,
                null,
                null,
                null,
                null,
                bhk,
                null,
                null,
                pageable
        );

        return page.map(mapper::toResponse);
    }

    /* ---------------- AI PARSERS ---------------- */

    private Integer extractBhk(String q) {
        if (q.contains("1 bhk")) return 1;
        if (q.contains("2 bhk")) return 2;
        if (q.contains("3 bhk")) return 3;
        if (q.contains("4 bhk")) return 4;
        return null;
    }

    private String extractLocation(String q) {
        if (q.contains("sarjapur")) return "sarjapur";
        if (q.contains("electronic city")) return "electronic city";
        if (q.contains("whitefield")) return "whitefield";
        if (q.contains("bellandur")) return "bellandur";
        return null;
    }

    private String extractType(String q) {
        if (q.contains("villa")) return "villa";
        if (q.contains("plot")) return "plot";
        return "apartment"; // safe default
    }
}

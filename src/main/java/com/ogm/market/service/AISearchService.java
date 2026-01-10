package com.ogm.market.service;

import com.ogm.market.dto.PropertyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AISearchService {
    Page<PropertyResponse> search(String prompt, Pageable pageable);
}

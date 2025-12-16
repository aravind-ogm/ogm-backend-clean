package com.ogm.market.dto;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrochureRequest {
    private Long propertyId;
    private String name;
    private String mobile;
    private String email;

}


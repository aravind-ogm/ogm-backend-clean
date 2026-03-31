package com.ogm.market.broker;

import lombok.Data;

import java.util.List;

/**
 * Request body for POST /api/broker/whatsapp-register
 * Called by frontend after user clicks "Register with WhatsApp"
 */
@Data
public class WhatsAppRegisterRequest {
    private String fullName;
    private String companyName;
    private String reraNumber;
    private String mobile;
    private String email;
    private String officeAddress;
    private Integer numberOfProperties;
    private List<String> operatingAreas;
    private List<String> propertyTypes;
    private String authProvider;
}
package com.ogm.market.broker;

import org.springframework.stereotype.Component;

@Component
public class BrokerMapper {

    public BrokerDto.BrokerProfileResponse toProfileResponse(Broker broker) {
        return BrokerDto.BrokerProfileResponse.builder()
                .id(broker.getId())
                .fullName(broker.getFullName())
                .companyName(broker.getCompanyName())
                .reraNumber(broker.getReraNumber())
                .mobile(broker.getMobile())
                .email(broker.getEmail())
                .officeAddress(broker.getOfficeAddress())
                .numberOfProperties(broker.getNumberOfProperties())
                .status(broker.getStatus().name())
                .authProvider(broker.getAuthProvider().name())
                .profileImageUrl(broker.getProfileImageUrl())
                .mobileVerified(broker.isMobileVerified())
                .emailVerified(broker.isEmailVerified())
                .operatingAreas(broker.getOperatingAreas())  // direct List<String> from JSONB
                .propertyTypes(broker.getPropertyTypes())     // direct List<String> from JSONB
                .createdAt(broker.getCreatedAt())
                .lastLoginAt(broker.getLastLoginAt())
                .build();
    }
}
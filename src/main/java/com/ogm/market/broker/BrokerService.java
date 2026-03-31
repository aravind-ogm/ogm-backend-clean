package com.ogm.market.broker;

import com.ogm.market.config.JwtUtil;
import com.ogm.market.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrokerService {

    private final PhoneEmailService phoneEmailService;
    private final BrokerNotificationService notificationService;

    // ==============================
    // REGISTER (NO DATABASE)
    // ==============================
    public BrokerDto.RegisterResponse register(BrokerDto.RegisterRequest req) {

        if (!req.isMobileVerified()) {
            throw new AuthException("Mobile number must be verified before registering.");
        }
        if (!req.isEmailVerified()) {
            throw new AuthException("Email address must be verified before registering.");
        }

        // Generate ID manually since no DB
        Broker broker = Broker.builder()
                .id(java.util.UUID.randomUUID())
                .fullName(req.getFullName())
                .companyName(req.getCompanyName())
                .reraNumber(req.getReraNumber())
                .mobile(req.getMobile())
                .email(req.getEmail())
                .officeAddress(req.getOfficeAddress())
                .numberOfProperties(req.getNumberOfProperties() != null ? req.getNumberOfProperties() : 0)
                .operatingAreas(req.getOperatingAreas() != null ? req.getOperatingAreas() : new java.util.ArrayList<>())
                .propertyTypes(req.getPropertyTypes() != null ? req.getPropertyTypes() : new java.util.ArrayList<>())
                .agreedToTerms(req.isAgreedToTerms())
                .mobileVerified(true)
                .emailVerified(true)
                .status(Broker.BrokerStatus.PENDING)
                .authProvider(Broker.AuthProvider.LOCAL)
                .build();

        // Send notifications
        notificationService.sendAllRegistrationNotifications(broker);

        log.info("[BrokerService] Broker registered (NO DB): id={} mobile={}",
                broker.getId(), broker.getMobile());

        return new BrokerDto.RegisterResponse(
                true,
                broker.getId(),
                "Registration successful. Our team will contact you shortly."
        );
    }

    // ==============================
    // VERIFY PHONE
    // ==============================
    public BrokerDto.VerifyPhoneResponse verifyPhone(String userJsonUrl) {
        String fullNumber = phoneEmailService.fetchVerifiedPhone(userJsonUrl);
        String mobile = phoneEmailService.extractMobile(fullNumber);
        return new BrokerDto.VerifyPhoneResponse(true, mobile);
    }

    // ==============================
    // VERIFY EMAIL
    // ==============================
    public BrokerDto.VerifyEmailResponse verifyEmail(String userJsonUrl) {
        String email = phoneEmailService.fetchVerifiedEmail(userJsonUrl);
        return new BrokerDto.VerifyEmailResponse(true, email);
    }
}
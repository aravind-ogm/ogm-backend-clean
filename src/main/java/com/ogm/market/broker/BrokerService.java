package com.ogm.market.broker;

import com.ogm.market.config.JwtUtil;
import com.ogm.market.exception.AuthException;
import com.ogm.market.exception.ConflictException;
import com.ogm.market.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrokerService {

    private final BrokerRepository          brokerRepository;
    private final BrokerMapper              brokerMapper;
    private final JwtUtil                   jwtUtil;
    private final PhoneEmailService         phoneEmailService;
    private final BrokerNotificationService notificationService; // ← NEW

    @Transactional
    public BrokerDto.RegisterResponse register(BrokerDto.RegisterRequest req) {

        if (!req.isMobileVerified()) {
            throw new AuthException("Mobile number must be verified before registering.");
        }
        if (!req.isEmailVerified()) {
            throw new AuthException("Email address must be verified before registering.");
        }
        if (brokerRepository.existsByEmail(req.getEmail())) {
            throw new ConflictException("An account with this email already exists.");
        }
        if (brokerRepository.existsByMobile(req.getMobile())) {
            throw new ConflictException("An account with this mobile number already exists.");
        }

        Broker broker = Broker.builder()
                .fullName(req.getFullName())
                .companyName(req.getCompanyName())
                .reraNumber(req.getReraNumber())
                .mobile(req.getMobile())
                .email(req.getEmail())
                .officeAddress(req.getOfficeAddress())
                .numberOfProperties(req.getNumberOfProperties() != null ? req.getNumberOfProperties() : 0)
                .operatingAreas(req.getOperatingAreas() != null ? req.getOperatingAreas() : new ArrayList<>())
                .propertyTypes(req.getPropertyTypes() != null ? req.getPropertyTypes() : new ArrayList<>())
                .agreedToTerms(req.isAgreedToTerms())
                .mobileVerified(true)
                .isMobileVerified(true)
                .emailVerified(true)
                .isEmailVerified(true)
                .totalListings(0)
                .totalLeads(0)
                .status(Broker.BrokerStatus.PENDING)
                .authProvider(Broker.AuthProvider.LOCAL)
                .build();

        Broker saved = brokerRepository.save(broker);

        // Fire all notifications asynchronously
        notificationService.sendAllRegistrationNotifications(saved);

        log.info("[BrokerService] Broker registered: id={} mobile={}", saved.getId(), saved.getMobile());
        return new BrokerDto.RegisterResponse(true, saved.getId(), "Registration successful");
    }

    @Transactional
    public BrokerDto.LoginResponse login(BrokerDto.LoginRequest req) {
        String fullNumber = phoneEmailService.fetchVerifiedPhone(req.getUserJsonUrl());
        String mobile     = phoneEmailService.extractMobile(fullNumber);

        Broker broker = brokerRepository.findByMobile(mobile)
                .orElseThrow(() -> new AuthException(
                        "No broker account found for this number. Please register first."));

        if (broker.getStatus() == Broker.BrokerStatus.SUSPENDED ||
                broker.getStatus() == Broker.BrokerStatus.REJECTED) {
            throw new AuthException("Your account is " +
                    broker.getStatus().name().toLowerCase() + ". Please contact support.");
        }

        broker.setLastLoginAt(OffsetDateTime.now());
        brokerRepository.save(broker);

        String token = jwtUtil.generateToken(broker.getEmail());
        log.info("[BrokerService] Broker logged in: id={}", broker.getId());
        return new BrokerDto.LoginResponse(token, brokerMapper.toProfileResponse(broker));
    }

    public BrokerDto.BrokerProfileResponse getProfile(String email) {
        return brokerMapper.toProfileResponse(
                brokerRepository.findByEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException("Broker not found")));
    }

    @Transactional
    public BrokerDto.BrokerProfileResponse updateProfile(String email,
                                                         BrokerDto.UpdateProfileRequest req) {
        Broker broker = brokerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Broker not found"));

        if (req.getFullName()           != null) broker.setFullName(req.getFullName());
        if (req.getCompanyName()        != null) broker.setCompanyName(req.getCompanyName());
        if (req.getReraNumber()         != null) broker.setReraNumber(req.getReraNumber());
        if (req.getOfficeAddress()      != null) broker.setOfficeAddress(req.getOfficeAddress());
        if (req.getNumberOfProperties() != null) broker.setNumberOfProperties(req.getNumberOfProperties());
        if (req.getOperatingAreas()     != null) broker.setOperatingAreas(req.getOperatingAreas());
        if (req.getPropertyTypes()      != null) broker.setPropertyTypes(req.getPropertyTypes());

        return brokerMapper.toProfileResponse(brokerRepository.save(broker));
    }

    public BrokerDto.VerifyPhoneResponse verifyPhone(String userJsonUrl) {
        String fullNumber = phoneEmailService.fetchVerifiedPhone(userJsonUrl);
        String mobile     = phoneEmailService.extractMobile(fullNumber);
        return new BrokerDto.VerifyPhoneResponse(true, mobile);
    }

    public BrokerDto.VerifyEmailResponse verifyEmail(String userJsonUrl) {
        String email = phoneEmailService.fetchVerifiedEmail(userJsonUrl);
        return new BrokerDto.VerifyEmailResponse(true, email);
    }
}
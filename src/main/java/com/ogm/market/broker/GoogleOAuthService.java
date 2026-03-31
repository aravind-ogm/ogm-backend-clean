package com.ogm.market.broker;

import com.ogm.market.config.JwtUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final BrokerRepository          brokerRepository;
    private final JwtUtil                   jwtUtil;
    private final BrokerNotificationService notificationService; // ← NEW

    @Transactional
    public GoogleAuthResult handleGoogleAuth(String email, String name,
                                             String googleId, String pictureUrl) {
        Optional<Broker> existingByEmail = brokerRepository.findByEmail(email);

        if (existingByEmail.isPresent()) {
            Broker broker = existingByEmail.get();
            broker.setGoogleId(googleId);
            broker.setProfileImageUrl(pictureUrl);
            brokerRepository.save(broker);

            String token = jwtUtil.generateToken(broker.getEmail());
            log.info("[GoogleOAuth] Existing broker logged in via Google: id={}", broker.getId());

            return GoogleAuthResult.builder()
                    .token(token)
                    .brokerId(broker.getId().toString())
                    .newAccount(false)
                    .email(email)
                    .build();
        }

        // New broker via Google
        Broker newBroker = Broker.builder()
                .fullName(name != null ? name : "")
                .companyName("")
                .email(email)
                .mobile("")
                .officeAddress("")
                .operatingAreas(new ArrayList<>())
                .propertyTypes(new ArrayList<>())
                .googleId(googleId)
                .profileImageUrl(pictureUrl)
                .numberOfProperties(0)
                .totalListings(0)
                .totalLeads(0)
                .emailVerified(true)
                .isEmailVerified(true)
                .mobileVerified(false)
                .isMobileVerified(false)
                .agreedToTerms(false)
                .status(Broker.BrokerStatus.PENDING)
                .authProvider(Broker.AuthProvider.GOOGLE)
                .build();

        Broker saved = brokerRepository.save(newBroker);

        // Fire notifications for new Google registrations
        notificationService.sendAllRegistrationNotifications(saved);

        String token = jwtUtil.generateToken(saved.getEmail());
        log.info("[GoogleOAuth] New broker via Google: id={} email={}", saved.getId(), email);

        return GoogleAuthResult.builder()
                .token(token)
                .brokerId(saved.getId().toString())
                .newAccount(true)
                .email(email)
                .build();
    }

    @Getter
    @lombok.Builder
    public static class GoogleAuthResult {
        private final String  token;
        private final String  brokerId;
        private final boolean newAccount;
        private final String  email;
    }
}
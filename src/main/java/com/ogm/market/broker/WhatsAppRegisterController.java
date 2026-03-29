package com.ogm.market.broker;

import com.ogm.market.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

/**
 * Handles WhatsApp registration notification.
 * Called by frontend immediately after opening WhatsApp.
 * Creates a PENDING broker record and sends confirmation email.
 */
@Slf4j
@RestController
@RequestMapping("/api/broker")
@RequiredArgsConstructor
public class WhatsAppRegisterController {

    private final BrokerRepository          brokerRepository;
    private final EmailService              emailService;
    private final BrokerNotificationService notificationService;

    /**
     * POST /api/broker/whatsapp-register
     * Creates a pending broker record and sends confirmation email.
     */
    @PostMapping("/whatsapp-register")
    public ResponseEntity<Map<String, Object>> whatsappRegister(
            @RequestBody WhatsAppRegisterRequest req) {

        log.info("[WhatsApp] Registration request from email={}", req.getEmail());

        // Skip if email is empty
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "No email provided"
            ));
        }

        try {
            // Check if already registered — avoid duplicates by email OR mobile
            boolean emailExists  = brokerRepository.existsByEmail(req.getEmail());
            boolean mobileExists = req.getMobile() != null &&
                    !req.getMobile().isBlank() &&
                    brokerRepository.existsByMobile(req.getMobile());

            if (emailExists || mobileExists) {
                // Already exists — just send confirmation email
                String lookup = emailExists ? req.getEmail() : null;
                if (lookup != null) {
                    brokerRepository.findByEmail(lookup).ifPresent(broker -> {
                        emailService.sendWhatsAppConfirmationEmail(broker);
                        log.info("[WhatsApp] Resent confirmation to existing broker email={}", req.getEmail());
                    });
                } else {
                    brokerRepository.findByMobile(req.getMobile()).ifPresent(broker -> {
                        emailService.sendWhatsAppConfirmationEmail(broker);
                        log.info("[WhatsApp] Resent confirmation to existing broker mobile={}", req.getMobile());
                    });
                }
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Confirmation email sent"
                ));
            }

            // Create a new PENDING broker record
            Broker broker = Broker.builder()
                    .fullName(req.getFullName() != null ? req.getFullName() : "")
                    .companyName(req.getCompanyName() != null ? req.getCompanyName() : "")
                    .reraNumber(req.getReraNumber())
                    .mobile(req.getMobile() != null ? req.getMobile() : "")
                    .email(req.getEmail())
                    .officeAddress(req.getOfficeAddress() != null ? req.getOfficeAddress() : "")
                    .numberOfProperties(req.getNumberOfProperties() != null ? req.getNumberOfProperties() : 0)
                    .operatingAreas(req.getOperatingAreas() != null ? req.getOperatingAreas() : new ArrayList<>())
                    .propertyTypes(req.getPropertyTypes() != null ? req.getPropertyTypes() : new ArrayList<>())
                    .agreedToTerms(true)
                    .mobileVerified(false)
                    .isMobileVerified(false)
                    .emailVerified(false)
                    .isEmailVerified(false)
                    .totalListings(0)
                    .totalLeads(0)
                    .status(Broker.BrokerStatus.PENDING)
                    .authProvider(Broker.AuthProvider.WHATSAPP)
                    .build();

            Broker saved = brokerRepository.save(broker);

            // Send WhatsApp confirmation email + admin notification
            emailService.sendWhatsAppConfirmationEmail(saved);
            emailService.sendBrokerAdminNotification(saved);

            log.info("[WhatsApp] Broker saved and emails sent: id={}", saved.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "brokerId", saved.getId().toString(),
                    "message", "Registration received. Confirmation email sent."
            ));

        } catch (Exception e) {
            log.error("[WhatsApp] Error processing registration: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Could not process registration"
            ));
        }
    }
}
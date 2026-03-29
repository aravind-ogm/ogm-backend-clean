package com.ogm.market.auth;

import com.ogm.market.exception.OtpException;
import com.ogm.market.model.Otp;
//import com.ogm.market.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;
//    private final WhatsAppService whatsAppService;

    /** SecureRandom is thread-safe and cryptographically strong. */
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${otp.expiry.minutes:5}")
    private int expiryMinutes;

    /**
     * Generate, store (hashed), and deliver an OTP to the given identifier.
     * Identifier can be an email address or an E.164 phone number.
     */
    public void sendOtp(String identifier) {
        String otp = generateOtp();

        // Delete any previous OTP for this identifier before creating a new one
        otpRepository.deleteByIdentifier(identifier);

        Otp entity = new Otp();
        entity.setIdentifier(identifier);
        entity.setOtpHash(OtpUtil.hash(otp));
        entity.setExpiryTime(LocalDateTime.now().plusMinutes(expiryMinutes));
        otpRepository.save(entity);

        deliver(identifier, otp);
    }

    /**
     * Verify an OTP for the given identifier.
     * Deletes the OTP record on success (one-time use).
     * Throws OtpException on failure so the controller stays clean.
     */
    public void verifyOtp(String identifier, String otp) {
        Otp stored = otpRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new OtpException("OTP not found. Please request a new one."));

        if (stored.getExpiryTime().isBefore(LocalDateTime.now())) {
            otpRepository.deleteByIdentifier(identifier);
            throw new OtpException("OTP has expired. Please request a new one.");
        }

        if (!OtpUtil.hash(otp).equals(stored.getOtpHash())) {
            throw new OtpException("Invalid OTP. Please check and try again.");
        }

        // Consume the OTP – it cannot be reused
        otpRepository.deleteByIdentifier(identifier);
        log.info("OTP verified successfully for identifier: {}", masked(identifier));
    }

    /* ── Private helpers ──────────────────────────────────────── */

    private String generateOtp() {
        // Produces a zero-padded 6-digit string: 000000–999999
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private void deliver(String identifier, String otp) {
        try {
            if (identifier.contains("@")) {
                emailService.sendOtp(identifier, otp);
            } else {
//                whatsAppService.sendOtp(identifier, otp);
            }
            log.info("OTP delivered to: {}", masked(identifier));
        } catch (Exception e) {
            // Log the failure but don't surface delivery internals to the caller.
            // In dev, print the OTP so you can still test without real delivery.
            log.warn("OTP delivery failed for {}: {}", masked(identifier), e.getMessage());
            log.debug("DEV FALLBACK – OTP = {}", otp);
        }
    }

    /** Mask identifier for safe logging (hide most of the email/phone). */
    private String masked(String identifier) {
        if (identifier == null || identifier.length() < 4) return "***";
        return identifier.substring(0, 3) + "***" + identifier.substring(identifier.length() - 2);
    }
}
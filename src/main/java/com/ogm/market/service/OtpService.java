package com.ogm.market.service;

import com.ogm.market.model.Otp;
import com.ogm.market.repository.OtpRepository;
import com.ogm.market.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional  // 🔥 THIS FIXES THE ERROR
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;

    @Value("${otp.expiry.minutes:5}")
    private int expiryMinutes;

    public void sendOtp(String identifier) {

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        // delete previous OTP if exists
        otpRepository.deleteByIdentifier(identifier);

        Otp entity = new Otp();
        entity.setIdentifier(identifier);
        entity.setOtpHash(OtpUtil.hash(otp));
        entity.setExpiryTime(LocalDateTime.now().plusMinutes(expiryMinutes));

        otpRepository.save(entity);

        try {
            if (identifier.contains("@")) {
                emailService.sendOtp(identifier, otp);
            } else {
                whatsAppService.sendOtp(identifier, otp);
            }
        } catch (Exception e) {
            // fallback for dev
            System.out.println("⚠ OTP DELIVERY FAILED: " + e.getMessage());
            System.out.println("DEBUG OTP = " + otp);
        }
    }

    public boolean verifyOtp(String identifier, String otp) {

        Otp stored = otpRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("OTP not found"));

        if (stored.getExpiryTime().isBefore(LocalDateTime.now())) {
            otpRepository.deleteByIdentifier(identifier);
            throw new RuntimeException("OTP expired");
        }

        boolean valid = OtpUtil.hash(otp).equals(stored.getOtpHash());

        if (valid) {
            otpRepository.deleteByIdentifier(identifier); // ✅ now works
        }

        return valid;
    }
}

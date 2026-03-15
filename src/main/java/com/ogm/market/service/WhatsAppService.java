package com.ogm.market.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * WhatsApp OTP delivery service.
 * TODO: Integrate with a provider such as Twilio, Gupshup, or the
 *       WhatsApp Business API when ready. The stub below keeps the
 *       OtpService delivery path working without a real provider.
 */
@Slf4j
@Service
public class WhatsAppService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.from-number}")
    private String fromNumber;

    @PostConstruct
    private void init() {
        Twilio.init(accountSid, authToken);
    }

    public void sendOtp(String phone, String otp) {
        try {
            Message.creator(
                    new PhoneNumber(phone),
                    new PhoneNumber(fromNumber),
                    "Your OGM OTP is: " + otp + ". Valid for 5 minutes. Do not share it."
            ).create();
            log.info("SMS sent to: {}", phone);
        } catch (Exception e) {
            log.warn("SMS delivery failed for {}: {}", phone, e.getMessage());
            log.debug("DEV FALLBACK – otp={}", otp);
        }
    }
}
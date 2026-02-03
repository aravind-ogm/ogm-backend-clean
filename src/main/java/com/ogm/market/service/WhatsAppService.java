package com.ogm.market.service;

import org.springframework.stereotype.Service;

@Service
public class WhatsAppService {

    public void sendOtp(String phone, String otp) {
        // TEMPORARY FALLBACK
        System.out.println("⚠ WhatsApp disabled. OTP for " + phone + " = " + otp);
    }
}

package com.ogm.market.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class OtpUtil {

    public static String hash(String otp) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(otp.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("OTP hash failed");
        }
    }
}

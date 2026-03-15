package com.ogm.market.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * OTP hashing utility.
 * SHA-256 is used so raw OTPs are never stored in the database.
 */
public class OtpUtil {

    private OtpUtil() {} // utility class – no instantiation

    public static String hash(String otp) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(otp.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JVM spec – this should never happen
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
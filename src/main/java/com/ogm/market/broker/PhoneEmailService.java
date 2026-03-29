package com.ogm.market.broker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogm.market.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches verified phone/email data from phone.email servers.
 *
 * How phone.email works:
 *  1. Frontend widget verifies OTP with phone.email servers
 *  2. On success, widget calls your JS listener with a user_json_url
 *     e.g. https://user.phone.email/user_abc123.json
 *  3. Your backend fetches that URL → gets verified phone/email
 *  4. The JSON is only valid once and expires quickly (security)
 */
@Slf4j
@Service
public class PhoneEmailService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Fetches verified phone number from phone.email servers.
     *
     * @param userJsonUrl URL like https://user.phone.email/user_abc123.json
     * @return verified mobile number in format "919876543210" (with country code)
     */
    public String fetchVerifiedPhone(String userJsonUrl) {
        validateUrl(userJsonUrl);

        try {
            String json = fetchJson(userJsonUrl);
            JsonNode node = objectMapper.readTree(json);

            // phone.email returns: { "user_country_code": "91", "user_phone_number": "9876543210" }
            String countryCode  = getField(node, "user_country_code", userJsonUrl);
            String phoneNumber  = getField(node, "user_phone_number", userJsonUrl);

            String fullNumber = countryCode + phoneNumber;
            log.info("[PhoneEmail] Phone verified: +{}", fullNumber);
            return fullNumber;

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("[PhoneEmail] Failed to fetch phone verification: {}", e.getMessage());
            throw new AuthException("Phone verification failed. Please try again.");
        }
    }

    /**
     * Fetches verified email from phone.email servers.
     *
     * @param userJsonUrl URL like https://user.phone.email/user_abc123.json
     * @return verified email address
     */
    public String fetchVerifiedEmail(String userJsonUrl) {
        validateUrl(userJsonUrl);

        try {
            String json = fetchJson(userJsonUrl);
            JsonNode node = objectMapper.readTree(json);

            // phone.email returns: { "user_email_id": "user@example.com" }
            String email = getField(node, "user_email_id", userJsonUrl);

            log.info("[PhoneEmail] Email verified: {}", email);
            return email;

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("[PhoneEmail] Failed to fetch email verification: {}", e.getMessage());
            throw new AuthException("Email verification failed. Please try again.");
        }
    }

    /**
     * Extracts 10-digit mobile from full number with country code.
     * "919876543210" → "9876543210"
     */
    public String extractMobile(String fullNumber) {
        // Remove any leading + or country code 91
        String cleaned = fullNumber.replaceAll("^\\+?91", "");
        return cleaned;
    }

    // ── Private helpers ───────────────────────────────────────────────

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new AuthException("Verification URL is missing.");
        }
        // Security: only allow URLs from phone.email domain
        if (!url.startsWith("https://user.phone.email/")) {
            throw new AuthException("Invalid verification source.");
        }
    }

    private String fetchJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new AuthException("Verification data unavailable or expired. Please verify again.");
        }
        return response.body();
    }

    private String getField(JsonNode node, String field, String url) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            log.warn("[PhoneEmail] Missing field '{}' in response from {}", field, url);
            throw new AuthException("Incomplete verification data. Please try again.");
        }
        return value.asText().trim();
    }
}
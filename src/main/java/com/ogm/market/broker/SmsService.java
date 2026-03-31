package com.ogm.market.broker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends SMS via MSG91 API.
 * Register at msg91.com to get API key and create SMS template.
 * All methods are @Async — non-blocking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    @Value("${msg91.api-key:}")
    private String apiKey;

    @Value("${msg91.sender-id:OGMMKT}")
    private String senderId;

    @Value("${msg91.template-id:}")
    private String templateId;

    @Value("${msg91.base-url:https://api.msg91.com/api/v5}")
    private String baseUrl;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Sends welcome SMS to broker after registration.
     * Message: "Welcome to OGM! Your Broker ID: {brokerId}. Our team will contact you within 24hrs."
     */
    @Async
    public void sendWelcomeSms(Broker broker) {
        if (broker.getMobile() == null || broker.getMobile().isBlank()) {
            log.warn("[SMS] No mobile number for broker {}, skipping SMS", broker.getId());
            return;
        }

        String mobile  = "91" + broker.getMobile(); // E.164 format for India
        String message = String.format(
                "Welcome to One Global Marketplace! Your Broker ID: %s. " +
                "Our team will contact you within 24 hours. " +
                "Start your journey at oneglobalmarketplace.com",
                broker.getId().toString().substring(0, 8).toUpperCase() // short ID
        );

        sendSms(mobile, message, broker.getId().toString());
    }

    /**
     * Core SMS send method using MSG91 Send SMS API.
     */
    private void sendSms(String mobile, String message, String brokerId) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_MSG91_API_KEY")) {
            // Dev mode — just log
            log.info("[SMS] [DEV] Would send SMS to {}: {}", mobile, message);
            return;
        }

        try {
            // MSG91 API v5 — Flow based SMS
            String requestBody = String.format("""
                {
                    "template_id": "%s",
                    "sender": "%s",
                    "short_url": "0",
                    "mobiles": "%s",
                    "VAR1": "%s",
                    "VAR2": "%s"
                }
                """,
                    templateId,
                    senderId,
                    mobile,
                    brokerId.substring(0, 8).toUpperCase(),
                    "24 hours"
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/flow/"))
                    .header("Content-Type", "application/json")
                    .header("authkey", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("[SMS] Sent successfully to mobile={} brokerId={}", mobile, brokerId);
            } else {
                log.warn("[SMS] Failed. Status={} Body={}", response.statusCode(), response.body());
            }

        } catch (Exception e) {
            log.error("[SMS] Error sending SMS to {}: {}", mobile, e.getMessage());
        }
    }
}
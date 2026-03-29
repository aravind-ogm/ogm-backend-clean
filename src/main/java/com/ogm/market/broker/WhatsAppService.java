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
 * Sends WhatsApp messages via Meta WhatsApp Cloud API.
 *
 * Setup (free):
 * 1. Go to developers.facebook.com → My Apps → Create App → Business
 * 2. Add WhatsApp product
 * 3. Get Phone Number ID and Access Token
 * 4. Create message template in WhatsApp Manager
 *
 * Free tier: 1000 conversations/month free
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

    @Value("${whatsapp.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.access-token:}")
    private String accessToken;

    @Value("${whatsapp.template-name:broker_welcome}")
    private String templateName;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Sends welcome WhatsApp message to newly registered broker.
     */
    @Async
    public void sendWelcomeWhatsApp(Broker broker) {
        if (broker.getMobile() == null || broker.getMobile().isBlank()) {
            log.warn("[WhatsApp] No mobile for broker {}, skipping", broker.getId());
            return;
        }

        if (phoneNumberId == null || phoneNumberId.isBlank() ||
            accessToken == null || accessToken.isBlank()) {
            // Dev mode — log instead
            log.info("[WhatsApp] [DEV] Would send WhatsApp to +91{}: Welcome message, BrokerID={}",
                    broker.getMobile(), broker.getId().toString().substring(0, 8));
            return;
        }

        String mobile = "91" + broker.getMobile();
        String shortId = broker.getId().toString().substring(0, 8).toUpperCase();

        // Meta WhatsApp Cloud API — template message
        String requestBody = String.format("""
            {
              "messaging_product": "whatsapp",
              "to": "%s",
              "type": "template",
              "template": {
                "name": "%s",
                "language": { "code": "en" },
                "components": [
                  {
                    "type": "body",
                    "parameters": [
                      { "type": "text", "text": "%s" },
                      { "type": "text", "text": "%s" },
                      { "type": "text", "text": "%s" }
                    ]
                  }
                ]
              }
            }
            """,
                mobile,
                templateName,
                broker.getFullName(),
                shortId,
                "24 hours"
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://graph.facebook.com/v18.0/" + phoneNumberId + "/messages"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("[WhatsApp] Sent to +91{} for broker {}", broker.getMobile(), broker.getId());
            } else {
                log.warn("[WhatsApp] Failed. Status={} Body={}", response.statusCode(), response.body());
            }

        } catch (Exception e) {
            log.error("[WhatsApp] Error: {}", e.getMessage());
        }
    }
}
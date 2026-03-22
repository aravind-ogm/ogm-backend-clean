package com.ogm.market.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebCorsConfig {

    @Value("${cors.extra-origins:}")
    private String extraOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ── Allowed origins ────────────────────────────────────────────
        // IMPORTANT: When allowCredentials = true you MUST use
        // setAllowedOriginPatterns (not setAllowedOrigins) because
        // a wildcard "*" is not permitted alongside credentials.
        List<String> origins = new ArrayList<>(List.of(
                "https://oneglobalmarketplace.com",
                "https://www.oneglobalmarketplace.com",
                "http://localhost:3000",
                "http://localhost:3001"
        ));

        if (extraOrigins != null && !extraOrigins.isBlank()) {
            for (String o : extraOrigins.split(",")) {
                String trimmed = o.trim();
                if (!trimmed.isEmpty()) origins.add(trimmed);
            }
        }

        // Use allowedOriginPatterns — required when credentials = true
        config.setAllowedOriginPatterns(origins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));

        // ── CRITICAL FIX ───────────────────────────────────────────────
        // SockJS sends requests with withCredentials: true.
        // The browser blocks the WebSocket handshake unless the server
        // responds with Access-Control-Allow-Credentials: true.
        // Changing false → true fixes the "🔴 Offline" dot.
        config.setAllowCredentials(true);

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
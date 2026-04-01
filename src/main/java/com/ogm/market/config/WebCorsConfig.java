package com.ogm.market.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class WebCorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // ✅ Allowed Origins (Production + Local + Mac + Mobile)
        config.setAllowedOriginPatterns(List.of(
                "https://oneglobalmarketplace.com",
                "https://www.oneglobalmarketplace.com",
                "https://api.oneglobalmarketplace.com",
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));

        // ✅ Allow all HTTP methods
        config.setAllowedMethods(List.of("*"));

        // ✅ Allow all headers
        config.setAllowedHeaders(List.of("*"));

        // ✅ Allow cookies / authorization headers
        config.setAllowCredentials(true);

        // ✅ Important for mobile & Mac (preflight cache)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
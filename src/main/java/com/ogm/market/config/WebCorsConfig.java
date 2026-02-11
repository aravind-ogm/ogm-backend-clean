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

        // ✅ Allow all required origins (www + non-www + http + https)
        config.setAllowedOriginPatterns(List.of(
                "https://oneglobalmarketplace.com",
                "https://www.oneglobalmarketplace.com",
                "http://oneglobalmarketplace.com",
                "http://www.oneglobalmarketplace.com",
                "http://localhost:3000"
        ));

        // ✅ Allow all HTTP methods
        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"
        ));

        // ✅ Allow all headers
        config.setAllowedHeaders(List.of("*"));

        // ✅ Important for modern browsers & mobile
        config.setAllowCredentials(true);

        // ✅ Cache preflight response for 1 hour (improves mobile performance)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}

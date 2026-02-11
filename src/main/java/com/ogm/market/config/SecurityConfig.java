package com.ogm.market.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // ✅ Enable CORS (uses WebCorsConfig)
                .cors(Customizer.withDefaults())

                // ✅ Disable CSRF for REST API
                .csrf(csrf -> csrf.disable())

                // ✅ Allow all requests (you can restrict later)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/contact/**").permitAll()
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}

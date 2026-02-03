package com.ogm.market.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                /* Disable CSRF */
                .csrf(csrf -> csrf.disable())

                /* Enable CORS */
                .cors(Customizer.withDefaults())

                /* Authorization */
                .authorizeHttpRequests(auth -> auth

                        /* ✅ PUBLIC STATIC RESOURCES (THIS FIXES 403 IMAGES) */
                        .requestMatchers(
                                new AntPathRequestMatcher("/images/**"),
                                new AntPathRequestMatcher("/videos/**"),
                                new AntPathRequestMatcher("/brochures/**"),
                                new AntPathRequestMatcher("/favicon.ico"),
                                new AntPathRequestMatcher("/logo.png")
                        ).permitAll()

                        /* ✅ PUBLIC APIs */
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/properties/**",
                                "/api/ai/**",
                                "/"
                        ).permitAll()

                        /* 🔒 Everything else requires auth */
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

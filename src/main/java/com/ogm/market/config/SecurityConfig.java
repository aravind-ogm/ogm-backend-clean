package com.ogm.market.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    /**
     * PUBLIC — no JWT required.
     * Only expose the minimum needed for unauthenticated callers.
     * /api/agent/** is intentionally NOT here — agents must be authenticated.
     */
    private static final String[] PUBLIC_PATHS = {
            // Auth
            "/api/agent/login",                    // login endpoint
            "/api/agent/book-call",                // customers book calls (no account)
            // Properties — public browsing
            "/api/properties/**",
            "/api/ai/**",
            "/api/brochure/**",
            "/api/contact/**",
            "/api/auth/**",
            // Live tour — customer-facing only (no auth)
            "/api/live-tour/availability/**",      // check if agent is online
            "/api/live-tour/join-queue",           // customer joins queue
            "/api/live-tour/jaas-token",           // get JaaS JWT (public — customers need it too)
            // WebSocket handshake — must be public for SockJS
            "/live-queue/**",
            // Static
            "/"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                new AntPathRequestMatcher("/images/**"),
                                new AntPathRequestMatcher("/videos/**"),
                                new AntPathRequestMatcher("/brochures/**"),
                                new AntPathRequestMatcher("/favicon.ico"),
                                new AntPathRequestMatcher("/logo.png")
                        ).permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        // Everything else requires a valid JWT
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
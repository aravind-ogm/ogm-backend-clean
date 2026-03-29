package com.ogm.market.config;

import com.ogm.market.broker.GoogleOAuthSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthFilter             jwtAuthFilter;
    private final GoogleOAuthSuccessHandler googleOAuthSuccessHandler;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private static final String[] PUBLIC_PATHS = {
            "/api/agent/login",
            "/api/agent/book-call",
            "/api/broker/verify-phone",
            "/api/broker/verify-email",
            "/api/broker/register",
            "/api/broker/login",
            "/api/broker/whatsapp-register",
            "/api/auth/google",
            "/oauth2/**",
            "/login/oauth2/**",
            "/api/properties/**",
            "/api/ai/**",
            "/api/brochure/**",
            "/api/contact/**",
            "/api/auth/**",
            "/api/live-tour/availability/**",
            "/api/live-tour/join-queue",
            "/api/live-tour/jaas-token",
            "/live-queue/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("[SecurityConfig] Configuring security filter chain with Google OAuth2");

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                new AntPathRequestMatcher("/images/**"),
                                new AntPathRequestMatcher("/videos/**"),
                                new AntPathRequestMatcher("/brochures/**"),
                                new AntPathRequestMatcher("/favicon.ico"),
                                new AntPathRequestMatcher("/logo.png")
                        ).permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth ->
                                auth.baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(redir ->
                                redir.baseUri("/login/oauth2/code/*"))
                        .successHandler(googleOAuthSuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            log.error("[GoogleOAuth] FAILURE HANDLER: {}", exception.getMessage(), exception);
                            response.sendRedirect(frontendUrl + "/broker/register?error=" + exception.getMessage());
                        })
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
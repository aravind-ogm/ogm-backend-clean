package com.ogm.market.broker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Handles Google OAuth2 flow for broker registration/login.
 *
 * Flow:
 *  1. Frontend → GET /api/auth/google?role=broker
 *  2. Spring Security redirects to Google login page (automatic)
 *  3. Google redirects back → GET /api/auth/google/callback
 *  4. We get user email + name from Google
 *  5. We create/find broker → generate JWT → redirect to frontend
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class GoogleOAuthController {

    private final GoogleOAuthService googleOAuthService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Step 1 — Entry point. Spring Security automatically redirects
     * to Google login when this endpoint is accessed.
     * The actual redirect is configured in SecurityConfig.
     *
     * GET /api/auth/google?role=broker
     */
    @GetMapping("/google")
    public RedirectView initiateGoogleOAuth(@RequestParam(defaultValue = "broker") String role) {
        // Spring Security OAuth2 handles the redirect to Google automatically.
        // This just redirects to the Spring Security OAuth2 authorization endpoint.
        return new RedirectView("/oauth2/authorization/google?role=" + role);
    }

    /**
     * Step 3 — Google redirects here after user logs in.
     * Spring Security processes the OAuth2 callback and populates OAuth2User.
     *
     * GET /api/auth/google/callback
     */
    @GetMapping("/google/callback")
    public RedirectView handleGoogleCallback(@AuthenticationPrincipal OAuth2User oAuth2User) {
        try {
            if (oAuth2User == null) {
                log.warn("[GoogleOAuth] OAuth2User is null — authentication failed");
                return new RedirectView(frontendUrl + "/broker/register?error=google_auth_failed");
            }

            String email    = oAuth2User.getAttribute("email");
            String name     = oAuth2User.getAttribute("name");
            String googleId = oAuth2User.getAttribute("sub");
            String picture  = oAuth2User.getAttribute("picture");

            log.info("[GoogleOAuth] Google login: email={} name={}", email, name);

            // Create or find broker + generate JWT
            GoogleOAuthService.GoogleAuthResult result =
                    googleOAuthService.handleGoogleAuth(email, name, googleId, picture);

            // Redirect to frontend with JWT token
            String redirectUrl = frontendUrl + "/broker/register?token=" + result.getToken()
                    + "&brokerId=" + result.getBrokerId()
                    + "&isNew=" + result.isNewAccount();

            return new RedirectView(redirectUrl);

        } catch (Exception e) {
            log.error("[GoogleOAuth] Error during Google callback: {}", e.getMessage(), e);
            return new RedirectView(frontendUrl + "/broker/register?error=" +
                    java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
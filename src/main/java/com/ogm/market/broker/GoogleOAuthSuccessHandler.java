package com.ogm.market.broker;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final GoogleOAuthService googleOAuthService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        log.info("==================================================");
        log.info("[GoogleOAuth] SUCCESS HANDLER CALLED");
        log.info("[GoogleOAuth] Auth type: {}", authentication.getClass().getSimpleName());
        log.info("[GoogleOAuth] Principal type: {}", authentication.getPrincipal().getClass().getSimpleName());

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attrs = oAuth2User.getAttributes();

        log.info("[GoogleOAuth] All attributes: {}", attrs.keySet());

        String email    = oAuth2User.getAttribute("email");
        String name     = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");
        String picture  = oAuth2User.getAttribute("picture");

        log.info("[GoogleOAuth] email={} name={} googleId={}", email, name, googleId);
        log.info("==================================================");

        try {
            GoogleOAuthService.GoogleAuthResult result =
                    googleOAuthService.handleGoogleAuth(email, name, googleId, picture);

            String redirectUrl = frontendUrl + "/broker/register"
                    + "?token="    + URLEncoder.encode(result.getToken(), StandardCharsets.UTF_8)
                    + "&brokerId=" + URLEncoder.encode(result.getBrokerId(), StandardCharsets.UTF_8)
                    + "&isNew="    + result.isNewAccount();

            log.info("[GoogleOAuth] Redirecting to: {}", redirectUrl);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            log.error("[GoogleOAuth] Error: {}", e.getMessage(), e);
            getRedirectStrategy().sendRedirect(request, response,
                    frontendUrl + "/broker/register?error=" +
                            URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
        }
    }
}
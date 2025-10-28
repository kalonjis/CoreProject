package be.steby.CoreProject.il.security;

import be.steby.CoreProject.bll.domains.auth.services.AuthService;
import be.steby.CoreProject.bll.domains.auth.models.LoginTokens;
import be.steby.CoreProject.bll.domains.auth.services.cookies.AuthCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles successful OAuth2 authentication.
 * This handler is called by Spring Security AFTER the OAuth2 flow is complete
 * and the OAuth2User is available in the Authentication object.
 *
 * Uses @Lazy injection to break circular dependency cycle with SecurityConfig.
 */
@Component
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final AuthCookieService authCookieService;

    @Value("${url.front_server}")
    private String frontUrl;

    // ✅ Constructeur avec @Lazy pour casser le cycle de dépendances
    public OAuth2AuthenticationSuccessHandler(
            @Lazy AuthService authService,
            AuthCookieService authCookieService) {
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        log.info("OAuth2 authentication successful");

        try {
            // ✅ Extract OAuth2User from the Authentication object
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

            // ✅ Log all attributes for debugging
            log.debug("OAuth2 raw attributes: {}", oauth2User.getAttributes());

            // Extract OAuth2 user information with proper type handling
            String provider = "GITHUB"; // Currently only GitHub supported

            // ✅ GitHub returns ID as Integer, not String - handle it properly
            Object idObj = oauth2User.getAttribute("id");
            String providerId;
            if (idObj instanceof Integer) {
                providerId = String.valueOf((Integer) idObj);
            } else if (idObj instanceof Long) {
                providerId = String.valueOf((Long) idObj);
            } else if (idObj instanceof String) {
                providerId = (String) idObj;
            } else {
                providerId = (idObj != null) ? idObj.toString() : null;
            }

            String email = oauth2User.getAttribute("email");
            String login = oauth2User.getAttribute("login");
            String name = oauth2User.getAttribute("name");

            log.info("OAuth2 user data - provider: {}, providerId: {}, email: {}, login: {}",
                    provider, providerId, email, login);

            // ✅ Validation des données obligatoires
            if (providerId == null || login == null) {
                log.error("Missing required OAuth2 data - providerId: {}, login: {}", providerId, login);
                throw new IllegalStateException("Missing required OAuth2 user data");
            }

            // BLL: Complete OAuth2 authentication
            LoginTokens tokens = authService.oauth2Login(
                    provider,
                    providerId,
                    email,
                    login,
                    name,
                    request
            );

            // PL: Set authentication cookies
            authCookieService.setAuthenticationCookies(response, tokens);

            log.info("OAuth2 login successful, redirecting to frontend");

            // Redirect to frontend home page
            response.sendRedirect(frontUrl);

        } catch (Exception e) {
            log.error("OAuth2 authentication processing failed: {}", e.getMessage(), e);

            // Redirect to login page with error
            response.sendRedirect(frontUrl + "/auth/login?error=oauth2");
        }
    }
}
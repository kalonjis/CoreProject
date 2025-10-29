package be.steby.CoreProject.il.security;

import be.steby.CoreProject.bll.domains.auth.models.LoginTokens;
import be.steby.CoreProject.bll.domains.auth.services.cookies.AuthCookieService;
import be.steby.CoreProject.bll.domains.auth.services.oauth.OAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles successful OAuth2 authentication.
 * This handler is called by Spring Security AFTER the OAuth2 flow is complete
 * and the OAuth2User is available in the Authentication object.
 *
 * Supports multiple OAuth2 providers (GitHub, Google, etc.) by dynamically
 * extracting the provider from the authentication token.
 *
 * Uses @Lazy injection to break circular dependency cycle with SecurityConfig.
 */
@Component
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthService oAuthService;
    private final AuthCookieService authCookieService;

    @Value("${url.front_server}")
    private String frontUrl;

    public OAuth2AuthenticationSuccessHandler(
            @Lazy OAuthService oAuthService,
            AuthCookieService authCookieService) {
        this.oAuthService = oAuthService;
        this.authCookieService = authCookieService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        log.info("OAuth2 authentication successful");

        try {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            log.debug("OAuth2 raw attributes: {}", oauth2User.getAttributes());

            // Extract provider dynamically from authentication token
            String provider = extractProvider(authentication);

            // Extract provider-specific attributes
            String providerId = extractProviderId(oauth2User, provider);
            String username = extractUsername(oauth2User, provider);
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");

            log.info("OAuth2 user data - provider: {}, providerId: {}, email: {}, username: {}",
                    provider, providerId, email, username);

            // Validate required data
            if (providerId == null || username == null) {
                log.error("Missing required OAuth2 data - providerId: {}, username: {}", providerId, username);
                throw new IllegalStateException("Missing required OAuth2 user data");
            }

            // Complete OAuth2 authentication
            LoginTokens tokens = oAuthService.oauth2Login(
                    provider,
                    providerId,
                    email,
                    username,
                    name,
                    request
            );

            // Set authentication cookies
            authCookieService.setAuthenticationCookies(response, tokens);

            log.info("OAuth2 login successful, redirecting to frontend");
            response.sendRedirect(frontUrl);

        } catch (Exception e) {
            log.error("OAuth2 authentication processing failed: {}", e.getMessage(), e);
            response.sendRedirect(frontUrl + "/auth/login?error=oauth2");
        }
    }

    /**
     * Extracts OAuth2 provider name from authentication token.
     * Supports any OAuth2 provider configured in Spring Security.
     *
     * @param authentication OAuth2 authentication token
     * @return Provider name in uppercase (e.g., GITHUB, GOOGLE)
     */
    private String extractProvider(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
            return registrationId.toUpperCase();
        }
        log.warn("Unable to extract provider from authentication, defaulting to UNKNOWN");
        return "UNKNOWN";
    }

    /**
     * Extracts provider ID from OAuth2User with type safety.
     * Handles different provider ID types and attribute names.
     *
     * @param oauth2User OAuth2 user principal
     * @param provider Provider name
     * @return Provider ID as String, or null if not found
     */
    private String extractProviderId(OAuth2User oauth2User, String provider) {
        Object idObj = null;

        // Different providers use different attribute names
        switch (provider) {
            case "GITHUB":
                idObj = oauth2User.getAttribute("id");
                break;
            case "GOOGLE":
                idObj = oauth2User.getAttribute("sub"); // Google uses 'sub'
                break;
            case "FACEBOOK":
                idObj = oauth2User.getAttribute("id");
                break;
            default:
                // Try common attribute names
                idObj = oauth2User.getAttribute("sub");
                if (idObj == null) {
                    idObj = oauth2User.getAttribute("id");
                }
        }

        if (idObj instanceof Integer) {
            return String.valueOf((Integer) idObj);
        } else if (idObj instanceof Long) {
            return String.valueOf((Long) idObj);
        } else if (idObj instanceof String) {
            return (String) idObj;
        } else if (idObj != null) {
            return idObj.toString();
        }

        return null;
    }

    /**
     * Extracts username from OAuth2User based on provider.
     * Different providers have different username attributes.
     *
     * @param oauth2User OAuth2 user principal
     * @param provider Provider name
     * @return Username as String, or email if no username found
     */
    private String extractUsername(OAuth2User oauth2User, String provider) {
        String username = null;

        switch (provider) {
            case "GITHUB":
                username = oauth2User.getAttribute("login");
                break;
            case "GOOGLE":
                // Google doesn't have a username, use email or name
                username = oauth2User.getAttribute("email");
                if (username == null) {
                    username = oauth2User.getAttribute("name");
                }
                // Remove domain from email for cleaner username
                if (username != null && username.contains("@")) {
                    username = username.split("@")[0];
                }
                break;
            case "FACEBOOK":
                username = oauth2User.getAttribute("name");
                break;
            default:
                // Fallback: try common attributes
                username = oauth2User.getAttribute("login");
                if (username == null) {
                    username = oauth2User.getAttribute("preferred_username");
                }
                if (username == null) {
                    String email = oauth2User.getAttribute("email");
                    if (email != null && email.contains("@")) {
                        username = email.split("@")[0];
                    }
                }
        }

        return username;
    }
}
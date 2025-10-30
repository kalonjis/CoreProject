package be.steby.CoreProject.bll.domains.auth.services.oauth;

import be.steby.CoreProject.bll.domains.auth.models.LoginTokens;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for handling OAuth2 authentication operations.
 * Manages user authentication and registration through OAuth2 providers.
 */
public interface OAuthService {

    /**
     * Authenticates or creates a user via OAuth2 provider.
     * If the user exists (matched by email or provider credentials), authenticates them.
     * If the user doesn't exist, creates a new account.
     *
     * @param provider OAuth provider name (e.g., GITHUB, GOOGLE)
     * @param providerId User ID from OAuth provider
     * @param email User email from OAuth provider (may be null)
     * @param username Username from OAuth provider
     * @param name Full name from OAuth provider (may be null)
     * @param request HTTP request for device detection
     * @return LoginTokens containing accessToken and refreshToken
     */
    LoginTokens oauth2Login(String provider, String providerId, String email,
                            String username, String name, HttpServletRequest request);
}
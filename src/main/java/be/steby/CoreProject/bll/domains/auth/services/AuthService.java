package be.steby.CoreProject.bll.domains.auth.services;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthenticationException;
import be.steby.CoreProject.bll.domains.auth.models.LoginTokens;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.RefreshToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Service interface for managing user authentication and user-related operations.
 * This interface extends {@link UserDetailsService} to provide authentication-related functionality.
 */
public interface AuthService extends UserDetailsService {

    /**
     * Authenticates a user and detects device.
     *
     * @param username Username
     * @param password Password
     * @param request HTTP request for device detection
     * @return LoginTokens containing accessToken and refreshToken
     */
    LoginTokens login(String username, String password, HttpServletRequest request);

    /**
     * Performs complete logout business logic including token revocation and events publishing.
     *
     * @param refreshTokenCookie The refresh token cookie value for token revocation
     * @param request The HTTP request for context capture and device detection
     */
    void logout(String refreshTokenCookie, HttpServletRequest request);

    /**
     * Retrieves the currently authenticated user.
     *
     * @return The authenticated user.
     */
    User getAuthenticatedUser();


    /**
     * Refreshes authentication tokens from a validated refresh token.
     *
     * @param validatedToken Validated refresh token
     * @return LoginTokens containing new access token and rotated refresh token
     */
    LoginTokens refreshAuthTokens(RefreshToken validatedToken);
}
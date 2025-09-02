package be.steby.CoreProject.bll.domains.auth.services;

import be.steby.CoreProject.dl.entities.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Service interface for managing user authentication and user-related operations.
 * This interface extends {@link UserDetailsService} to provide authentication-related functionality.
 */
public interface AuthService extends UserDetailsService {

    /**
     * Authenticates a user based on the provided credentials and performs complete login business logic.
     * This includes device detection, events publishing, and security notifications.
     *
     * @param username The username of the user to be authenticated.
     * @param password The password of the user to be authenticated.
     * @param request The HTTP request for device detection and context capture.
     * @return A {@link User} object containing the user's details.
     * @throws AuthenticationException If the login credentials are invalid or authentication fails.
     */
    User login(String username, String password, HttpServletRequest request);

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
}
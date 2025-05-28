package be.steby.CoreProject.bll.domains.auth.services;

import be.steby.CoreProject.dl.entities.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Service interface for managing user authentication and user-related operations.
 * This interface extends {@link UserDetailsService} to provide authentication-related functionality.
 * It includes methods for user login, registration, enabling/disabling user accounts,
 * email confirmation, password reset, and password change operations.
 *
 * <p>The service provides the following operations:</p>
 * <ul>
 *     <li>User login with email and password authentication.</li>
 *     <li>User logout.</li>
 *     <li>Registering a new user.</li>
 *     <li>Resetting user passwords.</li>
 *     <li>Changing user passwords.</li>
 * </ul>
 */
public interface AuthService extends UserDetailsService {

    /**
     * Authenticates a user based on the provided email and password.
     *
     * <p>This method checks the provided credentials and returns the authenticated user details along with
     * an authentication token if the login is successful.</p>
     *
     * @param username    The username of the user to be authenticated.
     * @param password The password of the user to be authenticated.
     * @return A {@link User} object containing the user's details and an authentication token.
     * AuthenticationException If the login credentials are invalid or authentication fails.
     */
    User login(String username, String password);

    void logout();


    User getAuthenticatedUser();

}


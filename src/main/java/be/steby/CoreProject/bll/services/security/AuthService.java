package be.steby.CoreProject.bll.services.security;

import be.steby.CoreProject.dl.entities.User;
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


    User register(User user);

    /**
     * Resets the password of the specified user.
     *
     * <p>This method allows a user to reset their password, typically triggered by a password reset request.</p>
     *
     * @param user     The user whose password is to be reset.
     * @param password The new password to set for the user.
     */
    void resetPassword(User user, String password);

    /**
     * Changes the password of the specified user.
     *
     * <p>This method allows the user to change their password by providing the current password
     * and the desired new password.</p>
     *
     * @param username        The username of the user whose password is to be changed.
     * @param currentPassword The user's current password.
     * @param newPassword     The new password to set for the user.
     *                        InvalidPasswordException If the current password is incorrect.
     */
    void changePassword(String username, String currentPassword, String newPassword);

}

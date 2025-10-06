package be.steby.CoreProject.bll.domains.user.exceptions;

/**
 * Exception thrown when a user cannot be found.
 * Extends UserDomainException following DDD exception hierarchy.
 *
 * This is a domain-specific exception for the user domain,
 * providing clearer semantics and proper exception hierarchy.
 */
public class UserNotFoundException extends UserDomainException {

    /**
     * Creates a new exception with a custom message.
     *
     * @param message The detailed error message
     */
    public UserNotFoundException(String message) {
        super(message, 404);
    }

    /**
     * Creates a new exception for a user not found by ID.
     *
     * @param userId The user ID that was not found
     * @return A new exception instance
     */
    public static UserNotFoundException byId(Long userId) {
        return new UserNotFoundException("User not found with ID: " + userId);
    }

    /**
     * Creates a new exception for a user not found by username.
     *
     * @param username The username that was not found
     * @return A new exception instance
     */
    public static UserNotFoundException byUsername(String username) {
        return new UserNotFoundException("User not found with username: " + username);
    }

    /**
     * Creates a new exception for a user not found by email.
     *
     * @param email The email that was not found
     * @return A new exception instance
     */
    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException("User not found with email: " + email);
    }

    /**
     * Creates a new exception for a user not found by identifier (username or email).
     *
     * @param identifier The identifier that was not found
     * @return A new exception instance
     */
    public static UserNotFoundException byIdentifier(String identifier) {
        return new UserNotFoundException("User not found with identifier: " + identifier);
    }
}
package be.steby.CoreProject.bll.domains.device.exceptions;


/**
 * Exception thrown when a user cannot be found.
 * Extends UserDomainException following DDD exception hierarchy.
 *
 * This is a domain-specific exception for the user domain,
 * providing clearer semantics and proper exception hierarchy.
 */
public class DeviceNotFoundException extends DeviceDomainException {

    /**
     * Creates a new exception with a custom message.
     *
     * @param message The detailed error message
     */
    public DeviceNotFoundException(String message) {
        super(message, 404);
    }

    /**
     * Creates a new exception for a user not found by ID.
     *
     * @param deviceId The user ID that was not found
     * @return A new exception instance
     */
    public static DeviceNotFoundException byId(Long deviceId) {
        return new DeviceNotFoundException("Device not found with ID: " + deviceId);
    }

    /**
     * Creates a new exception for a user not found by ID.
     *
     * @param publicId The user publicId that was not found
     * @return A new exception instance
     */
    public static DeviceNotFoundException byPublicId(String publicId) {
        return new DeviceNotFoundException("Device not found with publicId: " + publicId);
    }

    /**
     * Creates a new exception for a user not found by username.
     *
     * @param username The username that was not found
     * @return A new exception instance
     */
    public static DeviceNotFoundException byUsername(String username) {
        return new DeviceNotFoundException("User not found with username: " + username);
    }

    /**
     * Creates a new exception for a user not found by email.
     *
     * @param email The email that was not found
     * @return A new exception instance
     */
    public static DeviceNotFoundException byEmail(String email) {
        return new DeviceNotFoundException("User not found with email: " + email);
    }

    /**
     * Creates a new exception for a user not found by identifier (username or email).
     *
     * @param identifier The identifier that was not found
     * @return A new exception instance
     */
    public static DeviceNotFoundException byIdentifier(String identifier) {
        return new DeviceNotFoundException("User not found with identifier: " + identifier);
    }
}
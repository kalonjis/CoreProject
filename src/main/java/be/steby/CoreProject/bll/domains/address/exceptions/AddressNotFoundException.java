package be.steby.CoreProject.bll.domains.address.exceptions;

/**
 * Exception thrown when an address or address link cannot be found.
 *
 * <p>This exception is thrown in scenarios such as:</p>
 * <ul>
 *   <li>Looking up an address by public ID that doesn't exist</li>
 *   <li>Looking up a user address link that doesn't exist</li>
 *   <li>Attempting to access an address that has been deleted</li>
 * </ul>
 *
 * <p>HTTP Status: 404 (Not Found)</p>
 *
 * @see AddressDomainException
 */
public class AddressNotFoundException extends AddressDomainException {

    private static final int STATUS = 404;

    /**
     * Creates a new exception with a custom message.
     *
     * @param message the error message
     */
    public AddressNotFoundException(String message) {
        super(message, STATUS);
    }

    /**
     * Creates a new exception for an address not found by public ID.
     *
     * @param publicId the public ID that was not found
     * @return a new AddressNotFoundException
     */
    public static AddressNotFoundException forPublicId(String publicId) {
        return new AddressNotFoundException("Address not found with publicId: " + publicId);
    }

    /**
     * Creates a new exception for a user address link not found.
     *
     * @param publicId the user address public ID that was not found
     * @return a new AddressNotFoundException
     */
    public static AddressNotFoundException forUserAddressPublicId(String publicId) {
        return new AddressNotFoundException("User address link not found with publicId: " + publicId);
    }

    /**
     * Creates a new exception for a user address link not found or not owned by user.
     *
     * @param publicId the user address public ID
     * @param username the username of the expected owner
     * @return a new AddressNotFoundException
     */
    public static AddressNotFoundException forUserAddressNotOwned(String publicId, String username) {
        return new AddressNotFoundException(
                "User address link not found or not owned by user: " + publicId + " (user: " + username + ")");
    }

    /**
     * Creates a new exception for no default address found.
     *
     * @param addressType the type of address that was expected
     * @param username    the username
     * @return a new AddressNotFoundException
     */
    public static AddressNotFoundException noDefaultAddress(String addressType, String username) {
        return new AddressNotFoundException(
                "No default " + addressType + " address found for user: " + username);
    }

    /**
     * Creates a new exception for no primary address found.
     *
     * @param username the username
     * @return a new AddressNotFoundException
     */
    public static AddressNotFoundException noPrimaryAddress(String username) {
        return new AddressNotFoundException("No primary address found for user: " + username);
    }
}
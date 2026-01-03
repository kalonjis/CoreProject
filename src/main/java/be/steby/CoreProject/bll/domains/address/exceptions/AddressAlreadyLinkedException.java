package be.steby.CoreProject.bll.domains.address.exceptions;

import be.steby.CoreProject.dl.enums.AddressType;

/**
 * Exception thrown when attempting to create a duplicate address link.
 *
 * <p>This exception is thrown in scenarios such as:</p>
 * <ul>
 *   <li>Linking an address to a user when the same link already exists</li>
 *   <li>Creating a duplicate address link with the same type</li>
 * </ul>
 *
 * <p>HTTP Status: 409 (Conflict)</p>
 *
 * @see AddressDomainException
 */
public class AddressAlreadyLinkedException extends AddressDomainException {

    private static final int STATUS = 409;

    /**
     * Creates a new exception with a custom message.
     *
     * @param message the error message
     */
    public AddressAlreadyLinkedException(String message) {
        super(message, STATUS);
    }

    /**
     * Creates a new exception for a duplicate user-address link.
     *
     * @param username        the username
     * @param addressPublicId the address public ID
     * @param addressType     the address type
     * @return a new AddressAlreadyLinkedException
     */
    public static AddressAlreadyLinkedException forUserAndType(
            String username, String addressPublicId, AddressType addressType) {
        return new AddressAlreadyLinkedException(
                "User " + username + " already has address " + addressPublicId +
                        " linked with type " + addressType);
    }

    /**
     * Creates a new exception for a duplicate user-address link (any type).
     *
     * @param username        the username
     * @param addressPublicId the address public ID
     * @return a new AddressAlreadyLinkedException
     */
    public static AddressAlreadyLinkedException forUser(String username, String addressPublicId) {
        return new AddressAlreadyLinkedException(
                "User " + username + " already has address " + addressPublicId + " linked");
    }

    /**
     * Creates a new exception when user already has maximum addresses.
     *
     * @param username the username
     * @param maxCount the maximum allowed count
     * @return a new AddressAlreadyLinkedException
     */
    public static AddressAlreadyLinkedException maxAddressesReached(String username, int maxCount) {
        return new AddressAlreadyLinkedException(
                "User " + username + " has reached the maximum number of addresses (" + maxCount + ")");
    }

    /**
     * Creates a new exception when user already has maximum addresses of a type.
     *
     * @param username    the username
     * @param addressType the address type
     * @param maxCount    the maximum allowed count
     * @return a new AddressAlreadyLinkedException
     */
    public static AddressAlreadyLinkedException maxTypeAddressesReached(
            String username, AddressType addressType, int maxCount) {
        return new AddressAlreadyLinkedException(
                "User " + username + " has reached the maximum number of " +
                        addressType + " addresses (" + maxCount + ")");
    }
}
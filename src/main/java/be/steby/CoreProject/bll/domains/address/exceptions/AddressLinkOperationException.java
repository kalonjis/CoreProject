package be.steby.CoreProject.bll.domains.address.exceptions;

import be.steby.CoreProject.dl.enums.AddressType;

/**
 * Exception thrown when an address link operation fails.
 *
 * <p>This exception is thrown in scenarios such as:</p>
 * <ul>
 *   <li>Failed to set default address</li>
 *   <li>Failed to change address type</li>
 *   <li>Address link is inactive or expired</li>
 *   <li>Address not eligible for the requested operation</li>
 * </ul>
 *
 * <p>HTTP Status: 400 (Bad Request) by default</p>
 *
 * @see AddressDomainException
 */
public class AddressLinkOperationException extends AddressDomainException {

    private static final int STATUS = 400;

    /**
     * Creates a new exception with a custom message.
     *
     * @param message the error message
     */
    public AddressLinkOperationException(String message) {
        super(message, STATUS);
    }

    /**
     * Creates a new exception with a custom message and status.
     *
     * @param message the error message
     * @param status  the HTTP status code
     */
    public AddressLinkOperationException(String message, int status) {
        super(message, status);
    }

    /**
     * Creates a new exception for inactive address link.
     *
     * @param publicId the user address public ID
     * @return a new AddressLinkOperationException
     */
    public static AddressLinkOperationException inactive(String publicId) {
        return new AddressLinkOperationException(
                "Address link is inactive: " + publicId);
    }

    /**
     * Creates a new exception for expired address link.
     *
     * @param publicId the user address public ID
     * @return a new AddressLinkOperationException
     */
    public static AddressLinkOperationException expired(String publicId) {
        return new AddressLinkOperationException(
                "Address link has expired: " + publicId);
    }

    /**
     * Creates a new exception for future address link (not yet valid).
     *
     * @param publicId the user address public ID
     * @return a new AddressLinkOperationException
     */
    public static AddressLinkOperationException notYetValid(String publicId) {
        return new AddressLinkOperationException(
                "Address link is not yet valid: " + publicId);
    }

    /**
     * Creates a new exception for address not eligible for billing.
     *
     * @param publicId the user address public ID
     * @return a new AddressLinkOperationException
     */
    public static AddressLinkOperationException notBillingEligible(String publicId) {
        return new AddressLinkOperationException(
                "Address is not eligible for billing: " + publicId);
    }

    /**
     * Creates a new exception for address not eligible for shipping.
     *
     * @param publicId the user address public ID
     * @return a new AddressLinkOperationException
     */
    public static AddressLinkOperationException notShippingEligible(String publicId) {
        return new AddressLinkOperationException(
                "Address is not eligible for shipping: " + publicId);
    }

    /**
     * Creates a new exception for address not complete.
     *
     * @param publicId the user address public ID
     * @return a new AddressLinkOperationException
     */
    public static AddressLinkOperationException addressIncomplete(String publicId) {
        return new AddressLinkOperationException(
                "Address is incomplete and cannot be used for this operation: " + publicId);
    }

    /**
     * Creates a new exception when cannot remove the only address.
     *
     * @param username the username
     * @return a new AddressLinkOperationException
     */
    public static AddressLinkOperationException cannotRemoveOnlyAddress(String username) {
        return new AddressLinkOperationException(
                "Cannot remove the only address for user: " + username);
    }

    /**
     * Creates a new exception when cannot remove the default address.
     *
     * @param addressType the address type
     * @return a new AddressLinkOperationException
     */
    public static AddressLinkOperationException cannotRemoveDefaultAddress(AddressType addressType) {
        return new AddressLinkOperationException(
                "Cannot remove the default " + addressType + " address. Set another address as default first.");
    }

    /**
     * Creates a new exception when cannot remove the primary address.
     *
     * @return a new AddressLinkOperationException
     */
    public static AddressLinkOperationException cannotRemovePrimaryAddress() {
        return new AddressLinkOperationException(
                "Cannot remove the primary address. Set another address as primary first.");
    }

    /**
     * Creates a new exception for invalid type change.
     *
     * @param fromType the current type
     * @param toType   the target type
     * @return a new AddressLinkOperationException
     */
    public static AddressLinkOperationException invalidTypeChange(AddressType fromType, AddressType toType) {
        return new AddressLinkOperationException(
                "Cannot change address type from " + fromType + " to " + toType);
    }
}
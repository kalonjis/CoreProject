package be.steby.CoreProject.bll.domains.address.exceptions;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;

/**
 * Base exception for all address domain-related exceptions.
 *
 * <p>This abstract class extends {@link CoreProjectException} to integrate
 * with the global exception handling system via {@code ControllerAdvisor}.</p>
 *
 * <p>All specific address exceptions should extend this class to ensure
 * consistent error handling across the address domain.</p>
 *
 * @see CoreProjectException
 * @see AddressNotFoundException
 * @see AddressModificationNotAllowedException
 * @see AddressAlreadyLinkedException
 * @see AddressValidationException
 */
public abstract class AddressDomainException extends CoreProjectException {

    /**
     * Creates a new address domain exception with a message.
     * Default status is 400 (Bad Request).
     *
     * @param message the error message
     */
    public AddressDomainException(String message) {
        super(message, 400);
    }

    /**
     * Creates a new address domain exception with a message and status code.
     *
     * @param message the error message
     * @param status  the HTTP status code
     */
    public AddressDomainException(String message, int status) {
        super(message, status);
    }

    /**
     * Creates a new address domain exception with a message and cause.
     * Default status is 400 (Bad Request).
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public AddressDomainException(String message, Throwable cause) {
        super(message, 400, cause);
    }

    /**
     * Creates a new address domain exception with message, status, and cause.
     *
     * @param message the error message
     * @param status  the HTTP status code
     * @param cause   the underlying cause
     */
    public AddressDomainException(String message, int status, Throwable cause) {
        super(message, status, cause);
    }
}
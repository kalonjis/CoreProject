package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthDomainException;
import be.steby.CoreProject.bll.exceptions.CoreProjectException;

/**
 * Base exception for all two-factor authentication domain-related exceptions.
 *
 * <p>This abstract class extends AuthDomainException {@link AuthDomainException} to integrate
 * with the global exception handling system via {@code ControllerAdvisor}.
 *
 * <p>All specific two-factor authentication exceptions should extend this class
 * to ensure consistent error handling across the 2FA domain.
 *
 * <p><b>Exception Hierarchy:</b>
 * <pre>
 * CoreProjectException
 * └── TwoFactorDomainException
 *     ├── InvalidTwoFactorTokenException
 *     ├── TwoFactorNotEnabledException
 *     ├── TwoFactorCodeExpiredException
 *     └── ...
 * </pre>
 *
 * @author Steby Team
 * @since 2.1.0
 * @see CoreProjectException
 */
public abstract class TwoFactorDomainException extends AuthDomainException {

    /**
     * Creates a new 2FA domain exception with a message.
     * Default status is 401 (Unauthorized).
     *
     * @param message the error message
     */
    public TwoFactorDomainException(String message) {
        super(message, 401);
    }

    /**
     * Creates a new 2FA domain exception with a message and status code.
     *
     * @param message the error message
     * @param status  the HTTP status code
     */
    public TwoFactorDomainException(String message, int status) {
        super(message, status);
    }

    /**
     * Creates a new 2FA domain exception with a message and cause.
     * Default status is 401 (Unauthorized).
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public TwoFactorDomainException(String message, Throwable cause) {
        super(message, 401, cause);
    }

    /**
     * Creates a new 2FA domain exception with message, status, and cause.
     *
     * @param message the error message
     * @param status  the HTTP status code
     * @param cause   the underlying cause
     */
    public TwoFactorDomainException(String message, int status, Throwable cause) {
        super(message, status, cause);
    }
}
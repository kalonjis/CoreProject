package be.steby.CoreProject.bll.domains.auth.exceptions;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;

/**
 * Base exception for all authentication domain-related exceptions.
 *
 * <p>This abstract class extends {@link CoreProjectException} to integrate
 * with the global exception handling system via {@code ControllerAdvisor}.
 *
 * <p>All specific authentication exceptions should extend this class to ensure
 * consistent error handling across the auth domain.
 *
 * <p><b>Exception Hierarchy:</b>
 * <pre>
 * CoreProjectException
 * └── AuthDomainException
 *     ├── InvalidAccessTokenException
 *     ├── InvalidTwoFactorTokenException
 *     ├── InvalidCredentialsException
 *     └── ...
 * </pre>
 *
 * @author Steby Team
 * @since 2.1.0
 * @see CoreProjectException
 * @see InvalidAccessTokenException
 */
public abstract class AuthDomainException extends CoreProjectException {

    /**
     * Creates a new auth domain exception with a message.
     * Default status is 401 (Unauthorized).
     *
     * @param message the error message
     */
    public AuthDomainException(String message) {
        super(message, 401);
    }

    /**
     * Creates a new auth domain exception with a message and status code.
     *
     * @param message the error message
     * @param status  the HTTP status code
     */
    public AuthDomainException(String message, int status) {
        super(message, status);
    }

    /**
     * Creates a new auth domain exception with a message and cause.
     * Default status is 401 (Unauthorized).
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public AuthDomainException(String message, Throwable cause) {
        super(message, 401, cause);
    }

    /**
     * Creates a new auth domain exception with message, status, and cause.
     *
     * @param message the error message
     * @param status  the HTTP status code
     * @param cause   the underlying cause
     */
    public AuthDomainException(String message, int status, Throwable cause) {
        super(message, status, cause);
    }
}
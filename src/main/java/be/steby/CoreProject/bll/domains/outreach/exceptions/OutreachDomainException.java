package be.steby.CoreProject.bll.domains.outreach.exceptions;

import be.steby.CoreProject.bll.common.exceptions.CoreProjectException;

/**
 * Base exception for all outreach domain-related failures.
 *
 * <p>Extends {@link CoreProjectException} so that every outreach exception is
 * automatically handled by the global {@code ControllerAdvisor} and serialised
 * to a consistent JSON error response.</p>
 *
 * <h3>Exception hierarchy</h3>
 * <pre>
 * CoreProjectException
 * └── OutreachDomainException
 *     ├── OutreachContactNotFoundException
 *     └── OutreachContactNoEmailException
 * </pre>
 *
 * @see CoreProjectException
 * @see OutreachContactNotFoundException
 * @see OutreachContactNoEmailException
 */
public abstract class OutreachDomainException extends CoreProjectException {

    private static final String ERROR_CODE = "OUTREACH_DOMAIN";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    /**
     * Creates a new outreach domain exception with a message.
     * Default HTTP status is 400 (Bad Request).
     *
     * @param message the error message
     */
    public OutreachDomainException(String message) {
        super(message, 400);
    }

    /**
     * Creates a new outreach domain exception with a message and HTTP status.
     *
     * @param message the error message
     * @param status  the HTTP status code
     */
    public OutreachDomainException(String message, int status) {
        super(message, status);
    }

    /**
     * Creates a new outreach domain exception with a message and underlying cause.
     * Default HTTP status is 400 (Bad Request).
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public OutreachDomainException(String message, Throwable cause) {
        super(message, 400, cause);
    }

    /**
     * Creates a new outreach domain exception with a message, HTTP status, and cause.
     *
     * @param message the error message
     * @param status  the HTTP status code
     * @param cause   the underlying cause
     */
    public OutreachDomainException(String message, int status, Throwable cause) {
        super(message, status, cause);
    }
}

package be.steby.CoreProject.bll.common.exceptions;

import lombok.Getter;

@Getter
public class CoreProjectException extends RuntimeException {

    /**
     * The {@code detailed message} associated with the {@link Exception}.
     */
    private final String message;

    /**
     * The {@code HTTP status code} associated with the {@link Exception}.
     */
    private final int status;

    /**
     * A machine-readable {@code error code} identifying the type of error.
     * Examples: {@code "INVALID_TOKEN"}, {@code "USER_NOT_FOUND"}, {@code "FORBIDDEN"}.
     * <p>
     * Useful to distinguish between exceptions that share the same HTTP status code.
     */
    private final String errorCode;

    // -------------------------------------------------------------------------
    // Constructors without cause
    // -------------------------------------------------------------------------

    public CoreProjectException(String message) {
        super(message);
        this.message   = message;
        this.status    = 500;
        this.errorCode = "INTERNAL_ERROR";
    }

    public CoreProjectException(String message, int status) {
        super(message);
        this.message   = message;
        this.status    = status;
        this.errorCode = "INTERNAL_ERROR";
    }

    public CoreProjectException(String message, int status, String errorCode) {
        super(message);
        this.message   = message;
        this.status    = status;
        this.errorCode = errorCode;
    }

    // -------------------------------------------------------------------------
    // Constructors with cause
    // -------------------------------------------------------------------------

    public CoreProjectException(String message, Throwable cause) {
        super(message, cause);
        this.message   = message;
        this.status    = 500;
        this.errorCode = "INTERNAL_ERROR";
    }

    public CoreProjectException(String message, int status, Throwable cause) {
        super(message, cause);
        this.message   = message;
        this.status    = status;
        this.errorCode = "INTERNAL_ERROR";
    }

    public CoreProjectException(String message, int status, String errorCode, Throwable cause) {
        super(message, cause);
        this.message   = message;
        this.status    = status;
        this.errorCode = errorCode;
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    /**
     * Returns a string representation of this exception.
     * Includes class name, method, file, line number, status, error code, and message.
     *
     * <p>Reads the error code via {@link #getErrorCode()} (not the field) so that
     * subclasses overriding it log their own code instead of {@code INTERNAL_ERROR}.
     */
    @Override
    public String toString() {
        StackTraceElement el = this.getStackTrace()[0];
        return String.format(
                "%s thrown in %s() at %s:%d | status=%d | code=%s | message: %s",
                this.getClass().getSimpleName(),
                el.getMethodName(),
                el.getFileName(),
                el.getLineNumber(),
                this.getStatus(),
                this.getErrorCode(),
                this.getMessage()
        );
    }
}
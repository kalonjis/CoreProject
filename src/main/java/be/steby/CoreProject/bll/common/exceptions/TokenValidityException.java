package be.steby.CoreProject.bll.common.exceptions;

public class TokenValidityException extends CoreProjectException {
    
    private static final String ERROR_CODE = "TOKEN_VALIDITY";

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
    // ────────────────────────────────────────────────────────────────
/**
     * Constructs a new {@code AuthenticationException} with the specified {@code detail message}.
     * The {@code status code} is set to {@code 400 (Unauthorized)} by default.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     */
    public TokenValidityException(String message) {
        super(message, 400);
    }

    /**
     * Constructs a new {@code AuthenticationException} with the specified {@code detail message} and {@code status code}.
     *
     * @param message the {@code detail message} (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param status  the {@code HTTP status code} (which is saved for later retrieval
     *                by the {@link #getStatus()} method).
     */
    public TokenValidityException(String message, int status) {
        super(message, status);
    }
}

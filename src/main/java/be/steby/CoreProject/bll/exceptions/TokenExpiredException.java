package be.steby.CoreProject.bll.exceptions;

public class TokenExpiredException extends CoreProjectException {
    /**
     * Constructs a new TokenExpiredException with the specified detail message.
     * The status code is set to 498 by default.
     */
    public TokenExpiredException(String message) {
        super(message, 498);
    }

    /**
     * Constructs a new TokenExpiredException with the specified detail message and status code.
     */
    public TokenExpiredException(String message, int status) {
        super(message, status);
    }
}
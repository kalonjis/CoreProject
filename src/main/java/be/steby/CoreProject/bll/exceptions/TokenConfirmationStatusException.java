package be.steby.CoreProject.bll.exceptions;

import be.steby.CoreProject.dl.entities.tokens.BaseToken;
import lombok.Getter;

@Getter
public class TokenConfirmationStatusException extends CoreProjectException {
    private final BaseToken token;

    /**
     * Constructs a new {@code TokenExpiredException} with the specified {@code detail message}
     * and the token that caused the exception.
     *
     * @param message the detail message
     * @param token the token that caused the exception
     */
    public TokenConfirmationStatusException(String message, BaseToken token) {
        super(message, 400);
        this.token = token;
    }

    /**
     * Constructs a new {@code TokenExpiredException} with the specified {@code detail message},
     * {@code status code}, and the token that caused the exception.
     *
     * @param message the detail message
     * @param status the HTTP status code
     * @param token the token that caused the exception
     */
    public TokenConfirmationStatusException(String message, int status, BaseToken token) {
        super(message, status);
        this.token = token;
    }


    /**
     * Constructs a new {@code TokenExpiredException} with the specified {@code detail message},
     * {@code status code}, and the token that caused the exception.
     *
     * @param message the detail message
     */
    public TokenConfirmationStatusException(String message) {
        super(message, 400);
        this.token = null;
    }


    /**
     * Constructs a new {@code TokenExpiredException} with the specified {@code detail message},
     * {@code status code}, and the token that caused the exception.
     *
     * @param message the detail message
     * @param status the HTTP status code
     */
    public TokenConfirmationStatusException(String message, int status) {
        super(message, status);
        this.token = null;
    }
}
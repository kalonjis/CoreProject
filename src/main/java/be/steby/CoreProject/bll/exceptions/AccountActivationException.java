package be.steby.CoreProject.bll.exceptions;

import lombok.Getter;

/**
 * Exception thrown specifically for non-activated accounts.
 */
@Getter
public class AccountActivationException extends CoreProjectException {
    /**
     * Constructs a new {@code AccountActivationException} for non-activated accounts
     * @param message the error message
     * The status code is set to 498 by default.
     */
    public AccountActivationException(String message, String username) {
        super(message, 403); // 403 Forbidden
    }


    /**
     * Constructs a new AccountActivationException with the specified detail message and status code.
     */
    public AccountActivationException(String message, int status) {
        super(message, status);
    }
}
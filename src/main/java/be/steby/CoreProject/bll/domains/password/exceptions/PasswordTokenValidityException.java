package be.steby.CoreProject.bll.domains.password.exceptions;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;

public class PasswordTokenValidityException extends PasswordDomainException {
    /**
     * Constructs a new {@code AuthenticationException} with the specified {@code detail message}.
     * The {@code status code} is set to {@code 400 (Unauthorized)} by default.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     */
    public PasswordTokenValidityException(String message) {
        super(message);
    }
}

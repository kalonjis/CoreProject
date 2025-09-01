package be.steby.CoreProject.bll.domains.auth.exceptions;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;

/**
 * Base exception for all authentication-related errors.
 * This serves as the parent class for all auth domain exceptions.
 */
public class AuthenticationException extends CoreProjectException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, int status) {
        super(message, status);
    }
}
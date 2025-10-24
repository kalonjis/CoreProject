package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthenticationException;

/**
 * Exception thrown when attempting to enable WebAuthn 2FA 
 * but it's already enabled for the user.
 *
 */
public class WebAuthnAlreadyEnabledException extends AuthenticationException {

    public WebAuthnAlreadyEnabledException(String message) {
        super(message);
    }

    public WebAuthnAlreadyEnabledException(String message, int status) {
        super(message, status);
    }

    public WebAuthnAlreadyEnabledException(String message, Throwable cause) {
        super(message, cause);
    }
}
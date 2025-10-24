package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthenticationException;

/**
 * Exception thrown when attempting to use WebAuthn 2FA
 * but it's not enabled for the user.
 *
 */
public class WebAuthnNotEnabledException extends AuthenticationException {

    public WebAuthnNotEnabledException(String message) {
        super(message);
    }

    public WebAuthnNotEnabledException(String message, int status) {
        super(message, status);
    }

    public WebAuthnNotEnabledException(String message, Throwable cause) {
        super(message, cause);
    }
}


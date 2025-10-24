package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthenticationException;

/**
 * Exception thrown when WebAuthn assertion verification fails.
 * 
 * This can occur during assertion validation, credential lookup,
 * or integration with Spring Security WebAuthn.
 */
public class WebAuthnAuthenticationException extends AuthenticationException {

    public WebAuthnAuthenticationException(String message) {
        super(message);
    }

    public WebAuthnAuthenticationException(String message, int status) {
        super(message, status);
    }

    public WebAuthnAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
// File: WebAuthnRegistrationException.java
package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthenticationException;

/**
 * Exception thrown when WebAuthn credential registration fails.
 * 
 * This can occur during credential validation, storage, or
 * integration with Spring Security WebAuthn.
 */
public class WebAuthnRegistrationException extends AuthenticationException {

    public WebAuthnRegistrationException(String message) {
        super(message);
    }

    public WebAuthnRegistrationException(String message, int status) {
        super(message, status);
    }

    public WebAuthnRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}

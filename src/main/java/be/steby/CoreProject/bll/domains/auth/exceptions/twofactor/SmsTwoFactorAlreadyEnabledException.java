package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthenticationException;

/**
 * Exception thrown when attempting to enable SMS 2FA for a user
 * who already has SMS 2FA enabled.
 *
 * HTTP Status: 409 Conflict - The request conflicts with the current state
 * of the resource (user already has SMS 2FA enabled).
 *
 * @author Steby Team
 * @since 2.0.0
 */
public class SmsTwoFactorAlreadyEnabledException extends AuthenticationException {

    public SmsTwoFactorAlreadyEnabledException(String message) {
        super(message, 409);
    }

    public SmsTwoFactorAlreadyEnabledException() {
        super("SMS two-factor authentication is already enabled for this user", 409);
    }

    public SmsTwoFactorAlreadyEnabledException(String message, Throwable cause) {
        super(message, 409);
        initCause(cause);
    }
}

package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthenticationException;

/**
 * Exception thrown when attempting to enable TOTP-based 2FA for a user
 * who already has TOTP 2FA enabled.
 *
 * This prevents duplicate TOTP 2FA configurations and ensures data consistency.
 * The client should check the current 2FA status before attempting to enable
 * additional methods.
 *
 * HTTP Status: 409 Conflict - The request conflicts with the current state
 * of the resource (user already has TOTP 2FA enabled).
 *
 * @author Steby Team
 * @since 2.0.0
 */
public class TOTPTwoFactorAlreadyEnabledException extends AuthenticationException {

    /**
     * Creates a new exception with a descriptive message.
     * Uses HTTP 409 Conflict status to indicate the resource conflict.
     *
     * @param message detailed error message explaining the conflict
     */
    public TOTPTwoFactorAlreadyEnabledException(String message) {
        super(message, 409);
    }

    /**
     * Creates a new exception with a default message.
     * Provides a standard user-friendly error message.
     */
    public TOTPTwoFactorAlreadyEnabledException() {
        super("TOTP two-factor authentication is already enabled for this user", 409);
    }

    /**
     * Creates a new exception with a custom message and cause.
     * Useful when wrapping lower-level exceptions.
     *
     * @param message detailed error message
     * @param cause the underlying cause of this exception
     */
    public TOTPTwoFactorAlreadyEnabledException(String message, Throwable cause) {
        super(message, 409);
        initCause(cause);
    }
}
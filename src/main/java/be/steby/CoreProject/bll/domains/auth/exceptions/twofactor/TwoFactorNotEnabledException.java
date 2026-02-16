package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthDomainException;

/**
 * Exception thrown when attempting to perform 2FA operations on a user
 * who has no enabled two-factor authentication methods.
 * 
 * This exception is thrown by the TwoFactorFactory when:
 * - Trying to generate verification codes for users without 2FA
 * - Attempting to verify codes for users without 2FA
 * - Any operation requiring an active 2FA method when none exists
 * 
 * This is different from method-specific exceptions (like EmailTwoFactorNotEnabledException)
 * as it indicates the complete absence of any 2FA method rather than a specific method.
 * 
 * HTTP Status: 404 Not Found - No 2FA configuration exists for this user.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public class TwoFactorNotEnabledException extends TwoFactorDomainException {
    
    /**
     * Creates a new exception with a descriptive message.
     * Uses HTTP 404 Not Found status to indicate missing 2FA configuration.
     * 
     * @param message detailed error message explaining what was not found
     */
    public TwoFactorNotEnabledException(String message) {
        super(message, 404);
    }
    
    /**
     * Creates a new exception with a default message.
     * Provides a standard user-friendly error message.
     */
    public TwoFactorNotEnabledException() {
        super("No two-factor authentication method is enabled for this user", 404);
    }
    
    /**
     * Creates a new exception with a custom message and cause.
     * Useful when wrapping lower-level exceptions or database errors.
     * 
     * @param message detailed error message
     * @param cause the underlying cause of this exception
     */
    public TwoFactorNotEnabledException(String message, Throwable cause) {
        super(message, 404);
        initCause(cause);
    }
}
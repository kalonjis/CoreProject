package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthenticationException;

/**
 * Exception thrown when attempting to perform operations on email-based 2FA
 * for a user who does not have email 2FA enabled.
 * 
 * This can occur when:
 * - Trying to disable email 2FA that isn't enabled
 * - Attempting to generate verification codes for non-enabled email 2FA
 * - Trying to verify codes when email 2FA is not configured
 * 
 * HTTP Status: 404 Not Found - The requested 2FA configuration does not exist
 * for this user.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public class EmailTwoFactorNotEnabledException extends AuthenticationException {
    
    /**
     * Creates a new exception with a descriptive message.
     * Uses HTTP 404 Not Found status to indicate the missing configuration.
     * 
     * @param message detailed error message explaining what was not found
     */
    public EmailTwoFactorNotEnabledException(String message) {
        super(message, 404);
    }
    
    /**
     * Creates a new exception with a default message.
     * Provides a standard user-friendly error message.
     */
    public EmailTwoFactorNotEnabledException() {
        super("Email two-factor authentication is not enabled for this user", 404);
    }
    
    /**
     * Creates a new exception with a custom message and cause.
     * Useful when wrapping lower-level exceptions or database errors.
     * 
     * @param message detailed error message
     * @param cause the underlying cause of this exception
     */
    public EmailTwoFactorNotEnabledException(String message, Throwable cause) {
        super(message, 404);
        initCause(cause);
    }
}
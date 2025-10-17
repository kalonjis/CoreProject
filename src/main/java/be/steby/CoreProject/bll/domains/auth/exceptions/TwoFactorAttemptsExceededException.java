package be.steby.CoreProject.bll.domains.auth.exceptions;

import org.springframework.security.core.AuthenticationException;

/**
 * Exception thrown when 2FA verification has too many failed attempts.
 * Used for rate limiting protection.
 */
public class TwoFactorAttemptsExceededException extends AuthenticationException {
    
    public TwoFactorAttemptsExceededException(String message) {
        super(message);
    }
    
    public TwoFactorAttemptsExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
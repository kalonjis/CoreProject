package be.steby.CoreProject.bll.domains.auth.exceptions;

import org.springframework.security.core.AuthenticationException;

/**
 * Exception thrown when 2FA token is invalid or expired.
 * Used when JWT 2FA token validation fails.
 */
public class InvalidTwoFactorTokenException extends AuthenticationException {
    
    public InvalidTwoFactorTokenException(String message) {
        super(message);
    }
    
    public InvalidTwoFactorTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
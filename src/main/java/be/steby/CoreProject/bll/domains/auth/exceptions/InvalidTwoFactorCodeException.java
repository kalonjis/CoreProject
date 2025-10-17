package be.steby.CoreProject.bll.domains.auth.exceptions;

import org.springframework.security.core.AuthenticationException;

/**
 * Exception thrown when 2FA verification code is invalid.
 * Used when user provides wrong 6-digit code.
 */
public class InvalidTwoFactorCodeException extends AuthenticationException {
    
    public InvalidTwoFactorCodeException(String message) {
        super(message);
    }
    
    public InvalidTwoFactorCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
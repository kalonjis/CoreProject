package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthenticationException;

/**
 * Exception thrown when a provided backup code is invalid or does not match
 * any of the user's backup codes.
 * 
 * This is used for both malformed codes and codes that don't exist in the
 * user's backup code set.
 * 
 * HTTP Status: 401 Unauthorized - The provided backup code is not valid
 * for authentication.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public class BackupCodeInvalidException extends AuthenticationException {
    
    public BackupCodeInvalidException(String message) {
        super(message, 401);
    }
    
    public BackupCodeInvalidException() {
        super("Invalid backup code", 401);
    }
    
    public BackupCodeInvalidException(String message, Throwable cause) {
        super(message, 401);
        initCause(cause);
    }
}

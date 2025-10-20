package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthenticationException;

/**
 * Exception thrown when attempting to perform operations on backup codes
 * for a user who does not have backup codes enabled.
 * 
 * This can occur when:
 * - Trying to verify a backup code when none are configured
 * - Attempting to regenerate backup codes that don't exist
 * - Trying to get backup code status for non-configured codes
 * 
 * HTTP Status: 404 Not Found - The requested backup codes configuration 
 * does not exist for this user.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public class BackupCodeNotEnabledException extends AuthenticationException {
    
    public BackupCodeNotEnabledException(String message) {
        super(message, 404);
    }
    
    public BackupCodeNotEnabledException() {
        super("Backup codes are not enabled for this user", 404);
    }
    
    public BackupCodeNotEnabledException(String message, Throwable cause) {
        super(message, 404);
        initCause(cause);
    }
}
package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthenticationException;

/**
 * Exception thrown when a user has exhausted all their backup codes.
 * 
 * This occurs when attempting to use backup codes but the user has
 * already consumed all available backup codes.
 * 
 * HTTP Status: 423 Locked - The backup codes resource is locked because
 * all codes have been consumed.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public class BackupCodeExhaustedException extends AuthenticationException {
    
    public BackupCodeExhaustedException(String message) {
        super(message, 423);
    }
    
    public BackupCodeExhaustedException() {
        super("All backup codes have been used. Please regenerate new backup codes.", 423);
    }
    
    public BackupCodeExhaustedException(String message, Throwable cause) {
        super(message, 423);
        initCause(cause);
    }
}
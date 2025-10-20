package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthenticationException;

/**
 * Exception thrown when attempting to use a backup code that has already
 * been consumed or when all backup codes have been used.
 * 
 * This ensures that backup codes can only be used once for security.
 * 
 * HTTP Status: 410 Gone - The backup code was valid but has been consumed
 * and is no longer available for use.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public class BackupCodeAlreadyUsedException extends AuthenticationException {
    
    public BackupCodeAlreadyUsedException(String message) {
        super(message, 410);
    }
    
    public BackupCodeAlreadyUsedException() {
        super("This backup code has already been used", 410);
    }
    
    public BackupCodeAlreadyUsedException(String message, Throwable cause) {
        super(message, 410);
        initCause(cause);
    }
}

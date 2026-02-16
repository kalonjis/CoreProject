package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthDomainException;

/**
 * Exception thrown when attempting to perform operations on backup codes 2FA
 * for a user who does not have backup codes 2FA enabled.
 * 
 * HTTP Status: 404 Not Found - The requested 2FA configuration does not exist
 * for this user.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public class BackupCodesTwoFactorNotEnabledException extends TwoFactorDomainException {
    
    public BackupCodesTwoFactorNotEnabledException(String message) {
        super(message, 404);
    }
    
    public BackupCodesTwoFactorNotEnabledException() {
        super("Backup codes two-factor authentication is not enabled for this user", 404);
    }
    
    public BackupCodesTwoFactorNotEnabledException(String message, Throwable cause) {
        super(message, 404);
        initCause(cause);
    }
}
package be.steby.CoreProject.bll.domains.auth.exceptions.twofactor;

import be.steby.CoreProject.bll.domains.auth.exceptions.AuthDomainException;

/**
 * Exception thrown when attempting to enable backup codes 2FA for a user
 * who already has backup codes 2FA enabled.
 *
 * HTTP Status: 409 Conflict - The request conflicts with the current state
 * of the resource (user already has backup codes 2FA enabled).
 *
 * @author Steby Team
 * @since 2.0.0
 */
public class BackupCodesTwoFactorAlreadyEnabledException extends TwoFactorDomainException {

    public BackupCodesTwoFactorAlreadyEnabledException(String message) {
        super(message, 409);
    }

    public BackupCodesTwoFactorAlreadyEnabledException() {
        super("Backup codes two-factor authentication is already enabled for this user", 409);
    }

    public BackupCodesTwoFactorAlreadyEnabledException(String message, Throwable cause) {
        super(message, 409);
        initCause(cause);
    }
}
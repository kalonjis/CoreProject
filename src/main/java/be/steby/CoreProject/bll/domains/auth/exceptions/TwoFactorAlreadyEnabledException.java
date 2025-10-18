package be.steby.CoreProject.bll.domains.auth.exceptions;

/**
 * Exception thrown when trying to enable a 2FA method that is already enabled.
 * Replaces generic IllegalStateException with domain-specific exception.
 */
public class TwoFactorAlreadyEnabledException extends AuthenticationException {

    public TwoFactorAlreadyEnabledException(String message) {
        super(message, 409); // 409 Conflict
    }
    
    public static TwoFactorAlreadyEnabledException forType(String type) {
        return new TwoFactorAlreadyEnabledException(
            type + " two-factor authentication is already enabled for this user"
        );
    }
}
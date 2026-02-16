package be.steby.CoreProject.bll.domains.auth.exceptions;

/**
 * Exception thrown when a user account has been disabled by an administrator.
 * This should result in a 403 Forbidden response.
 */
public class AccountDisabledException extends AuthDomainException {

    public AccountDisabledException(String message) {
        super(message, 403);
    }
}
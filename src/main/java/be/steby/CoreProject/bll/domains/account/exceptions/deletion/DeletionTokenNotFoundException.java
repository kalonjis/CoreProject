package be.steby.CoreProject.bll.domains.account.exceptions.deletion;

import be.steby.CoreProject.bll.domains.account.exceptions.AccountDomainException;

/**
 * Thrown when a GDPR deletion confirmation token cannot be found.
 *
 * <p>Typically triggered when the raw token value extracted from the
 * email link does not match any stored {@link be.steby.CoreProject.dl.entities.tokens.AccountDeletionToken}.
 *
 * <p>HTTP status: 404 Not Found.
 */
public class DeletionTokenNotFoundException extends AccountDomainException {

    public DeletionTokenNotFoundException(String message) {
        super(message, 404);
    }
}
package be.steby.CoreProject.bll.domains.account.exceptions.deletion;

import be.steby.CoreProject.bll.domains.account.exceptions.AccountDomainException;

/**
 * Thrown when a GDPR deletion confirmation token has already been revoked.
 *
 * <p>A token is revoked after successful deletion confirmation, or when
 * the user explicitly cancels their deletion request.
 *
 * <p>HTTP status: 410 Gone — the token existed but is no longer valid.
 */
public class DeletionTokenRevokedException extends AccountDomainException {

    public DeletionTokenRevokedException(String message) {
        super(message, 410);
    }
}
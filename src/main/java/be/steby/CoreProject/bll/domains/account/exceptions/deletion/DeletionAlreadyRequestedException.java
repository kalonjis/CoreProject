package be.steby.CoreProject.bll.domains.account.exceptions.deletion;

import be.steby.CoreProject.bll.domains.account.exceptions.AccountDomainException;

/**
 * Thrown when a user submits a GDPR deletion request while one is already
 * pending confirmation.
 */
public class DeletionAlreadyRequestedException extends AccountDomainException {

    public DeletionAlreadyRequestedException(String message) {
        super(message, 409);
    }
}
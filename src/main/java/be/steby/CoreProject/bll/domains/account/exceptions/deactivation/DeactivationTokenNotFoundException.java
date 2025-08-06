package be.steby.CoreProject.bll.domains.account.exceptions.deactivation;

import be.steby.CoreProject.bll.domains.account.exceptions.AccountDomainException;

public class DeactivationTokenNotFoundException extends AccountDomainException {
    public DeactivationTokenNotFoundException(String message) {
        super(message, 404);
    }
}

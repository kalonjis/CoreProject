package be.steby.CoreProject.bll.domains.account.exceptions.deactivation;

import be.steby.CoreProject.bll.domains.account.exceptions.AccountDomainException;

public class AccountAlreadyDeactivatedException extends AccountDomainException {

    public AccountAlreadyDeactivatedException(String message) {
        super(message, 409);
    }
}

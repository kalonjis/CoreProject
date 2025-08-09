package be.steby.CoreProject.bll.domains.account.exceptions.reactivation;

import be.steby.CoreProject.bll.domains.account.exceptions.AccountDomainException;

public class ReactivationNotAllowedException extends AccountDomainException {

    public ReactivationNotAllowedException(String message) {
        super(message, 403);
    }

    public ReactivationNotAllowedException(String message, int status) {
        super(message, status);
    }
}
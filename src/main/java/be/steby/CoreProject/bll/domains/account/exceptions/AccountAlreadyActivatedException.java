package be.steby.CoreProject.bll.domains.account.exceptions;

public class AccountAlreadyActivatedException extends AccountDomainException {

    public AccountAlreadyActivatedException(String message) {
        super(message, 409);
    }
}

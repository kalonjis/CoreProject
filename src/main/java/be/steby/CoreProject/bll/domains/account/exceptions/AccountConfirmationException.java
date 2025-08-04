package be.steby.CoreProject.bll.domains.account.exceptions;

/**
 * Exception pour les problèmes de confirmation de compte
 */
public class AccountConfirmationException extends AccountDomainException {

    public AccountConfirmationException(String message) {
        super(message); // 400 par défaut
    }

    public AccountConfirmationException(String message, int status) {
        super(message, status);
    }
}
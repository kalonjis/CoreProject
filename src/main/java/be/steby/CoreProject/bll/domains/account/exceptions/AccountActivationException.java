package be.steby.CoreProject.bll.domains.account.exceptions;

/**
 * Exception pour les problèmes de confirmation de compte
 */
public class AccountActivationException extends AccountDomainException {

    public AccountActivationException(String message) {
        super(message); // 400 par défaut
    }

    public AccountActivationException(String message, int status) {
        super(message, status);
    }
}
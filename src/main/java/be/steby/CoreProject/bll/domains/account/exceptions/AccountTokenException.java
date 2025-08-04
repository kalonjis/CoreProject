package be.steby.CoreProject.bll.domains.account.exceptions;

/**
 * Exception pour les problèmes de confirmation de compte
 */
public class AccountTokenException extends AccountDomainException {

    public AccountTokenException(String message) {
        super(message); // 400 par défaut
    }

    public AccountTokenException(String message, int status) {
        super(message, status);
    }
}
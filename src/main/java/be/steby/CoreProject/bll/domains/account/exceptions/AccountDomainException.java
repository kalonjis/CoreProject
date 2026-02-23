package be.steby.CoreProject.bll.domains.account.exceptions;

import be.steby.CoreProject.bll.common.exceptions.CoreProjectException;

public abstract class AccountDomainException extends CoreProjectException {

    /**
     * Crée une nouvelle exception avec un message et un code de statut par défaut 400 (Bad Request).
     *
     * @param message Message décrivant l'erreur
     */
    public AccountDomainException(String message) {
        super(message, 400);
    }

    /**
     * Crée une nouvelle exception avec un message et un code de statut spécifique.
     *
     * @param message Message décrivant l'erreur
     * @param status Code de statut HTTP à retourner dans la réponse
     */
    public AccountDomainException(String message, int status) {
        super(message, status);
    }


    /**
     * Creates a new exception with a message, cause, and default status 400.
     *
     * @param message Error message
     * @param cause Root cause
     */
    public AccountDomainException(String message, Throwable cause) {
        super(message, 400, cause);
    }

    /**
     * Creates a new exception with a message, cause, and specific status code.
     *
     * @param message Error message
     * @param status HTTP status code
     * @param cause Root cause
     */
    public AccountDomainException(String message, int status, Throwable cause) {
        super(message, status, cause);
    }
}
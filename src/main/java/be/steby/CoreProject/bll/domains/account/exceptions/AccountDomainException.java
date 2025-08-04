package be.steby.CoreProject.bll.domains.account.exceptions;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;

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
}
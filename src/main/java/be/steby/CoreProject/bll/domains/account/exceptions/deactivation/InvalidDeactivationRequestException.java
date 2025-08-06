package be.steby.CoreProject.bll.domains.account.exceptions.deactivation;

import be.steby.CoreProject.bll.domains.account.exceptions.AccountDomainException;

public class InvalidDeactivationRequestException extends AccountDomainException {

    /**
     * Crée une exception avec un message d'erreur et le code de statut par défaut (400).
     *
     * @param message Message décrivant pourquoi la demande de désactivation est invalide
     */
    public InvalidDeactivationRequestException(String message) {
        super(message); // 400 Bad Request par défaut
    }

    /**
     * Crée une exception avec un message d'erreur et un code de statut personnalisé.
     *
     * @param message Message décrivant pourquoi la demande de désactivation est invalide
     * @param status Code de statut HTTP spécifique (ex: 409 pour conflit)
     */
    public InvalidDeactivationRequestException(String message, int status) {
        super(message, status);
    }
}

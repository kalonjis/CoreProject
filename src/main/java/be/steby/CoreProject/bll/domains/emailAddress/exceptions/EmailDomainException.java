package be.steby.CoreProject.bll.domains.emailAddress.exceptions;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;

/**
 * Exception de base pour toutes les exceptions liées au domaine des emails.
 * Cette classe étend CoreProjectException pour s'intégrer avec le système global de gestion d'erreurs.
 */
public abstract class EmailDomainException extends CoreProjectException {

    /**
     * Crée une nouvelle exception avec un message et un code de statut par défaut 400 (Bad Request).
     *
     * @param message Message décrivant l'erreur
     */
    public EmailDomainException(String message) {
        super(message, 400);
    }

    /**
     * Crée une nouvelle exception avec un message et un code de statut spécifique.
     *
     * @param message Message décrivant l'erreur
     * @param status Code de statut HTTP
     */
    public EmailDomainException(String message, int status) {
        super(message, status);
    }
}
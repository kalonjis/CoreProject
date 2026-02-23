package be.steby.CoreProject.bll.domains.admin.exceptions;

import be.steby.CoreProject.bll.common.exceptions.CoreProjectException;

/**
 * Exception de base pour toutes les exceptions liées au domaine d'administration.
 * Cette classe étend CoreProjectException pour s'intégrer avec le système global de gestion d'erreurs.
 */
public abstract class AdminDomainException extends CoreProjectException {

    /**
     * Crée une nouvelle exception avec un message et un code de statut par défaut 400 (Bad Request).
     *
     * @param message Message décrivant l'erreur
     */
    public AdminDomainException(String message) {
        super(message, 400);
    }

    /**
     * Crée une nouvelle exception avec un message et un code de statut spécifique.
     *
     * @param message Message décrivant l'erreur
     * @param status Code de statut HTTP
     */
    public AdminDomainException(String message, int status) {
        super(message, status);
    }


    public AdminDomainException(String message, Throwable cause) {
        super(message, cause);
    }

}
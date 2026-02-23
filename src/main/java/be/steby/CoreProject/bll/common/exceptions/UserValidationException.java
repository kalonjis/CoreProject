package be.steby.CoreProject.bll.common.exceptions;

/**
 * Exception lancée lors d'échec de validation des données utilisateur
 */
public class UserValidationException extends CoreProjectException {

    /**
     * Constructeur avec message d'erreur
     *
     * @param message Le message d'erreur décrivant l'échec de validation
     */
    public UserValidationException(String message) {
        super(message, 400);
    }

    /**
     * Constructeur avec message et code de statut personnalisé
     *
     * @param message Le message d'erreur
     * @param status Le code de statut HTTP
     */
    public UserValidationException(String message, int status) {
        super(message, status);
    }
}
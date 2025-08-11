package be.steby.CoreProject.bll.domains.admin.exceptions;

/**
 * Exception lancée lorsqu'une opération d'administration échoue pour des raisons
 * techniques ou métier qui ne sont pas liées aux privilèges.
 */
public class AdminOperationException extends AdminDomainException {

    /**
     * Crée une nouvelle exception avec un message spécifique.
     *
     * @param message Message décrivant pourquoi l'opération a échoué
     */
    public AdminOperationException(String message) {
        super(message);
    }

    /**
     * Crée une nouvelle exception avec un message et un code de statut spécifique.
     *
     * @param message Message décrivant pourquoi l'opération a échoué
     * @param status Code de statut HTTP
     */
    public AdminOperationException(String message, int status) {
        super(message, status);
    }

    /**
     * Crée une exception avec une cause sous-jacente.
     *
     * @param message Message décrivant l'erreur
     * @param cause La cause de l'exception
     */
    public AdminOperationException(String message, Throwable cause) {
        super(message, 500); // Internal Server Error pour les erreurs techniques
        initCause(cause);
    }

    /**
     * Crée une exception pour une opération qui a échoué sur un utilisateur spécifique.
     *
     * @param operation L'opération qui a échoué
     * @param userId L'ID de l'utilisateur concerné
     * @param reason La raison de l'échec
     * @return Une nouvelle instance de l'exception
     */
    public static AdminOperationException forUser(String operation, Long userId, String reason) {
        return new AdminOperationException(
                String.format("Échec de l'opération '%s' pour l'utilisateur ID %d : %s", operation, userId, reason)
        );
    }
}
package be.steby.CoreProject.bll.domains.admin.exceptions;

/**
 * Exception lancée lorsqu'un utilisateur tente d'effectuer une opération d'administration
 * sans avoir les privilèges nécessaires.
 */
public class InsufficientAdminPrivilegesException extends AdminDomainException {

    /**
     * Crée une nouvelle exception avec un message spécifique.
     *
     * @param message Message décrivant pourquoi l'opération n'est pas autorisée
     */
    public InsufficientAdminPrivilegesException(String message) {
        super(message, 403); // Forbidden
    }

    /**
     * Crée une exception avec un message formaté pour une opération spécifique.
     *
     * @param operation L'opération tentée
     * @param requiredRole Le rôle requis pour cette opération
     * @return Une nouvelle instance de l'exception
     */
    public static InsufficientAdminPrivilegesException forOperation(String operation, String requiredRole) {
        return new InsufficientAdminPrivilegesException(
                String.format("Privilèges insuffisants pour %s. Rôle requis : %s", operation, requiredRole)
        );
    }

    /**
     * Crée une exception pour les opérations nécessitant des privilèges SUPER_ADMIN.
     *
     * @param operation L'opération tentée
     * @return Une nouvelle instance de l'exception
     */
    public static InsufficientAdminPrivilegesException forSuperAdminOperation(String operation) {
        return new InsufficientAdminPrivilegesException(
                String.format("Seuls les Super Administrateurs peuvent %s", operation)
        );
    }
}
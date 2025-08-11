package be.steby.CoreProject.bll.domains.admin.exceptions;

/**
 * Exception lancée lorsqu'un administrateur tente d'effectuer une opération
 * sur un Super Administrateur sans avoir lui-même les privilèges SUPER_ADMIN.
 */
public class SuperAdminManagementException extends AdminDomainException {

    /**
     * Crée une nouvelle exception avec un message spécifique.
     *
     * @param message Message décrivant pourquoi l'opération n'est pas autorisée
     */
    public SuperAdminManagementException(String message) {
        super(message, 403); // Forbidden
    }

    /**
     * Crée une exception pour une tentative de gestion d'un Super Administrateur.
     *
     * @param operation L'opération tentée
     * @return Une nouvelle instance de l'exception
     */
    public static SuperAdminManagementException forOperation(String operation) {
        return new SuperAdminManagementException(
                String.format("Seul un Super Administrateur peut %s un autre Super Administrateur", operation)
        );
    }

    /**
     * Crée une exception pour une tentative d'attribution du rôle SUPER_ADMIN.
     *
     * @return Une nouvelle instance de l'exception
     */
    public static SuperAdminManagementException forRoleGrant() {
        return new SuperAdminManagementException(
                "Seul un Super Administrateur peut attribuer le rôle SUPER_ADMIN"
        );
    }

    /**
     * Crée une exception pour une tentative de révocation du rôle SUPER_ADMIN.
     *
     * @return Une nouvelle instance de l'exception
     */
    public static SuperAdminManagementException forRoleRevoke() {
        return new SuperAdminManagementException(
                "Seul un Super Administrateur peut révoquer le rôle SUPER_ADMIN"
        );
    }

    /**
     * Crée une exception pour une tentative de création d'utilisateur avec le rôle SUPER_ADMIN.
     *
     * @return Une nouvelle instance de l'exception
     */
    public static SuperAdminManagementException forUserCreation() {
        return new SuperAdminManagementException(
                "Seul un Super Administrateur peut créer un utilisateur avec le rôle SUPER_ADMIN"
        );
    }
}
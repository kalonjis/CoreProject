package be.steby.CoreProject.bll.domains.admin.exceptions;

/**
 * Exception lancée lorsqu'un administrateur tente d'effectuer une opération
 * sur son propre compte qui n'est pas autorisée pour des raisons de sécurité.
 */
public class SelfManagementException extends AdminDomainException {

    /**
     * Crée une nouvelle exception avec un message spécifique.
     *
     * @param message Message décrivant pourquoi l'auto-gestion n'est pas autorisée
     */
    public SelfManagementException(String message) {
        super(message, 409); // Conflict
    }

    /**
     * Crée une exception pour une tentative d'activation de son propre compte.
     *
     * @return Une nouvelle instance de l'exception
     */
    public static SelfManagementException forSelfActivation() {
        return new SelfManagementException(
                "Vous ne pouvez pas activer votre propre compte"
        );
    }

    /**
     * Crée une exception pour une tentative de désactivation de son propre compte.
     *
     * @return Une nouvelle instance de l'exception
     */
    public static SelfManagementException forSelfDeactivation() {
        return new SelfManagementException(
                "Vous ne pouvez pas désactiver votre propre compte"
        );
    }

    /**
     * Crée une exception pour une tentative de suppression de son propre compte.
     *
     * @return Une nouvelle instance de l'exception
     */
    public static SelfManagementException forSelfDeletion() {
        return new SelfManagementException(
                "Vous ne pouvez pas supprimer votre propre compte"
        );
    }

    /**
     * Crée une exception pour une tentative de modification de ses propres rôles.
     *
     * @param operation L'opération tentée (grant/revoke)
     * @return Une nouvelle instance de l'exception
     */
    public static SelfManagementException forSelfRoleManagement(String operation) {
        return new SelfManagementException(
                String.format("Vous ne pouvez pas %s vos propres rôles", operation)
        );
    }

    /**
     * Crée une exception pour une opération générale d'auto-gestion non autorisée.
     *
     * @param operation L'opération tentée
     * @return Une nouvelle instance de l'exception
     */
    public static SelfManagementException forOperation(String operation) {
        return new SelfManagementException(
                String.format("Vous ne pouvez pas %s sur votre propre compte", operation)
        );
    }
}
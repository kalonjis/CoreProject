package be.steby.CoreProject.bll.domains.admin.exceptions;

/**
 * Exception levée lors des validations de désactivation admin.
 * Suit le pattern du projet : étend AdminDomainException.
 */
public class AdminDeactivationValidationException extends AdminDomainException {

    /**
     * Crée une nouvelle exception avec un message spécifique.
     * Utilise le code 422 (Unprocessable Entity) pour les erreurs de validation métier.
     *
     * @param message Message décrivant pourquoi la validation a échoué
     */
    public AdminDeactivationValidationException(String message) {
        super(message, 422); // Unprocessable Entity pour validation métier
    }

    /**
     * Crée une exception avec un code de statut personnalisé.
     *
     * @param message Message décrivant l'erreur
     * @param status Code de statut HTTP
     */
    public AdminDeactivationValidationException(String message, int status) {
        super(message, status);
    }

    /**
     * Crée une exception avec une cause sous-jacente.
     *
     * @param message Message décrivant l'erreur
     * @param cause La cause de l'exception
     */
    public AdminDeactivationValidationException(String message, Throwable cause) {
        super(message, 422);
        initCause(cause);
    }

    /**
     * Crée une exception pour des détails de désactivation invalides.
     *
     * @param details Les détails invalides
     * @return Une nouvelle instance de l'exception
     */
    public static AdminDeactivationValidationException forInvalidDetails(String details) {
        return new AdminDeactivationValidationException(
                String.format("Les détails de désactivation fournis ne sont pas valides : '%s'", details)
        );
    }

    /**
     * Crée une exception pour une catégorie de désactivation non autorisée.
     *
     * @param category La catégorie non autorisée
     * @param adminRole Le rôle de l'administrateur
     * @return Une nouvelle instance de l'exception
     */
    public static AdminDeactivationValidationException forUnauthorizedCategory(String category, String adminRole) {
        return new AdminDeactivationValidationException(
                String.format("La catégorie '%s' n'est pas autorisée pour un administrateur avec le rôle '%s'",
                        category, adminRole)
        );
    }

    /**
     * Crée une exception pour un utilisateur cible non valide.
     *
     * @param userId L'ID de l'utilisateur cible
     * @param reason La raison de l'invalidité
     * @return Une nouvelle instance de l'exception
     */
    public static AdminDeactivationValidationException forInvalidTargetUser(Long userId, String reason) {
        return new AdminDeactivationValidationException(
                String.format("L'utilisateur cible ID %d ne peut pas être désactivé : %s", userId, reason)
        );
    }

    /**
     * Crée une exception pour une tentative d'auto-désactivation.
     *
     * @return Une nouvelle instance de l'exception
     */
    public static AdminDeactivationValidationException forSelfDeactivation() {
        return new AdminDeactivationValidationException(
                "Un administrateur ne peut pas désactiver son propre compte",
                409 // Conflict comme SelfManagementException
        );
    }

    /**
     * Crée une exception pour des règles business violées.
     *
     * @param ruleName Le nom de la règle violée
     * @param details Les détails de la violation
     * @return Une nouvelle instance de l'exception
     */
    public static AdminDeactivationValidationException forBusinessRuleViolation(String ruleName, String details) {
        return new AdminDeactivationValidationException(
                String.format("Violation de la règle métier '%s' : %s", ruleName, details)
        );
    }
}
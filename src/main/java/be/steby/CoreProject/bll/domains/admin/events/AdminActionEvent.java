package be.steby.CoreProject.bll.domains.admin.events;

import be.steby.CoreProject.dl.entities.User;

import java.time.Instant;
import java.util.Map;

/**
 * Événement générique pour toutes les actions d'administration.
 * Cet événement peut être utilisé pour des actions qui n'ont pas d'événement spécifique
 * ou comme événement de base pour l'audit général des actions administratives.
 */
public record AdminActionEvent(
        /**
         * L'administrateur qui effectue l'action.
         */
        User adminUser,

        /**
         * L'utilisateur cible de l'action (optionnel).
         */
        User targetUser,

        /**
         * Type d'action effectuée.
         */
        AdminActionType actionType,

        /**
         * Description détaillée de l'action.
         */
        String actionDescription,

        /**
         * Résultat de l'action (SUCCESS, FAILURE, PARTIAL).
         */
        ActionResult result,

        /**
         * Message de résultat ou d'erreur.
         */
        String resultMessage,

        /**
         * Détails additionnels de l'action.
         */
        Map<String, Object> actionDetails,

        /**
         * Timestamp de l'événement.
         */
        Instant timestamp
) {

    /**
     * Énumération des types d'actions d'administration.
     */
    public enum AdminActionType {
        USER_CREATION("Création d'utilisateur"),
        USER_DELETION("Suppression d'utilisateur"),
        USER_ACTIVATION("Activation d'utilisateur"),
        USER_DEACTIVATION("Désactivation d'utilisateur"),
        ROLE_GRANT("Attribution de rôle"),
        ROLE_REVOKE("Révocation de rôle"),
        PASSWORD_RESET("Reset de mot de passe"),
        USER_SEARCH("Recherche d'utilisateurs"),
        DATA_EXPORT("Export de données"),
        SYSTEM_CONFIGURATION("Configuration système"),
        SECURITY_ACTION("Action de sécurité"),
        AUDIT_ACCESS("Accès aux logs d'audit"),
        OTHER("Autre action");

        private final String description;

        AdminActionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Énumération des résultats d'action.
     */
    public enum ActionResult {
        SUCCESS("Succès"),
        FAILURE("Échec"),
        PARTIAL("Partiel");

        private final String description;

        ActionResult(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Constructeur avec timestamp automatique.
     */
    public AdminActionEvent(
            User adminUser,
            User targetUser,
            AdminActionType actionType,
            String actionDescription,
            ActionResult result,
            String resultMessage,
            Map<String, Object> actionDetails) {
        this(adminUser, targetUser, actionType, actionDescription, result,
                resultMessage, actionDetails, Instant.now());
    }

    /**
     * Crée un événement d'action réussie.
     *
     * @param adminUser L'administrateur
     * @param targetUser L'utilisateur cible (optionnel)
     * @param actionType Le type d'action
     * @param actionDescription Description de l'action
     * @return Nouvel événement de succès
     */
    public static AdminActionEvent success(
            User adminUser,
            User targetUser,
            AdminActionType actionType,
            String actionDescription) {
        return new AdminActionEvent(
                adminUser, targetUser, actionType, actionDescription,
                ActionResult.SUCCESS, "Action exécutée avec succès",
                Map.of()
        );
    }

    /**
     * Crée un événement d'action échouée.
     *
     * @param adminUser L'administrateur
     * @param targetUser L'utilisateur cible (optionnel)
     * @param actionType Le type d'action
     * @param actionDescription Description de l'action
     * @param errorMessage Message d'erreur
     * @param requestContext Le contexte de la requête
     * @return Nouvel événement d'échec
     */
    public static AdminActionEvent failure(
            User adminUser,
            User targetUser,
            AdminActionType actionType,
            String actionDescription,
            String errorMessage) {
        return new AdminActionEvent(
                adminUser, targetUser, actionType, actionDescription,
                ActionResult.FAILURE, errorMessage,
                Map.of("error", true)
        );
    }

    /**
     * Crée un événement d'action avec détails personnalisés.
     *
     * @param adminUser L'administrateur
     * @param targetUser L'utilisateur cible (optionnel)
     * @param actionType Le type d'action
     * @param actionDescription Description de l'action
     * @param result Résultat de l'action
     * @param resultMessage Message de résultat
     * @param details Détails additionnels
     * @return Nouvel événement personnalisé
     */
    public static AdminActionEvent withDetails(
            User adminUser,
            User targetUser,
            AdminActionType actionType,
            String actionDescription,
            ActionResult result,
            String resultMessage,
            Map<String, Object> details) {
        return new AdminActionEvent(
                adminUser, targetUser, actionType, actionDescription,
                result, resultMessage, details
        );
    }

    /**
     * Obtient l'ID de l'administrateur.
     *
     * @return ID de l'administrateur
     */
    public Long getAdminUserId() {
        return adminUser.getId();
    }

    /**
     * Obtient l'ID de l'utilisateur cible.
     *
     * @return ID de l'utilisateur cible ou null
     */
    public Long getTargetUserId() {
        return targetUser != null ? targetUser.getId() : null;
    }

    /**
     * Obtient le nom d'utilisateur de l'administrateur.
     *
     * @return Nom d'utilisateur de l'admin
     */
    public String getAdminUsername() {
        return adminUser.getUsername();
    }

    /**
     * Obtient le nom d'utilisateur de l'utilisateur cible.
     *
     * @return Nom d'utilisateur cible ou null
     */
    public String getTargetUsername() {
        return targetUser != null ? targetUser.getUsername() : null;
    }

    /**
     * Vérifie si l'action a réussi.
     *
     * @return true si l'action a réussi
     */
    public boolean isSuccess() {
        return result == ActionResult.SUCCESS;
    }

    /**
     * Vérifie si l'action a échoué.
     *
     * @return true si l'action a échoué
     */
    public boolean isFailure() {
        return result == ActionResult.FAILURE;
    }

    /**
     * Vérifie si l'action concerne un utilisateur cible.
     *
     * @return true si un utilisateur cible est défini
     */
    public boolean hasTargetUser() {
        return targetUser != null;
    }

    /**
     * Vérifie si l'action a des détails additionnels.
     *
     * @return true si des détails sont disponibles
     */
    public boolean hasDetails() {
        return actionDetails != null && !actionDetails.isEmpty();
    }

    /**
     * Obtient un détail spécifique de l'action.
     *
     * @param key Clé du détail
     * @param <T> Type attendu
     * @return Valeur du détail ou null
     */
    @SuppressWarnings("unchecked")
    public <T> T getDetail(String key) {
        return actionDetails != null ? (T) actionDetails.get(key) : null;
    }

    /**
     * Vérifie si l'action est liée à la sécurité.
     *
     * @return true si c'est une action de sécurité
     */
    public boolean isSecurityAction() {
        return actionType == AdminActionType.SECURITY_ACTION ||
                actionType == AdminActionType.PASSWORD_RESET ||
                actionType == AdminActionType.USER_DEACTIVATION;
    }

    /**
     * Crée une description complète de l'événement.
     *
     * @return Description complète
     */
    public String getFullDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(String.format("[%s] %s par %s",
                actionType.getDescription(),
                actionDescription,
                getAdminUsername()));

        if (hasTargetUser()) {
            desc.append(String.format(" sur l'utilisateur '%s'", getTargetUsername()));
        }

        desc.append(String.format(" - Résultat: %s", result.getDescription()));

        if (resultMessage != null && !resultMessage.isBlank()) {
            desc.append(" (").append(resultMessage).append(")");
        }

        return desc.toString();
    }
}
package be.steby.CoreProject.bll.domains.admin.models;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Représente le résultat d'une opération d'administration.
 * Ce modèle encapsule les informations sur le succès/échec d'une opération
 * ainsi que les détails pertinents pour l'audit et le retour utilisateur.
 */
public record AdminOperationResult(
        /**
         * Indique si l'opération s'est déroulée avec succès.
         */
        boolean success,

        /**
         * Type d'opération effectuée.
         */
        String operationType,

        /**
         * ID de l'utilisateur concerné par l'opération.
         */
        Long affectedUserId,

        /**
         * Nom d'utilisateur de l'utilisateur concerné.
         */
        String affectedUsername,

        /**
         * Message de succès ou d'erreur.
         */
        String message,

        /**
         * Détails additionnels de l'opération (optionnel).
         */
        Map<String, Object> details,

        /**
         * Timestamp de l'opération.
         */
        Instant timestamp,

        /**
         * ID de l'administrateur qui a effectué l'opération.
         */
        Long performedBy
) {

    /**
     * Constructeur avec timestamp automatique.
     */
    public AdminOperationResult(
            boolean success,
            String operationType,
            Long affectedUserId,
            String affectedUsername,
            String message,
            Map<String, Object> details,
            Long performedBy) {
        this(success, operationType, affectedUserId, affectedUsername, message, details, Instant.now(), performedBy);
    }

    /**
     * Crée un résultat de succès pour une création d'utilisateur.
     *
     * @param createdUser L'utilisateur créé
     * @param performedBy L'ID de l'administrateur
     * @return Un résultat de succès
     */
    public static AdminOperationResult userCreated(User createdUser, Long performedBy) {
        return new AdminOperationResult(
                true,
                "USER_CREATION",
                createdUser.getId(),
                createdUser.getUsername(),
                String.format("Utilisateur '%s' créé avec succès", createdUser.getUsername()),
                Map.of(
                        "email", createdUser.getEmail(),
                        "roles", createdUser.getUserRoles(),
                        "activated", createdUser.isEnabled()
                ),
                performedBy
        );
    }

    /**
     * Crée un résultat de succès pour une attribution de rôle.
     *
     * @param user L'utilisateur concerné
     * @param grantedRole Le rôle accordé
     * @param performedBy L'ID de l'administrateur
     * @return Un résultat de succès
     */
    public static AdminOperationResult roleGranted(User user, UserRole grantedRole, Long performedBy) {
        return new AdminOperationResult(
                true,
                "ROLE_GRANT",
                user.getId(),
                user.getUsername(),
                String.format("Rôle '%s' accordé à l'utilisateur '%s'", grantedRole, user.getUsername()),
                Map.of(
                        "grantedRole", grantedRole,
                        "currentRoles", user.getUserRoles()
                ),
                performedBy
        );
    }

    /**
     * Crée un résultat de succès pour une révocation de rôle.
     *
     * @param user L'utilisateur concerné
     * @param revokedRole Le rôle révoqué
     * @param performedBy L'ID de l'administrateur
     * @return Un résultat de succès
     */
    public static AdminOperationResult roleRevoked(User user, UserRole revokedRole, Long performedBy) {
        return new AdminOperationResult(
                true,
                "ROLE_REVOKE",
                user.getId(),
                user.getUsername(),
                String.format("Rôle '%s' révoqué de l'utilisateur '%s'", revokedRole, user.getUsername()),
                Map.of(
                        "revokedRole", revokedRole,
                        "currentRoles", user.getUserRoles()
                ),
                performedBy
        );
    }

    /**
     * Crée un résultat de succès pour une activation d'utilisateur.
     *
     * @param user L'utilisateur activé
     * @param performedBy L'ID de l'administrateur
     * @return Un résultat de succès
     */
    public static AdminOperationResult userActivated(User user, Long performedBy) {
        return new AdminOperationResult(
                true,
                "USER_ACTIVATION",
                user.getId(),
                user.getUsername(),
                String.format("Utilisateur '%s' activé avec succès", user.getUsername()),
                Map.of("activated", true),
                performedBy
        );
    }

    /**
     * Crée un résultat de succès pour une désactivation d'utilisateur.
     *
     * @param user L'utilisateur désactivé
     * @param reason La raison de la désactivation
     * @param performedBy L'ID de l'administrateur
     * @return Un résultat de succès
     */
    public static AdminOperationResult userDeactivated(User user, String reason, Long performedBy) {
        return new AdminOperationResult(
                true,
                "USER_DEACTIVATION",
                user.getId(),
                user.getUsername(),
                String.format("Utilisateur '%s' désactivé", user.getUsername()),
                Map.of(
                        "activated", false,
                        "reason", reason
                ),
                performedBy
        );
    }

    /**
     * Crée un résultat d'échec pour une opération.
     *
     * @param operationType Le type d'opération qui a échoué
     * @param userId L'ID de l'utilisateur concerné (optionnel)
     * @param username Le nom d'utilisateur concerné (optionnel)
     * @param errorMessage Le message d'erreur
     * @param performedBy L'ID de l'administrateur
     * @return Un résultat d'échec
     */
    public static AdminOperationResult failure(
            String operationType,
            Long userId,
            String username,
            String errorMessage,
            Long performedBy) {
        return new AdminOperationResult(
                false,
                operationType,
                userId,
                username,
                errorMessage,
                Map.of("error", true),
                performedBy
        );
    }

    /**
     * Retourne une valeur spécifique des détails de l'opération.
     *
     * @param key La clé du détail recherché
     * @param <T> Le type attendu
     * @return La valeur ou null si non trouvée
     */
    @SuppressWarnings("unchecked")
    public <T> T getDetail(String key) {
        return details != null ? (T) details.get(key) : null;
    }

    /**
     * Vérifie si l'opération a des détails additionnels.
     *
     * @return true si des détails sont disponibles
     */
    public boolean hasDetails() {
        return details != null && !details.isEmpty();
    }
}
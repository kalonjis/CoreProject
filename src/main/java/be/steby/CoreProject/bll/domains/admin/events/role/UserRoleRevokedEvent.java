package be.steby.CoreProject.bll.domains.admin.events.role;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;

import java.time.Instant;
import java.util.Set;

/**
 * Événement émis lorsqu'un rôle est révoqué d'un utilisateur par un administrateur.
 * Cet événement contient toutes les informations nécessaires pour l'audit
 * et les notifications liées à la révocation de rôles.
 */
public record UserRoleRevokedEvent(
        /**
         * L'utilisateur dont le rôle est révoqué.
         */
        User targetUser,

        /**
         * L'administrateur qui révoque le rôle.
         */
        User adminUser,

        /**
         * Le rôle qui vient d'être révoqué.
         */
        UserRole revokedRole,

        /**
         * Les rôles de l'utilisateur avant la révocation.
         */
        Set<UserRole> previousRoles,

        /**
         * Les rôles de l'utilisateur après la révocation.
         */
        Set<UserRole> currentRoles,

        /**
         * Raison de la révocation du rôle (optionnel).
         */
        String reason,

        /**
         * Timestamp de l'événement.
         */
        Instant timestamp
) {

    /**
     * Constructeur avec timestamp automatique.
     */
    public UserRoleRevokedEvent(
            User targetUser,
            User adminUser,
            UserRole revokedRole,
            Set<UserRole> previousRoles,
            Set<UserRole> currentRoles,
            String reason) {
        this(targetUser, adminUser, revokedRole, previousRoles, currentRoles, reason, Instant.now());
    }

    /**
     * Crée un événement de révocation de rôle.
     *
     * @param targetUser L'utilisateur cible
     * @param adminUser L'administrateur
     * @param revokedRole Le rôle révoqué
     * @param previousRoles Les rôles précédents
     * @param currentRoles Les rôles actuels
     * @param reason La raison (optionnel)
     * @param requestContext Le contexte de la requête
     * @return Nouvel événement
     */
    public static UserRoleRevokedEvent of(
            User targetUser,
            User adminUser,
            UserRole revokedRole,
            Set<UserRole> previousRoles,
            Set<UserRole> currentRoles,
            String reason) {
        return new UserRoleRevokedEvent(
                targetUser, adminUser, revokedRole, previousRoles, currentRoles, reason
        );
    }

    /**
     * Vérifie si le rôle révoqué était un rôle administratif.
     *
     * @return true si le rôle révoqué était ADMIN, SUPER_ADMIN ou MODERATOR
     */
    public boolean isAdministrativeRole() {
        return revokedRole == UserRole.ADMIN ||
                revokedRole == UserRole.SUPER_ADMIN ||
                revokedRole == UserRole.MODERATOR;
    }

    /**
     * Vérifie si le rôle révoqué était SUPER_ADMIN.
     *
     * @return true si le rôle révoqué était SUPER_ADMIN
     */
    public boolean isSuperAdminRole() {
        return revokedRole == UserRole.SUPER_ADMIN;
    }

    /**
     * Vérifie si l'utilisateur perd tous ses privilèges administratifs.
     *
     * @return true si l'utilisateur n'a plus de rôles administratifs
     */
    public boolean losesAllAdministrativeRoles() {
        return isAdministrativeRole() &&
                currentRoles.stream().noneMatch(role ->
                        role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN || role == UserRole.MODERATOR
                );
    }

    /**
     * Vérifie si l'utilisateur devient un utilisateur standard.
     *
     * @return true si l'utilisateur n'a plus que le rôle USER
     */
    public boolean becomesStandardUser() {
        return currentRoles.size() == 1 && currentRoles.contains(UserRole.USER);
    }

    /**
     * Obtient l'ID de l'utilisateur cible.
     *
     * @return ID de l'utilisateur cible
     */
    public Long getTargetUserId() {
        return targetUser.getId();
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
     * Obtient le nom d'utilisateur de l'utilisateur cible.
     *
     * @return Nom d'utilisateur cible
     */
    public String getTargetUsername() {
        return targetUser.getUsername();
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
     * Vérifie si une raison a été fournie.
     *
     * @return true si une raison est présente
     */
    public boolean hasReason() {
        return reason != null && !reason.isBlank();
    }

    /**
     * Crée une description textuelle de l'événement.
     *
     * @return Description de l'événement
     */
    public String getDescription() {
        String base = String.format(
                "Rôle '%s' révoqué de l'utilisateur '%s' par l'administrateur '%s'",
                revokedRole,
                getTargetUsername(),
                getAdminUsername()
        );

        if (hasReason()) {
            base += ". Raison: " + reason;
        }

        return base;
    }

    /**
     * Crée un résumé des changements de rôles.
     *
     * @return Résumé des changements
     */
    public String getRoleChangeSummary() {
        return String.format(
                "Rôles: %s → %s (- %s)",
                previousRoles,
                currentRoles,
                revokedRole
        );
    }
}
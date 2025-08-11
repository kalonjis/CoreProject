package be.steby.CoreProject.bll.domains.admin.events;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;

import java.time.Instant;
import java.util.Set;

/**
 * Événement émis lorsqu'un rôle est accordé à un utilisateur par un administrateur.
 * Cet événement contient toutes les informations nécessaires pour l'audit
 * et les notifications liées à l'attribution de rôles.
 */
public record UserRoleGrantedEvent(
        /**
         * L'utilisateur qui reçoit le nouveau rôle.
         */
        User targetUser,

        /**
         * L'administrateur qui accorde le rôle.
         */
        User adminUser,

        /**
         * Le rôle qui vient d'être accordé.
         */
        UserRole grantedRole,

        /**
         * Les rôles de l'utilisateur avant l'attribution.
         */
        Set<UserRole> previousRoles,

        /**
         * Les rôles de l'utilisateur après l'attribution.
         */
        Set<UserRole> currentRoles,

        /**
         * Raison de l'attribution du rôle (optionnel).
         */
        String reason,

        /**
         * Contexte de la requête (IP, User-Agent, etc.).
         */
        RequestContext requestContext,

        /**
         * Timestamp de l'événement.
         */
        Instant timestamp
) {

    /**
     * Constructeur avec timestamp automatique.
     */
    public UserRoleGrantedEvent(
            User targetUser,
            User adminUser,
            UserRole grantedRole,
            Set<UserRole> previousRoles,
            Set<UserRole> currentRoles,
            String reason,
            RequestContext requestContext) {
        this(targetUser, adminUser, grantedRole, previousRoles, currentRoles, reason, requestContext, Instant.now());
    }

    /**
     * Crée un événement d'attribution de rôle.
     *
     * @param targetUser L'utilisateur cible
     * @param adminUser L'administrateur
     * @param grantedRole Le rôle accordé
     * @param previousRoles Les rôles précédents
     * @param currentRoles Les rôles actuels
     * @param reason La raison (optionnel)
     * @param requestContext Le contexte de la requête
     * @return Nouvel événement
     */
    public static UserRoleGrantedEvent of(
            User targetUser,
            User adminUser,
            UserRole grantedRole,
            Set<UserRole> previousRoles,
            Set<UserRole> currentRoles,
            String reason,
            RequestContext requestContext) {
        return new UserRoleGrantedEvent(
                targetUser, adminUser, grantedRole, previousRoles, currentRoles, reason, requestContext
        );
    }

    /**
     * Vérifie si le rôle accordé est un rôle administratif.
     *
     * @return true si le rôle accordé est ADMIN, SUPER_ADMIN ou MODERATOR
     */
    public boolean isAdministrativeRole() {
        return grantedRole == UserRole.ADMIN ||
                grantedRole == UserRole.SUPER_ADMIN ||
                grantedRole == UserRole.MODERATOR;
    }

    /**
     * Vérifie si le rôle accordé est SUPER_ADMIN.
     *
     * @return true si le rôle accordé est SUPER_ADMIN
     */
    public boolean isSuperAdminRole() {
        return grantedRole == UserRole.SUPER_ADMIN;
    }

    /**
     * Vérifie si l'utilisateur devient administrateur pour la première fois.
     *
     * @return true si c'est son premier rôle administratif
     */
    public boolean isFirstAdministrativeRole() {
        boolean hadAdminRole = previousRoles.stream().anyMatch(role ->
                role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN || role == UserRole.MODERATOR
        );
        return !hadAdminRole && isAdministrativeRole();
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
                "Rôle '%s' accordé à l'utilisateur '%s' par l'administrateur '%s'",
                grantedRole,
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
                "Rôles: %s → %s (+ %s)",
                previousRoles,
                currentRoles,
                grantedRole
        );
    }
}
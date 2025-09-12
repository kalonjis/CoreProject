package be.steby.CoreProject.bll.domains.admin.events;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;

import java.time.Instant;
import java.util.Set;

/**
 * Événement émis lorsqu'un utilisateur est créé par un administrateur.
 * Cet événement contient toutes les informations nécessaires pour l'audit
 * et les notifications liées à la création d'utilisateur par un admin.
 */
public record UserCreatedByAdminEvent(
        /**
         * L'utilisateur qui vient d'être créé.
         */
        User createdUser,

        /**
         * L'administrateur qui a effectué la création.
         */
        User adminUser,

        /**
         * Les rôles attribués lors de la création.
         */
        Set<UserRole> assignedRoles,

        /**
         * Indique si l'utilisateur a été automatiquement activé.
         */
        boolean autoActivated,

        /**
         * Timestamp de l'événement.
         */
        Instant timestamp
) {

    /**
     * Constructeur avec timestamp automatique.
     */
    public UserCreatedByAdminEvent(
            User createdUser,
            User adminUser,
            Set<UserRole> assignedRoles,
            boolean autoActivated) {
        this(createdUser, adminUser, assignedRoles, autoActivated, Instant.now());
    }

    /**
     * Crée un événement de création d'utilisateur par admin.
     *
     * @param createdUser L'utilisateur créé
     * @param adminUser L'administrateur
     * @param assignedRoles Les rôles assignés
     * @param autoActivated Si activation automatique
     * @return Nouvel événement
     */
    public static UserCreatedByAdminEvent of(
            User createdUser,
            User adminUser,
            Set<UserRole> assignedRoles,
            boolean autoActivated) {
        return new UserCreatedByAdminEvent(
                createdUser, adminUser, assignedRoles, autoActivated
        );
    }

    /**
     * Vérifie si l'utilisateur créé a des rôles administratifs.
     *
     * @return true si des rôles d'administration ont été attribués
     */
    public boolean hasAdministrativeRoles() {
        return assignedRoles.stream().anyMatch(role ->
                role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN || role == UserRole.MODERATOR
        );
    }

    /**
     * Vérifie si l'utilisateur créé a le rôle SUPER_ADMIN.
     *
     * @return true si le rôle SUPER_ADMIN a été attribué
     */
    public boolean hasSuperAdminRole() {
        return assignedRoles.contains(UserRole.SUPER_ADMIN);
    }

    /**
     * Obtient l'ID de l'utilisateur créé.
     *
     * @return ID de l'utilisateur créé
     */
    public Long getCreatedUserId() {
        return createdUser.getId();
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
     * Obtient le nom d'utilisateur de l'utilisateur créé.
     *
     * @return Nom d'utilisateur créé
     */
    public String getCreatedUsername() {
        return createdUser.getUsername();
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
     * Crée une description textuelle de l'événement.
     *
     * @return Description de l'événement
     */
    public String getDescription() {
        return String.format(
                "Utilisateur '%s' créé par l'administrateur '%s' avec les rôles: %s%s",
                getCreatedUsername(),
                getAdminUsername(),
                assignedRoles,
                autoActivated ? " (activé automatiquement)" : ""
        );
    }
}
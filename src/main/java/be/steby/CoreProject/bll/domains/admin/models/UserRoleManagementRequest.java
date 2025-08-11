package be.steby.CoreProject.bll.domains.admin.models;

import be.steby.CoreProject.bll.domains.admin.exceptions.AdminOperationException;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.pl.models.admin.UserRoleForm;

/**
 * Représente une demande de modification de rôle utilisateur par un administrateur.
 * Ce modèle encapsule toutes les informations nécessaires pour accorder ou révoquer
 * un rôle à un utilisateur existant.
 */
public record UserRoleManagementRequest(
        /**
         * ID de l'utilisateur dont les rôles doivent être modifiés.
         */
        Long userId,

        /**
         * Rôle à accorder ou révoquer.
         */
        UserRole role,

        /**
         * Type d'opération : GRANT pour accorder, REVOKE pour révoquer.
         */
        RoleOperation operation,

        /**
         * Raison de la modification de rôle (optionnel).
         */
        String reason,

        /**
         * Contexte de la requête pour l'audit.
         */
        RequestContext requestContext
) {

    /**
     * Énumération des types d'opérations sur les rôles.
     */
    public enum RoleOperation {
        GRANT("accorder"),
        REVOKE("révoquer");

        private final String description;

        RoleOperation(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Constructeur avec validation des données.
     */
    public UserRoleManagementRequest {
        if (userId == null || userId <= 0) {
            throw new AdminOperationException("L'ID utilisateur doit être valide");
        }
        if (role == null) {
            throw new AdminOperationException("Le rôle ne peut pas être null");
        }
        if (operation == null) {
            throw new AdminOperationException("L'opération ne peut pas être null");
        }
        if (requestContext == null) {
            throw new AdminOperationException("Le contexte de requête est requis pour l'audit");
        }
    }

    /**
     * Crée une requête d'attribution de rôle à partir d'un formulaire.
     *
     * @param form Formulaire de gestion des rôles
     * @param requestContext Contexte de la requête
     * @return Une nouvelle instance pour accorder un rôle
     */
    public static UserRoleManagementRequest fromFormForGrant(UserRoleForm form, RequestContext requestContext) {
        return new UserRoleManagementRequest(
                form.userId(),
                form.role(),
                RoleOperation.GRANT,
                form.reason(),
                requestContext
        );
    }

    /**
     * Crée une requête de révocation de rôle à partir d'un formulaire.
     *
     * @param form Formulaire de gestion des rôles
     * @param requestContext Contexte de la requête
     * @return Une nouvelle instance pour révoquer un rôle
     */
    public static UserRoleManagementRequest fromFormForRevoke(UserRoleForm form, RequestContext requestContext) {
        return new UserRoleManagementRequest(
                form.userId(),
                form.role(),
                RoleOperation.REVOKE,
                form.reason(),
                requestContext
        );
    }

    /**
     * Crée une requête d'attribution de rôle simple.
     *
     * @param userId ID de l'utilisateur
     * @param role Rôle à accorder
     * @param requestContext Contexte de la requête
     * @return Une nouvelle instance pour accorder un rôle
     */
    public static UserRoleManagementRequest forGrant(Long userId, UserRole role, RequestContext requestContext) {
        return new UserRoleManagementRequest(userId, role, RoleOperation.GRANT, null, requestContext);
    }

    /**
     * Crée une requête de révocation de rôle simple.
     *
     * @param userId ID de l'utilisateur
     * @param role Rôle à révoquer
     * @param requestContext Contexte de la requête
     * @return Une nouvelle instance pour révoquer un rôle
     */
    public static UserRoleManagementRequest forRevoke(Long userId, UserRole role, RequestContext requestContext) {
        return new UserRoleManagementRequest(userId, role, RoleOperation.REVOKE, null, requestContext);
    }

    /**
     * Vérifie si l'opération nécessite des privilèges SUPER_ADMIN.
     *
     * @return true si le rôle concerné est SUPER_ADMIN
     */
    public boolean requiresSuperAdminPrivileges() {
        return role == UserRole.SUPER_ADMIN;
    }

    /**
     * Vérifie si l'opération concerne un rôle d'administration.
     *
     * @return true si le rôle est ADMIN, SUPER_ADMIN ou MODERATOR
     */
    public boolean isAdministrativeRole() {
        return role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN || role == UserRole.MODERATOR;
    }

    /**
     * Retourne une description lisible de l'opération.
     *
     * @return Description de l'opération en cours
     */
    public String getOperationDescription() {
        return String.format("%s le rôle %s %s l'utilisateur ID %d",
                operation.getDescription().substring(0, 1).toUpperCase() + operation.getDescription().substring(1),
                role,
                operation == RoleOperation.GRANT ? "à" : "de",
                userId);
    }
}
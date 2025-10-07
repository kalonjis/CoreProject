package be.steby.CoreProject.bll.domains.admin.models;

import be.steby.CoreProject.bll.domains.admin.exceptions.AdminOperationException;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.pl.domains.admin.models.UserRoleForm;

public record UserRoleManagementRequest(
        Long userId,
        UserRole role,
        RoleOperation operation,
        String reason
) {

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
    }

    // ✅ CORRECTION : Prendre userId séparément et utiliser form.userRole()
    /**
     * Crée une requête d'attribution de rôle à partir d'un formulaire et d'un userId.
     *
     * @param userId ID de l'utilisateur (vient du path parameter)
     * @param form Formulaire de gestion des rôles
     * @return Une nouvelle instance pour accorder un rôle
     */
    public static UserRoleManagementRequest fromFormForGrant(Long userId, UserRoleForm form) {
        return new UserRoleManagementRequest(
                userId,                    // ✅ Pris séparément
                form.userRole(),          // ✅ Méthode correcte
                RoleOperation.GRANT,
                null                  // ✅ Pas de reason pour l'instant
        );
    }

    /**
     * Crée une requête de révocation de rôle à partir d'un formulaire et d'un userId.
     *
     * @param userId ID de l'utilisateur (vient du path parameter)
     * @param form Formulaire de gestion des rôles
     * @return Une nouvelle instance pour révoquer un rôle
     */
    public static UserRoleManagementRequest fromFormForRevoke(Long userId, UserRoleForm form) {
        return new UserRoleManagementRequest(
                userId,                    // ✅ Pris séparément
                form.userRole(),          // ✅ Méthode correcte
                RoleOperation.REVOKE,
                null                 // ✅ Pas de reason pour l'instant
        );
    }

    // ✅ Méthodes statiques simples (gardées telles quelles)
    public static UserRoleManagementRequest forGrant(Long userId, UserRole role) {
        return new UserRoleManagementRequest(userId, role, RoleOperation.GRANT, null);
    }

    public static UserRoleManagementRequest forRevoke(Long userId, UserRole role) {
        return new UserRoleManagementRequest(userId, role, RoleOperation.REVOKE, null);
    }

    // ✅ Méthodes utilitaires (gardées telles quelles)
    public boolean requiresSuperAdminPrivileges() {
        return role == UserRole.SUPER_ADMIN;
    }

    public boolean isAdministrativeRole() {
        return role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN || role == UserRole.MODERATOR;
    }

    public String getOperationDescription() {
        return String.format("%s le rôle %s %s l'utilisateur ID %d",
                operation.getDescription().substring(0, 1).toUpperCase() + operation.getDescription().substring(1),
                role,
                operation == RoleOperation.GRANT ? "à" : "de",
                userId);
    }
}
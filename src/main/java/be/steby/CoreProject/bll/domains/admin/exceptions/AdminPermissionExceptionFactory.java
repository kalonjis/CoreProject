package be.steby.CoreProject.bll.domains.admin.exceptions;

import be.steby.CoreProject.dl.enums.UserRole;

/**
 * Factory for creating admin domain specific permission exceptions.
 */
public class AdminPermissionExceptionFactory {

    public static AdminPermissionException forInsufficientAdminPrivileges(String action) {
        return new AdminPermissionException(
                String.format("Insufficient admin privileges to %s. Admin role required.", action)
        );
    }

    public static AdminPermissionException forSelfTargeting(String action) {
        return new AdminPermissionException(
                String.format("Admin cannot %s on their own account via administrative functions. " +
                        "Contact another administrator.", action)
        );
    }

    public static AdminPermissionException forUnauthorizedTargetRole(String action, UserRole targetRole) {
        return new AdminPermissionException(
                String.format("Admin cannot %s a user with role %s. Insufficient permissions in role hierarchy.",
                        action, targetRole.name())
        );
    }

    public static AdminPermissionException forSuperAdminRequired(String action) {
        return new AdminPermissionException(
                String.format("Insufficient privileges to %s. This operation requires SUPER_ADMIN role. " +
                        "Contact a super administrator if this action is necessary.", action)
        );
    }
}
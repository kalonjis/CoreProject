package be.steby.CoreProject.bll.domains.admin.services.role;

import be.steby.CoreProject.bll.common.exceptions.AttributeUnchangedException;
import be.steby.CoreProject.dl.enums.UserRole;

/**
 * Service for admin operations on user roles.
 * Handles role assignment and revocation with proper permission validation.
 *
 * This service focuses exclusively on role management.
 * For user account operations, see AdminUserAccountService.
 * For search operations, see AdminSearchService.
 *
 * Permission rules:
 * - SUPER_ADMIN can grant/revoke any role to/from anyone
 * - ADMIN can grant/revoke MODERATOR and USER roles (but not ADMIN or SUPER_ADMIN)
 * - MODERATOR and USER cannot grant or revoke any roles
 */
public interface AdminRoleService {

    /**
     * Grants a role to a user as an administrator.
     *
     * Permission validation:
     * - Checks if actor has sufficient privileges to grant the role
     * - Prevents granting roles to users with higher existing roles
     * - SUPER_ADMIN required for granting ADMIN or SUPER_ADMIN roles
     * - ADMIN required for granting MODERATOR or USER roles
     *
     * @param userId ID of the user receiving the role
     * @param role Role to grant
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException if user doesn't exist
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException if actor lacks permission
     * @throws AttributeUnchangedException if user already has the role
     */
    void grantRole(Long userId, UserRole role);

    /**
     * Revokes a role from a user as an administrator.
     *
     * Permission validation:
     * - Uses same permission logic as granting roles
     * - Checks if actor has sufficient privileges to revoke the role
     * - SUPER_ADMIN required for revoking ADMIN or SUPER_ADMIN roles
     * - ADMIN required for revoking MODERATOR or USER roles
     *
     * @param userId ID of the user losing the role
     * @param role Role to revoke
     * @throws be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException if user doesn't exist
     * @throws be.steby.CoreProject.bll.common.exceptions.UserPermissionException if actor lacks permission
     * @throws AttributeUnchangedException if user doesn't have the role
     */
    void revokeRole(Long userId, UserRole role);
}
package be.steby.CoreProject.bll.domains.admin.models.role;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;

/**
 * Event published when a role is revoked from a user by an administrator.
 * This event contains all necessary information for audit logging
 * and notifications related to role revocation.
 */
public record AdminRoleRevokedEvent(
        /**
         * The user who lost the role.
         */
        User targetUser,

        /**
         * The administrator who revoked the role.
         */
        User adminUser,

        /**
         * The role that was revoked.
         */
        UserRole revokedRole
) {
}
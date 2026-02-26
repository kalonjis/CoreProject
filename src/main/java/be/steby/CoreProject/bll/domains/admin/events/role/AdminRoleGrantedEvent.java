package be.steby.CoreProject.bll.domains.admin.events.role;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;

/**
 * Event published when a role is granted to a user by an administrator.
 * This event contains all necessary information for audit logging
 * and notifications related to role assignment.
 */
public record AdminRoleGrantedEvent(
        /**
         * The user who received the role.
         */
        User targetUser,

        /**
         * The administrator who granted the role.
         */
        User adminUser,

        /**
         * The device used to revoke the role.
         */
        Device device,

        /**
         * The role that was granted.
         */
        UserRole grantedRole
) {}
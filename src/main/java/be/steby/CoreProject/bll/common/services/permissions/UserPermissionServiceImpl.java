package be.steby.CoreProject.bll.common.services.permissions;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Implementation of user permission service.
 * Handles all permission checks between users based on role hierarchy.
 *
 * Does NOT depend on UserService to avoid circular dependencies.
 * All methods take User objects as parameters instead of fetching them.
 *
 * Role Hierarchy (highest to lowest):
 * 1. SUPER_ADMIN - Can do everything
 * 2. ADMIN - Can manage MODERATOR and USER roles
 * 3. MODERATOR - Limited admin capabilities
 * 4. USER - Standard user capabilities
 */
@Service
@Slf4j
public class UserPermissionServiceImpl implements UserPermissionService {

    // Self-deactivation configuration properties
    @Value("${security.account-deactivation.allow-admin-deactivation:false}")
    private boolean allowAdminSelfDeactivation;

    @Value("${security.account-deactivation.allow-super-admin-deactivation:false}")
    private boolean allowSuperAdminSelfDeactivation;

    @Value("${security.account-deactivation.allow-moderator-deactivation:true}")
    private boolean allowModeratorSelfDeactivation;

    // ===============================
    // USER DEACTIVATION PERMISSIONS
    // ===============================

    /**
     * Checks if an actor can deactivate a target user.
     * Users cannot deactivate themselves via admin deactivation.
     * Permission is based on role hierarchy.
     *
     * @param actor The user performing the action
     * @param target The user being deactivated
     * @return true if actor can deactivate target, false otherwise
     */
    @Override
    public boolean canDeactivateUser(User actor, User target) {
        if (actor == null || target == null) {
            log.warn("Cannot check deactivation permission: actor or target is null");
            return false;
        }

        // A user cannot deactivate themselves via admin deactivation
        if (actor.getId().equals(target.getId())) {
            log.debug("User {} cannot admin-deactivate themselves", actor.getUsername());
            return false;
        }

        // Check permissions according to role hierarchy
        return canActOnUser(actor, target);
    }

    /**
     * Checks if a user can deactivate their own account (self-deactivation).
     * Rules vary by role based on configuration properties.
     *
     * @param user The user wanting to deactivate themselves
     * @return true if user can self-deactivate, false otherwise
     */
    @Override
    public boolean canSelfDeactivate(User user) {
        if (user == null) {
            log.warn("Cannot check self-deactivation permission: user is null");
            return false;
        }

        Set<UserRole> userRoles = user.getUserRoles();

        // Check role-specific rules (from highest to lowest)
        if (userRoles.contains(UserRole.SUPER_ADMIN)) {
            log.debug("Super-admin self-deactivation allowed: {}", allowSuperAdminSelfDeactivation);
            return allowSuperAdminSelfDeactivation;
        }

        if (userRoles.contains(UserRole.ADMIN)) {
            log.debug("Admin self-deactivation allowed: {}", allowAdminSelfDeactivation);
            return allowAdminSelfDeactivation;
        }

        if (userRoles.contains(UserRole.MODERATOR)) {
            log.debug("Moderator self-deactivation allowed: {}", allowModeratorSelfDeactivation);
            return allowModeratorSelfDeactivation;
        }

        // USER can always self-deactivate
        log.debug("User self-deactivation allowed by default");
        return true;
    }

    // ===============================
    // USER ACTIVATION PERMISSIONS
    // ===============================

    /**
     * Checks if an actor can activate a target user.
     * Uses the same logic as deactivation based on role hierarchy.
     *
     * @param actor The user performing the action
     * @param target The user being activated
     * @return true if actor can activate target, false otherwise
     */
    @Override
    public boolean canActivateUser(User actor, User target) {
        if (actor == null || target == null) {
            log.warn("Cannot check activation permission: actor or target is null");
            return false;
        }

        // Same logic as deactivation
        return canActOnUser(actor, target);
    }

    // ===============================
    // ROLE MANAGEMENT PERMISSIONS
    // ===============================

    /**
     * Checks if an actor can grant a specific role to a target user.
     *
     * Permission rules:
     * - SUPER_ADMIN can grant any role to anyone
     * - ADMIN can grant MODERATOR and USER roles (but not ADMIN or SUPER_ADMIN)
     * - MODERATOR and USER cannot grant any roles
     *
     * Additional security:
     * - Cannot grant role to user who already has a higher role
     *
     * @param actor The user attempting to grant the role
     * @param target The user who will receive the role
     * @param roleToGrant The role being granted
     * @return true if actor can grant this role to target, false otherwise
     */
    @Override
    public boolean canGrantRole(User actor, User target, UserRole roleToGrant) {
        // All parameters are required
        if (actor == null || target == null || roleToGrant == null) {
            log.warn("Cannot check role grant permission: actor, target, or role is null");
            return false;
        }

        UserRole actorHighestRole = getHighestRole(actor);
        UserRole targetHighestRole = getHighestRole(target);

        // SUPER_ADMIN can grant any role to anyone
        if (actorHighestRole == UserRole.SUPER_ADMIN) {
            log.debug("SUPER_ADMIN {} can grant role {} to user {}",
                    actor.getUsername(), roleToGrant, target.getUsername());
            return true;
        }

        // ADMIN can grant MODERATOR and USER roles (but not ADMIN or SUPER_ADMIN)
        if (actorHighestRole == UserRole.ADMIN) {
            // Security check: Cannot grant role to user with higher existing role
            if (targetHighestRole == UserRole.SUPER_ADMIN || targetHighestRole == UserRole.ADMIN) {
                log.debug("Admin {} cannot grant {} to user {} with higher role {}",
                        actor.getUsername(), roleToGrant, target.getUsername(), targetHighestRole);
                return false;
            }

            // Can only grant MODERATOR or USER roles
            boolean canGrant = roleToGrant == UserRole.MODERATOR || roleToGrant == UserRole.USER;
            log.debug("Admin {} {} grant role {} to user {}",
                    actor.getUsername(), canGrant ? "can" : "cannot",
                    roleToGrant, target.getUsername());
            return canGrant;
        }

        // Other roles (MODERATOR, USER) cannot grant any roles
        log.debug("User {} with role {} cannot grant role {}",
                actor.getUsername(), actorHighestRole, roleToGrant);
        return false;
    }

    /**
     * Checks if an actor can revoke a specific role from a target user.
     * Uses the same permission logic as granting roles.
     *
     * @param actor The user attempting to revoke the role
     * @param target The user losing the role
     * @param roleToRevoke The role being revoked
     * @return true if actor can revoke this role from target, false otherwise
     */
    @Override
    public boolean canRevokeRole(User actor, User target, UserRole roleToRevoke) {
        // For revocation, we need an existing target (cannot revoke from non-existent user)
        if (actor == null || target == null || roleToRevoke == null) {
            log.warn("Cannot check role revoke permission: actor, target, or role is null");
            return false;
        }

        // Same logic as granting
        return canGrantRole(actor, target, roleToRevoke);
    }

    // ===============================
    // GENERAL PERMISSION CHECKS
    // ===============================

    /**
     * Checks if an actor can perform actions on a target user
     * based on role hierarchy.
     *
     * Permission rules:
     * - SUPER_ADMIN can act on everyone (except self-targeting in some contexts)
     * - ADMIN can act on MODERATOR and USER (but not SUPER_ADMIN or other ADMIN)
     * - MODERATOR and USER cannot act on other accounts
     *
     * @param actor The user performing the action
     * @param target The user being acted upon
     * @return true if actor has permission to act on target, false otherwise
     */
    @Override
    public boolean canActOnUser(User actor, User target) {
        if (actor == null || target == null) {
            log.warn("Cannot check action permission: actor or target is null");
            return false;
        }

        UserRole actorRole = getHighestRole(actor);
        UserRole targetRole = getHighestRole(target);

        // SUPER_ADMIN can act on everyone (self-targeting handled in specific methods)
        if (actorRole == UserRole.SUPER_ADMIN) {
            log.debug("SUPER_ADMIN {} can act on user {}",
                    actor.getUsername(), target.getUsername());
            return true;
        }

        // ADMIN can act on MODERATOR and USER (but not SUPER_ADMIN or other ADMIN)
        if (actorRole == UserRole.ADMIN) {
            boolean canAct = targetRole != UserRole.SUPER_ADMIN && targetRole != UserRole.ADMIN;
            log.debug("Admin {} {} act on user {} with role {}",
                    actor.getUsername(), canAct ? "can" : "cannot",
                    target.getUsername(), targetRole);
            return canAct;
        }

        // MODERATOR and USER cannot act on other accounts
        log.debug("User {} with role {} cannot act on user {} with role {}",
                actor.getUsername(), actorRole, target.getUsername(), targetRole);
        return false;
    }

    // ===============================
    // ROLE UTILITY METHODS
    // ===============================

    /**
     * Gets the highest role of a user from their role set.
     *
     * Hierarchy (highest to lowest):
     * SUPER_ADMIN > ADMIN > MODERATOR > USER
     *
     * @param user The user to check
     * @return The highest role, or USER if user has no roles
     */
    @Override
    public UserRole getHighestRole(User user) {
        if (user == null || user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
            log.debug("User has no roles, defaulting to USER");
            return UserRole.USER; // Default role
        }

        Set<UserRole> roles = user.getUserRoles();

        // Check in descending hierarchy order
        if (roles.contains(UserRole.SUPER_ADMIN)) {
            log.trace("User {} highest role: SUPER_ADMIN", user.getUsername());
            return UserRole.SUPER_ADMIN;
        }
        if (roles.contains(UserRole.ADMIN)) {
            log.trace("User {} highest role: ADMIN", user.getUsername());
            return UserRole.ADMIN;
        }
        if (roles.contains(UserRole.MODERATOR)) {
            log.trace("User {} highest role: MODERATOR", user.getUsername());
            return UserRole.MODERATOR;
        }

        log.trace("User {} highest role: USER", user.getUsername());
        return UserRole.USER;
    }

    /**
     * Checks if a user has administrative privileges.
     * Administrative privileges = ADMIN or SUPER_ADMIN role.
     *
     * @param user The user to check
     * @return true if user has ADMIN or SUPER_ADMIN role, false otherwise
     */
    @Override
    public boolean hasAdminPrivileges(User user) {
        if (user == null) {
            log.debug("Cannot check admin privileges: user is null");
            return false;
        }

        UserRole highestRole = getHighestRole(user);
        boolean hasPrivileges = highestRole == UserRole.ADMIN || highestRole == UserRole.SUPER_ADMIN;

        log.debug("User {} has admin privileges: {}", user.getUsername(), hasPrivileges);
        return hasPrivileges;
    }

    /**
     * Checks if a user has super admin privileges.
     * Only users with SUPER_ADMIN role have these privileges.
     *
     * @param user The user to check
     * @return true if user has SUPER_ADMIN role, false otherwise
     */
    @Override
    public boolean hasSuperAdminPrivileges(User user) {
        if (user == null) {
            log.debug("Cannot check super admin privileges: user is null");
            return false;
        }

        boolean hasPrivileges = getHighestRole(user) == UserRole.SUPER_ADMIN;
        log.debug("User {} has super admin privileges: {}", user.getUsername(), hasPrivileges);
        return hasPrivileges;
    }
}
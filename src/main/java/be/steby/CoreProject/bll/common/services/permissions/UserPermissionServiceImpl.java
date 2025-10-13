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

        // Check role-specific rules (from highest to lowest)
        if (user.isSuperAdmin()) {
            log.debug("Super-admin self-deactivation allowed: {}", allowSuperAdminSelfDeactivation);
            return allowSuperAdminSelfDeactivation;
        }

        if (user.isAdmin()) {
            log.debug("Admin self-deactivation allowed: {}", allowAdminSelfDeactivation);
            return allowAdminSelfDeactivation;
        }

        if (user.hasRole(UserRole.MODERATOR)) {
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
     * - ADMIN can grant MODERATOR and USER roles to anyone (but cannot grant ADMIN or SUPER_ADMIN)
     * - MODERATOR and USER cannot grant any roles
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

        UserRole actorHighestRole = actor.getHighestRole();

        // SUPER_ADMIN can grant any role to anyone
        if (actorHighestRole == UserRole.SUPER_ADMIN) {
            log.debug("SUPER_ADMIN {} can grant role {} to user {}",
                    actor.getUsername(), roleToGrant, target.getUsername());
            return true;
        }

        // ADMIN can grant MODERATOR and USER roles (but not ADMIN or SUPER_ADMIN)
        if (actorHighestRole == UserRole.ADMIN) {
            // Check the ROLE being granted, not the target's existing roles
            if (roleToGrant == UserRole.SUPER_ADMIN || roleToGrant == UserRole.ADMIN) {
                log.debug("Admin {} cannot grant administrative role {} to any user",
                        actor.getUsername(), roleToGrant);
                return false;
            }

            // Can grant MODERATOR or USER roles to anyone
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
     *
     * Permission rules:
     * - SUPER_ADMIN can revoke any role from anyone
     * - ADMIN can revoke MODERATOR and USER roles, but ONLY from users with lower hierarchy
     * - ADMIN cannot revoke roles from other ADMINs or SUPER_ADMINs
     * - MODERATOR and USER cannot revoke any roles
     *
     * @param actor The user attempting to revoke the role
     * @param target The user losing the role
     * @param roleToRevoke The role being revoked
     * @return true if actor can revoke this role from target, false otherwise
     */
    @Override
    public boolean canRevokeRole(User actor, User target, UserRole roleToRevoke) {
        // All parameters are required
        if (actor == null || target == null || roleToRevoke == null) {
            log.warn("Cannot check role revoke permission: actor, target, or role is null");
            return false;
        }

        UserRole actorRole = actor.getHighestRole();
        UserRole targetRole = target.getHighestRole();

        // SUPER_ADMIN can revoke any role from anyone
        if (actorRole == UserRole.SUPER_ADMIN) {
            log.debug("SUPER_ADMIN {} can revoke role {} from user {}",
                    actor.getUsername(), roleToRevoke, target.getUsername());
            return true;
        }

        // ADMIN can revoke MODERATOR and USER roles, but ONLY from lower hierarchy users
        if (actorRole == UserRole.ADMIN) {
            // Cannot touch SUPER_ADMIN or other ADMINs
            if (targetRole == UserRole.SUPER_ADMIN || targetRole == UserRole.ADMIN) {
                log.debug("Admin {} cannot revoke {} from user {} with equal/higher role {}",
                        actor.getUsername(), roleToRevoke, target.getUsername(), targetRole);
                return false;
            }

            // Can only revoke MODERATOR or USER roles
            boolean canRevoke = roleToRevoke == UserRole.MODERATOR || roleToRevoke == UserRole.USER;
            log.debug("Admin {} {} revoke role {} from user {}",
                    actor.getUsername(), canRevoke ? "can" : "cannot",
                    roleToRevoke, target.getUsername());
            return canRevoke;
        }

        // Other roles (MODERATOR, USER) cannot revoke any roles
        log.debug("User {} with role {} cannot revoke role {}",
                actor.getUsername(), actorRole, roleToRevoke);
        return false;
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

        UserRole actorRole = actor.getHighestRole();
        UserRole targetRole = target.getHighestRole();

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


}
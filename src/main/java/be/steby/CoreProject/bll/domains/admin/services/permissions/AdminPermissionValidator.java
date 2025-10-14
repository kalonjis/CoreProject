package be.steby.CoreProject.bll.domains.admin.services.permissions;

import be.steby.CoreProject.bll.domains.admin.exceptions.AdminPermissionExceptionFactory;
import be.steby.CoreProject.bll.domains.admin.exceptions.AdminPermissionException;
import be.steby.CoreProject.bll.domains.admin.exceptions.InvalidAdminArgumentException;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsible for validating administrative permissions.
 *
 * This is the ONLY service needed for admin permission validation.
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPermissionValidator {

    // ===============================
    // PUBLIC VALIDATION METHODS
    // ===============================

    /**
     * Validates strict role hierarchy for admin actions.
     *
     * Rules:
     * - SUPER_ADMIN can act on: ADMIN, MODERATOR, USER, GUEST (NOT other SUPER_ADMINs)
     * - ADMIN can act on: MODERATOR, USER, GUEST (NOT SUPER_ADMIN or other ADMINs)
     * - Others: Cannot perform admin actions
     *
     * Use case: Most admin operations (password reset, role management, etc.)
     *
     * @param actor The admin performing the action
     * @param target The user being acted upon
     * @param allowSelfTargeting Whether the action can target oneself
     * @param action Description of the action (for logging/errors)
     * @throws AdminPermissionException if validation fails
     */
    public void validateStrictHierarchy(User actor, User target, boolean allowSelfTargeting, String action) {
        // Step 1: Basic checks (admin privileges + self-targeting)
        validateBasicAdminAction(actor, target, allowSelfTargeting, action);

        // Step 2: Check role hierarchy
        if (!canActOnUserStrictHierarchy(actor, target)) {
            UserRole targetRole = target.getHighestRole();
            throw AdminPermissionExceptionFactory.forUnauthorizedTargetRole(action, targetRole);
        }

        log.debug("Strict hierarchy validation passed - actor: {} can {} on target: {}",
                actor.getUsername(), action, target.getUsername());
    }

    /**
     * Validates admin action with exception for SUPER_ADMIN.
     *
     * Rules:
     * - SUPER_ADMIN can act on: ADMIN, MODERATOR, USER, GUEST (NOT other SUPER_ADMINs)
     * - ADMIN can act on: OTHER ADMINs, MODERATOR, USER, GUEST (NOT SUPER_ADMIN)
     *
     * Use case: Special operations where ADMINs can modify other ADMINs but not SUPER_ADMINs
     *
     * @param actor The admin performing the action
     * @param target The user being acted upon
     * @param allowSelfTargeting Whether the action can target oneself
     * @param action Description of the action (for logging/errors)
     * @throws AdminPermissionException if validation fails
     */
    public void validateAdminActionExceptSuperAdmin(User actor, User target, boolean allowSelfTargeting, String action) {
        // Step 1: Basic checks
        validateBasicAdminAction(actor, target, allowSelfTargeting, action);

        // Step 2: Block action on SUPER_ADMIN
        if (target.isSuperAdmin()) {
            throw AdminPermissionExceptionFactory.forUnauthorizedTargetRole(action, UserRole.SUPER_ADMIN);
        }

        log.debug("Admin action validation passed - actor: {} can {} on target: {} (except SUPER_ADMIN)",
                actor.getUsername(), action, target.getUsername());
    }

    /**
     * Validates admin action on ALL users without role restrictions.
     *
     * Rules:
     * - Any admin (ADMIN or SUPER_ADMIN) can act on ANY user
     * - No role hierarchy checks
     *
     * Use case: Read-only operations, viewing user data, device management
     *
     * @param actor The admin performing the action
     * @param target The user being acted upon
     * @param allowSelfTargeting Whether the action can target oneself
     * @param action Description of the action (for logging/errors)
     * @throws AdminPermissionException if validation fails
     */
    public void validateAdminActionOnAllUsers(User actor, User target, boolean allowSelfTargeting, String action) {
        // Only basic checks - no role hierarchy validation
        validateBasicAdminAction(actor, target, allowSelfTargeting, action);

        log.debug("Admin validation passed - actor: {} can {} on target: {} (no role restrictions)",
                actor.getUsername(), action, target.getUsername());
    }

    /**
     * Validates that ONLY SUPER_ADMIN can perform this action.
     *
     * Rules:
     * - Only SUPER_ADMIN can perform this action
     * - ADMIN is blocked
     *
     * Use case: Critical system operations, permanent deletions, security audits
     *
     * @param actor The admin performing the action
     * @param target The user being acted upon
     * @param allowSelfTargeting Whether the action can target oneself
     * @param action Description of the action (for logging/errors)
     * @throws AdminPermissionException if validation fails
     */
    public void validateSuperAdminAction(User actor, User target, boolean allowSelfTargeting, String action) {
        // Step 1: Verify SUPER_ADMIN role
        if (!actor.isSuperAdmin()) {
            throw AdminPermissionExceptionFactory.forSuperAdminRequired(action);
        }

        // Step 2: Self-targeting check
        if (!allowSelfTargeting && actor.getId().equals(target.getId())) {
            throw AdminPermissionExceptionFactory.forSelfTargeting(action);
        }

        // Step 3: SUPER_ADMIN can act on everyone - no additional checks
        log.debug("SUPER_ADMIN validation passed - actor: {} can {} on target: {}",
                actor.getUsername(), action, target.getUsername());
    }

    // ===============================
    // PRIVATE HELPER METHODS
    // ===============================

    /**
     * Common validations for all admin actions:
     * 1. Verify actor has admin privileges (ADMIN or SUPER_ADMIN)
     * 2. Check self-targeting rules
     *
     * @param actor The user performing the action
     * @param target The user being acted upon
     * @param allowSelfTargeting Whether the action allows self-targeting
     * @param action Description of the action (for logging/errors)
     * @throws AdminPermissionException if validations fail
     */
    private void validateBasicAdminAction(User actor, User target, boolean allowSelfTargeting, String action) {
        if (actor == null || target == null) {
            log.warn("Cannot check action permission: actor or target is null");
            throw new InvalidAdminArgumentException("Cannot check action permission: actor or target is null");
        }
        // Validation : Verify actor has admin privileges
        if (!actor.hasAdminPrivileges()) {
            throw AdminPermissionExceptionFactory.forInsufficientAdminPrivileges(action);
        }

        // Validation : Self-targeting check
        if (!allowSelfTargeting && actor.getId().equals(target.getId())) {
            throw AdminPermissionExceptionFactory.forSelfTargeting(action);
        }
    }

    /**
     * Checks if an actor can perform actions on a target user based on STRICT role hierarchy.
     *
     * Permission rules:
     * - SUPER_ADMIN can act on: ADMIN, MODERATOR, USER, GUEST (NOT other SUPER_ADMINs)
     * - ADMIN can act on: MODERATOR, USER, GUEST (NOT SUPER_ADMIN or other ADMINs)
     * - MODERATOR/USER: Cannot act on other accounts
     *
     * @param actor The user performing the action
     * @param target The user being acted upon
     * @return true if actor has permission to act on target, false otherwise
     */
    private boolean canActOnUserStrictHierarchy(User actor, User target) {
        if (actor == null || target == null) {
            log.warn("Cannot check action permission: actor or target is null");
            return false;
        }

        UserRole actorRole = actor.getHighestRole();
        UserRole targetRole = target.getHighestRole();

        // SUPER_ADMIN can act on everyone EXCEPT other SUPER_ADMINs
        if (actorRole == UserRole.SUPER_ADMIN) {
            boolean canAct = targetRole != UserRole.SUPER_ADMIN;
            log.debug("SUPER_ADMIN {} {} act on user {} with role {}",
                    actor.getUsername(), canAct ? "can" : "cannot",
                    target.getUsername(), targetRole);
            return canAct;
        }

        // ADMIN can act on MODERATOR and USER (but NOT SUPER_ADMIN or other ADMINs)
        if (actorRole == UserRole.ADMIN) {
            boolean canAct = targetRole != UserRole.SUPER_ADMIN && targetRole != UserRole.ADMIN;
            log.debug("ADMIN {} {} act on user {} with role {}",
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
package be.steby.CoreProject.bll.domains.admin.services.permissions;

import be.steby.CoreProject.bll.common.services.permissions.UserPermissionService;
import be.steby.CoreProject.bll.domains.admin.exceptions.AdminPermissionExceptionFactory;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminValidationService {

    private final UserPermissionService userPermissionService;

    /**
     * Basic admin validation - Admin can only act on GHEST/USER/MODERATOR
     * ADMIN cannot act on other ADMIN
     * SUPER_ADMIN can only act on ADMIN/USER/MODERATOR/GHEST not on SUPER_ADMIN
     */
    public void validateStrictHierarchy (User actor, User target, boolean allowSelfTargeting, String action) {
        // 1. Verify actor has admin privileges
        if (!userPermissionService.hasAdminPrivileges(actor)) {
            throw AdminPermissionExceptionFactory.forInsufficientAdminPrivileges(action);
        }

        // 2. Self-targeting check
        if (!allowSelfTargeting && actor.getId().equals(target.getId())) {
            throw AdminPermissionExceptionFactory.forSelfTargeting(action);
        }

        // 3. CORRECTED: Use role hierarchy instead of absolute restriction
        if (!userPermissionService.canActOnUser(actor, target)) {
            UserRole targetRole = userPermissionService.getHighestRole(target);
            throw AdminPermissionExceptionFactory.forUnauthorizedTargetRole(action, targetRole);
        }
    }

    /**
     * Exception for SUPER_ADMIN - Admin can act on everyone EXCEPT SUPER_ADMIN
     * ADMIN can act on USER/MODERATOR/other ADMIN but NOT SUPER_ADMIN
     */
    public void validateAdminActionExceptSuperAdmin(User actor, User target, boolean allowSelfTargeting, String action) {
        // 1. Verify actor has admin privileges
        if (!userPermissionService.hasAdminPrivileges(actor)) {
            throw AdminPermissionExceptionFactory.forInsufficientAdminPrivileges(action);
        }

        // 2. Self-targeting check
        if (!allowSelfTargeting && actor.getId().equals(target.getId())) {
            throw AdminPermissionExceptionFactory.forSelfTargeting(action);
        }

        // 3. Use existing canActOnUser logic (ADMIN can't touch SUPER_ADMIN)
        if (!userPermissionService.canActOnUser(actor, target)) {
            UserRole targetRole = userPermissionService.getHighestRole(target);
            throw AdminPermissionExceptionFactory.forUnauthorizedTargetRole(action, targetRole);
        }
    }


    /**
     * Admin can act on ALL users - Just check admin privileges and self-targeting
     * No role hierarchy restrictions - ADMIN can even act on SUPER_ADMIN
     */
    public void validateAdminActionOnAllUsers(User actor, User target, boolean allowSelfTargeting, String action) {
        // 1. Verify actor has admin privileges (ADMIN or SUPER_ADMIN)
        if (!userPermissionService.hasAdminPrivileges(actor)) {
            throw AdminPermissionExceptionFactory.forInsufficientAdminPrivileges(action);
        }

        // 2. Self-targeting check
        if (!allowSelfTargeting && actor.getId().equals(target.getId())) {
            throw AdminPermissionExceptionFactory.forSelfTargeting(action);
        }

        // 3. No role hierarchy checks - admin can act on everyone
        log.debug("Admin validation successful - actor: {} can {} on target: {} (no role restrictions)",
                actor.getUsername(), action, target.getUsername());
    }

    /**
     * Full admin validation - Only SUPER_ADMIN can act on everyone
     * ADMIN gets blocked, only SUPER_ADMIN passes
     */
    public void validateSuperAdminAction(User actor, User target, boolean allowSelfTargeting, String action) {
        // 1. Verify actor has SUPER_ADMIN privileges
        if (!userPermissionService.hasSuperAdminPrivileges(actor)) {
            throw AdminPermissionExceptionFactory.forSuperAdminRequired(action);
        }

        // 2. Self-targeting check
        if (!allowSelfTargeting && actor.getId().equals(target.getId())) {
            throw AdminPermissionExceptionFactory.forSelfTargeting(action);
        }

        // 3. SUPER_ADMIN can act on everyone - no additional checks needed
    }
}

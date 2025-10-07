package be.steby.CoreProject.bll.domains.admin.services.role;

import be.steby.CoreProject.bll.common.exceptions.UserPermissionExceptionFactory;
import be.steby.CoreProject.bll.common.services.permissions.UserPermissionService;
import be.steby.CoreProject.bll.domains.admin.events.AdminRoleGrantedEvent;
import be.steby.CoreProject.bll.domains.admin.events.AdminRoleRevokedEvent;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for admin operations on user roles.
 * Handles role assignment and revocation with proper permission validation.
 *
 * Responsibilities:
 * - Orchestrates role management operations (business logic)
 * - Validates permissions (who can grant/revoke what)
 * - Publishes domain events (for notifications, audit, etc.)
 * - Delegates technical operations to specialized services
 *
 * Delegation strategy:
 * - UserService → Core user persistence and role state management
 * - UserPermissionService → Permission validation
 * - ApplicationEventPublisher → Event publishing for audit/notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminRoleServiceImpl implements AdminRoleService {

    private final UserService userService;
    private final UserPermissionService userPermissionService;
    private final ApplicationEventPublisher eventPublisher;

    // ===============================
    // ROLE GRANTING
    // ===============================

    @Override
    @Transactional
    public void grantRole(Long userId, UserRole role) {
        log.debug("Admin role grant request - targetId: {}, role: {}", userId, role);

        // 1. Get actors
        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserById(userId);

        // 2. Validate permissions
        if (!userPermissionService.canGrantRole(actor, target, role)) {
            log.warn("Admin {} attempted to grant role {} without permission to user {}",
                    actor.getUsername(), role, target.getUsername());

            // Provide appropriate error message based on role
            if (role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN) {
                throw UserPermissionExceptionFactory.forInsufficientPermissions(
                        UserRole.SUPER_ADMIN, "grant role " + role);
            } else {
                throw UserPermissionExceptionFactory.forInsufficientPermissions(
                        UserRole.ADMIN, "grant role " + role);
            }
        }

        // 3. Delegate to user service for persistence
        userService.grantUserRole(userId, role);

        // 4. Publish role granted event
        eventPublisher.publishEvent(new AdminRoleGrantedEvent(target, actor, role) );

        log.info("Role {} successfully granted to user {} by admin {}",
                role, target.getUsername(), actor.getUsername());
    }

    // ===============================
    // ROLE REVOCATION
    // ===============================

    @Override
    @Transactional
    public void revokeRole(Long userId, UserRole role) {
        log.debug("Admin role revoke request - targetId: {}, role: {}", userId, role);

        // 1. Get actors
        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserById(userId);

        // 2. Validate permissions
        if (!userPermissionService.canRevokeRole(actor, target, role)) {
            log.warn("Admin {} attempted to revoke role {} without permission from user {}",
                    actor.getUsername(), role, target.getUsername());

            throw UserPermissionExceptionFactory.forInsufficientPermissions(
                    userPermissionService.getHighestRole(actor), "revoke role " + role);
        }

        // 3. Delegate to user service for persistence
        userService.revokeUserRole(userId, role);

        // 4. Publish role revoked event
        eventPublisher.publishEvent(new AdminRoleRevokedEvent(target, actor, role ));

        log.info("Role {} successfully revoked from user {} by admin {}",
                role, target.getUsername(), actor.getUsername());
    }
}
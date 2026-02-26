package be.steby.CoreProject.bll.domains.admin.listeners;

import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserActivatedEvent;
import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserCreatedEvent;
import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserDeactivatedEvent;
import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserDeletedEvent;
import be.steby.CoreProject.bll.domains.admin.events.role.AdminRoleGrantedEvent;
import be.steby.CoreProject.bll.domains.admin.events.role.AdminRoleRevokedEvent;
import be.steby.CoreProject.bll.domains.admin.services.AdminActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for admin domain activity logging.
 *
 * <p>Each handler is deliberately thin: it delegates immediately to
 * {@link AdminActivityLogService} and owns only the try/catch guard so that
 * a logging failure never propagates back to the caller.</p>
 *
 * <p>All handlers run on the dedicated {@code activityLogExecutor} thread pool,
 * keeping activity persistence fully off the HTTP request thread.</p>
 *
 * <p>{@code @Order(100)} ensures this listener runs after notification listeners
 * — a logging failure never blocks email delivery or other critical flows.</p>
 *
 * <p>Covered events → AdminAction mapping:</p>
 * <ul>
 *   <li>{@link AdminUserCreatedEvent}     → ADMIN_USER_CREATED</li>
 *   <li>{@link AdminUserActivatedEvent}   → ADMIN_USER_ACTIVATED / ADMIN_USER_REACTIVATED</li>
 *   <li>{@link AdminUserDeactivatedEvent} → ADMIN_USER_DEACTIVATED</li>
 *   <li>{@link AdminUserDeletedEvent}     → ADMIN_USER_DELETED</li>
 *   <li>{@link AdminRoleGrantedEvent}     → ADMIN_ROLE_GRANTED</li>
 *   <li>{@link AdminRoleRevokedEvent}     → ADMIN_ROLE_REVOKED</li>
 * </ul>
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class AdminActivityLogListener {

    private final AdminActivityLogService adminActivityLogService;

    // =========================================================================
    // User Management
    // =========================================================================

    /**
     * Handles admin user creation events.
     *
     * @param event the admin user created event
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleAdminUserCreated(AdminUserCreatedEvent event) {
        try {
            log.debug("Processing admin user creation event — admin: {}, created: {}",
                    event.getAdminUsername(), event.getCreatedUsername());
            adminActivityLogService.logUserCreated(event);
        } catch (Exception e) {
            log.error("Failed to log admin user creation — admin: {}, created: {}",
                    event.getAdminUsername(), event.getCreatedUsername(), e);
        }
    }

    /**
     * Handles admin user activation events.
     * Maps to ADMIN_USER_ACTIVATED or ADMIN_USER_REACTIVATED based on context.
     *
     * @param event the admin user activated event
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleAdminUserActivated(AdminUserActivatedEvent event) {
        try {
            log.debug("Processing admin user activation event — admin: {}, target: {}",
                    event.getAdminUsername(), event.targetUser().getUsername());
            adminActivityLogService.logUserActivated(event);
        } catch (Exception e) {
            log.error("Failed to log admin user activation — admin: {}, target: {}",
                    event.getAdminUsername(), event.targetUser().getUsername(), e);
        }
    }

    /**
     * Handles admin user deactivation events.
     *
     * @param event the admin user deactivated event
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleAdminUserDeactivated(AdminUserDeactivatedEvent event) {
        try {
            log.debug("Processing admin user deactivation event — admin: {}, target: {}, category: {}",
                    event.getAdminUsername(), event.getTargetUsername(), event.category());
            adminActivityLogService.logUserDeactivated(event);

            // Extra logging for security-related deactivations
            if (event.isSecurityRelated()) {
                log.warn("Security-related user deactivation — target: {} by admin: {}, category: {}",
                        event.getTargetUsername(), event.getAdminUsername(), event.category());
            }
        } catch (Exception e) {
            log.error("Failed to log admin user deactivation — admin: {}, target: {}",
                    event.getAdminUsername(), event.getTargetUsername(), e);
        }
    }

    /**
     * Handles admin user deletion events.
     *
     * @param event the admin user deleted event
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleAdminUserDeleted(AdminUserDeletedEvent event) {
        try {
            log.debug("Processing admin user deletion event — admin: {}, deleted: {}, gdpr: {}",
                    event.getAdminUsername(), event.deletedUsername(), event.gdprDeletion());
            adminActivityLogService.logUserDeleted(event);

            // Extra logging for deletions
            if (event.gdprDeletion()) {
                log.info("GDPR user deletion performed — deleted: {} by admin: {}",
                        event.deletedUsername(), event.getAdminUsername());
            } else {
                log.warn("Hard user deletion performed — deleted: {} by admin: {}",
                        event.deletedUsername(), event.getAdminUsername());
            }
        } catch (Exception e) {
            log.error("Failed to log admin user deletion — admin: {}, deleted: {}",
                    event.getAdminUsername(), event.deletedUsername(), e);
        }
    }

    // =========================================================================
    // Role Management
    // =========================================================================

    /**
     * Handles admin role granted events.
     *
     * @param event the admin role granted event
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleAdminRoleGranted(AdminRoleGrantedEvent event) {
        try {
            log.debug("Processing admin role granted event — admin: {}, target: {}, role: {}",
                    event.adminUser().getUsername(), event.targetUser().getUsername(), event.grantedRole());
            adminActivityLogService.logRoleGranted(event);
        } catch (Exception e) {
            log.error("Failed to log admin role granted — admin: {}, target: {}, role: {}",
                    event.adminUser().getUsername(), event.targetUser().getUsername(), event.grantedRole(), e);
        }
    }

    /**
     * Handles admin role revoked events.
     *
     * @param event the admin role revoked event
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleAdminRoleRevoked(AdminRoleRevokedEvent event) {
        try {
            log.debug("Processing admin role revoked event — admin: {}, target: {}, role: {}",
                    event.adminUser().getUsername(), event.targetUser().getUsername(), event.revokedRole());
            adminActivityLogService.logRoleRevoked(event);
        } catch (Exception e) {
            log.error("Failed to log admin role revoked — admin: {}, target: {}, role: {}",
                    event.adminUser().getUsername(), event.targetUser().getUsername(), event.revokedRole(), e);
        }
    }

    // =========================================================================
    // Generic Admin Action (fallback)
    // =========================================================================

    /**
     * Handles generic admin action events.
     * Used for actions not covered by specific event handlers.
     *
     * @param event the generic admin action event
     */
//    @EventListener
//    @Async("activityLogExecutor")
//    public void handleAdminAction(AdminActionEvent event) {
//        try {
//            log.debug("Processing generic admin action event — type: {}, admin: {}",
//                    event.actionType(), event.getAdminUsername());
//            adminActivityLogService.logGenericAdminAction(event);
//
//            // Extra logging for security actions
//            if (event.actionType() == AdminActionEvent.AdminActionType.SECURITY_ACTION) {
//                log.info("Security action performed — admin: {}, description: {}",
//                        event.getAdminUsername(), event.actionDescription());
//            }
//
//            // Warning for failed critical actions
//            if (event.isFailure() && isCriticalAction(event.actionType())) {
//                log.warn("Critical admin action failed — type: {}, admin: {}, error: {}",
//                        event.actionType(), event.getAdminUsername(), event.resultMessage());
//            }
//        } catch (Exception e) {
//            log.error("Failed to log generic admin action — type: {}, admin: {}",
//                    event.actionType(), event.getAdminUsername(), e);
//        }
//    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Determines if an action type is considered critical for alerting purposes.
     */
//    private boolean isCriticalAction(AdminActionEvent.AdminActionType actionType) {
//        return switch (actionType) {
//            case USER_DELETION, USER_DEACTIVATION, ROLE_REVOKE, SECURITY_ACTION -> true;
//            default -> false;
//        };
//    }
}
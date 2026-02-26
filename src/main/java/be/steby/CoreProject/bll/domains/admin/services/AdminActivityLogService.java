// ============================================================================
// FILE 4: AdminActivityLogService.java
// Location: src/main/java/be/steby/CoreProject/bll/domains/admin/services/AdminActivityLogService.java
// ============================================================================

package be.steby.CoreProject.bll.domains.admin.services;

import be.steby.CoreProject.bll.common.services.activitylog.ActivityLogService;
import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserActivatedEvent;
import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserCreatedEvent;
import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserDeactivatedEvent;
import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserDeletedEvent;
import be.steby.CoreProject.bll.domains.admin.events.role.AdminRoleGrantedEvent;
import be.steby.CoreProject.bll.domains.admin.events.role.AdminRoleRevokedEvent;
import be.steby.CoreProject.bll.domains.admin.listeners.AdminActivityLogListener;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.action_log_type.AdminAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Admin domain activity log service.
 *
 * <p>Translates admin domain events into {@link be.steby.CoreProject.dl.entities.ActivityLog}
 * persistence calls via the generic {@link ActivityLogService} base.
 * Each method maps to an {@link AdminAction} constant.</p>
 *
 * <p>Admin actions always involve an admin user performing an action, often
 * targeting another user. The target user information is stored in
 * {@code actionDetails} since {@code ActivityLog} doesn't have a dedicated
 * {@code targetUser} field.</p>
 *
 * <p>Called exclusively from {@link AdminActivityLogListener}, which already
 * runs on the {@code activityLogExecutor} thread pool.</p>
 */
@Service
@Slf4j
public class AdminActivityLogService extends ActivityLogService {

    public AdminActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "ADMIN";
    }

    // =========================================================================
    // User Management
    // =========================================================================

    /**
     * Persists an {@link AdminAction#ADMIN_USER_CREATED} entry when an admin
     * creates a new user account.
     *
     * @param event contains the admin, created user and temporary password
     */
    public void logUserCreated(AdminUserCreatedEvent event) {
        String details = String.format("target: %s (email: %s)",
                event.getCreatedUsername(), event.getCreatedUserEmail());
        logUserActivity(event.adminUser(), event.device(), AdminAction.ADMIN_USER_CREATED, true, details);

        log.debug("ADMIN_USER_CREATED logged — admin: {}, created: {}",
                event.getAdminUsername(), event.getCreatedUsername());
    }

    /**
     * Persists an {@link AdminAction#ADMIN_USER_ACTIVATED} entry when an admin
     * manually activates a user account.
     *
     * @param event contains the admin and target user
     */
    public void logUserActivated(AdminUserActivatedEvent event) {
        AdminAction action = event.isReactivation()
                ? AdminAction.ADMIN_USER_REACTIVATED
                : AdminAction.ADMIN_USER_ACTIVATED;

        String details = buildTargetUserDetails(event.targetUser());
        logUserActivity(event.adminUser(), event.device(), action, true, details);

        log.debug("{} logged — admin: {}, target: {}",
                action.getName(), event.getAdminUsername(), event.targetUser().getUsername());
    }

    /**
     * Persists an {@link AdminAction#ADMIN_USER_DEACTIVATED} entry when an admin
     * deactivates a user account.
     *
     * @param event contains the admin, target user, category and comment
     */
    public void logUserDeactivated(AdminUserDeactivatedEvent event) {
        String details = buildDeactivationDetails(event);
        logUserActivity(event.adminUser(), event.device(), AdminAction.ADMIN_USER_DEACTIVATED, true, details);

        log.debug("ADMIN_USER_DEACTIVATED logged — admin: {}, target: {}, category: {}",
                event.getAdminUsername(), event.getTargetUsername(), event.category());
    }

    /**
     * Persists an {@link AdminAction#ADMIN_USER_DELETED} entry when an admin
     * deletes a user account.
     *
     * @param event contains the admin and deleted user info
     */
    public void logUserDeleted(AdminUserDeletedEvent event) {
        String deleteType = event.gdprDeletion() ? "GDPR" : "hard";
        String details = String.format("deleted: %s (id: %d, email: %s) | type: %s",
                event.deletedUsername(), event.deletedUserId(), event.deletedEmail(), deleteType);
        logUserActivity(event.adminUser(), event.device(), AdminAction.ADMIN_USER_DELETED, true, details);

        log.debug("ADMIN_USER_DELETED logged — admin: {}, deleted: {}, type: {}",
                event.getAdminUsername(), event.deletedUsername(), deleteType);
    }

    // =========================================================================
    // Role Management
    // =========================================================================

    /**
     * Persists an {@link AdminAction#ADMIN_ROLE_GRANTED} entry.
     *
     * @param event contains admin, target user and granted role
     */
    public void logRoleGranted(AdminRoleGrantedEvent event) {
        String details = String.format("target: %s | role: %s granted",
                event.targetUser().getUsername(), event.grantedRole().name());
        logUserActivity(event.adminUser(), event.device(), AdminAction.ADMIN_ROLE_GRANTED, true, details);

        log.debug("ADMIN_ROLE_GRANTED logged — admin: {}, target: {}, role: {}",
                event.adminUser().getUsername(), event.targetUser().getUsername(), event.grantedRole());
    }

    /**
     * Persists an {@link AdminAction#ADMIN_ROLE_REVOKED} entry.
     *
     * @param event contains admin, target user and revoked role
     */
    public void logRoleRevoked(AdminRoleRevokedEvent event) {
        String details = String.format("target: %s | role: %s revoked",
                event.targetUser().getUsername(), event.revokedRole().name());
        logUserActivity(event.adminUser(), event.device(), AdminAction.ADMIN_ROLE_REVOKED, true, details);

        log.debug("ADMIN_ROLE_REVOKED logged — admin: {}, target: {}, role: {}",
                event.adminUser().getUsername(), event.targetUser().getUsername(), event.revokedRole());
    }

    // =========================================================================
    // Security Actions
    // =========================================================================

    /**
     * Persists an {@link AdminAction#ADMIN_FORCE_LOGOUT} entry when an admin
     * terminates user session(s).
     *
     * @param adminUser  the admin performing the action
     * @param targetUser the user being logged out
     * @param allSessions whether all sessions were terminated
     */
    public void logForceLogout(User adminUser, User targetUser, boolean allSessions) {
        String scope = allSessions ? "all sessions" : "single session";
        String details = String.format("target: %s | scope: %s", targetUser.getUsername(), scope);
        logUserActivity(adminUser, null, AdminAction.ADMIN_FORCE_LOGOUT, true, details);

        log.debug("ADMIN_FORCE_LOGOUT logged — admin: {}, target: {}, scope: {}",
                adminUser.getUsername(), targetUser.getUsername(), scope);
    }

    /**
     * Persists an {@link AdminAction#ADMIN_ACCOUNT_UNLOCKED} entry.
     *
     * @param adminUser  the admin performing the action
     * @param targetUser the user whose account was unlocked
     */
    public void logAccountUnlocked(User adminUser, User targetUser) {
        String details = buildTargetUserDetails(targetUser);
        logUserActivity(adminUser, null, AdminAction.ADMIN_ACCOUNT_UNLOCKED, true, details);

        log.debug("ADMIN_ACCOUNT_UNLOCKED logged — admin: {}, target: {}",
                adminUser.getUsername(), targetUser.getUsername());
    }

    // =========================================================================
    // Audit & Data
    // =========================================================================

    /**
     * Persists an {@link AdminAction#ADMIN_AUDIT_ACCESSED} entry when an admin
     * accesses audit logs.
     *
     * @param adminUser the admin accessing the logs
     * @param scope     description of what was accessed (e.g. "user:123 logs")
     */
    public void logAuditAccessed(User adminUser, String scope) {
        String details = scope != null ? "scope: " + scope : null;
        logUserActivity(adminUser, null, AdminAction.ADMIN_AUDIT_ACCESSED, true, details);

        log.debug("ADMIN_AUDIT_ACCESSED logged — admin: {}, scope: {}",
                adminUser.getUsername(), scope);
    }

    /**
     * Persists an {@link AdminAction#ADMIN_DATA_EXPORTED} entry.
     *
     * @param adminUser  the admin performing the export
     * @param targetUser the user whose data was exported (nullable for bulk exports)
     * @param exportType type of export (e.g. "GDPR", "audit", "full")
     */
    public void logDataExported(User adminUser, User targetUser, String exportType) {
        String details = targetUser != null
                ? String.format("target: %s | type: %s", targetUser.getUsername(), exportType)
                : String.format("type: %s", exportType);
        logUserActivity(adminUser, null, AdminAction.ADMIN_DATA_EXPORTED, true, details);

        log.debug("ADMIN_DATA_EXPORTED logged — admin: {}, target: {}, type: {}",
                adminUser.getUsername(),
                targetUser != null ? targetUser.getUsername() : "bulk",
                exportType);
    }

    /**
     * Persists an {@link AdminAction#ADMIN_USER_SEARCHED} entry.
     *
     * @param adminUser the admin performing the search
     * @param query     the search query or criteria
     */
    public void logUserSearched(User adminUser, String query) {
        String details = "query: " + (query != null ? query : "all");
        logUserActivity(adminUser, null, AdminAction.ADMIN_USER_SEARCHED, true, details);

        log.debug("ADMIN_USER_SEARCHED logged — admin: {}", adminUser.getUsername());
    }

    // =========================================================================
    // System Configuration
    // =========================================================================

    /**
     * Persists an {@link AdminAction#ADMIN_CONFIG_CHANGED} entry.
     *
     * @param adminUser  the admin changing configuration
     * @param configKey  the configuration key changed
     * @param oldValue   the previous value (nullable)
     * @param newValue   the new value
     */
    public void logConfigChanged(User adminUser, String configKey, String oldValue, String newValue) {
        String details = String.format("key: %s | %s → %s",
                configKey,
                oldValue != null ? oldValue : "null",
                newValue);
        logUserActivity(adminUser, null, AdminAction.ADMIN_CONFIG_CHANGED, true, details);

        log.debug("ADMIN_CONFIG_CHANGED logged — admin: {}, key: {}",
                adminUser.getUsername(), configKey);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private String buildTargetUserDetails(User target) {
        return String.format("target: %s (id: %d)", target.getUsername(), target.getId());
    }

    private String buildDeactivationDetails(AdminUserDeactivatedEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("target: ").append(event.getTargetUsername());
        sb.append(" | category: ").append(event.category().name());
        if (event.hasComment()) {
            sb.append(" | comment: ").append(event.comment());
        }
        return sb.toString();
    }

//    private String buildGenericDetails(AdminActionEvent event) {
//        StringBuilder sb = new StringBuilder();
//        if (event.hasTargetUser()) {
//            sb.append("target: ").append(event.getTargetUsername());
//        }
//        if (event.actionDescription() != null && !event.actionDescription().isBlank()) {
//            if (!sb.isEmpty()) sb.append(" | ");
//            sb.append("action: ").append(event.actionDescription());
//        }
//        return sb.isEmpty() ? null : sb.toString();
//    }
//
//    private AdminAction mapEventTypeToAction(AdminActionEvent.AdminActionType eventType) {
//        return switch (eventType) {
//            case USER_CREATION -> AdminAction.ADMIN_USER_CREATED;
//            case USER_DELETION -> AdminAction.ADMIN_USER_DELETED;
//            case USER_ACTIVATION -> AdminAction.ADMIN_USER_ACTIVATED;
//            case USER_DEACTIVATION -> AdminAction.ADMIN_USER_DEACTIVATED;
//            case ROLE_GRANT -> AdminAction.ADMIN_ROLE_GRANTED;
//            case ROLE_REVOKE -> AdminAction.ADMIN_ROLE_REVOKED;
//            case PASSWORD_RESET -> AdminAction.ADMIN_PASSWORD_RESET;
//            case USER_SEARCH -> AdminAction.ADMIN_USER_SEARCHED;
//            case DATA_EXPORT -> AdminAction.ADMIN_DATA_EXPORTED;
//            case SYSTEM_CONFIGURATION -> AdminAction.ADMIN_CONFIG_CHANGED;
//            case SECURITY_ACTION -> AdminAction.ADMIN_FORCE_LOGOUT;
//            case AUDIT_ACCESS -> AdminAction.ADMIN_AUDIT_ACCESSED;
//            case OTHER -> AdminAction.ADMIN_CONFIG_CHANGED; // fallback
//        };
//    }
}
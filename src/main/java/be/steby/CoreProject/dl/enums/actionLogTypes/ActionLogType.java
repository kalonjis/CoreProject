package be.steby.CoreProject.dl.enums.actionLogTypes;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.bll.common.models.RequestContext;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Sealed interface for all activity log action types.
 * Replaces the monolithic enum with domain-specific enums.
 *
 * Currently permits:
 * - AuthAction (authentication domain)
 *
 * Future domains to be added:
 * - AdminAction, AccountAction, SecurityAction, etc.
 */
public sealed interface ActionLogType
        permits AuthAction {
    // TODO: Add other domains: AdminAction, AccountAction, SecurityAction,
    //       PasswordAction, EmailAction, DeviceAction, ApiAction, etc.

    // ================== ABSTRACT METHODS (must be implemented by all enums) ==================

    /**
     * Human-readable description of the action
     */
    String getDescription();

    /**
     * Category/domain of the action (AUTH, ADMIN, ACCOUNT, etc.)
     */
    String getCategory();

    // Note: name() is automatically provided by enum implementations

    // ================== COMMON DEFAULT METHODS ==================

    /**
     * Central pattern matching dispatcher - calls the appropriate domain-specific processor
     */
    default String processAction(ActivityLog log) {
        return switch (this) {
            case AuthAction auth -> auth.processAuthAction(log);
            // TODO: Add other domains when implemented
            // case AdminAction admin -> admin.processAdminAction(log);
            // case AccountAction account -> account.processAccountAction(log);
            // case SecurityAction security -> security.processSecurityAction(log);
        };
    }

    /**
     * Central enrichment dispatcher - calls the appropriate domain-specific enrichment
     */
    default void enrichActivityLog(ActivityLog log) {
        switch (this) {
            case AuthAction auth -> auth.enrichAuthLog(log);
            // TODO: Add other domains when implemented
            // case AdminAction admin -> admin.enrichAdminLog(log);
            // case AccountAction account -> account.enrichAccountLog(log);
            // case SecurityAction security -> security.enrichSecurityLog(log);
        }
    }

    /**
     * Get default risk level for this action type
     */
    default int getDefaultRiskLevel() {
        return 1; // Low risk by default
    }

    /**
     * Factory method to create a basic ActivityLog for this action type
     */
    default ActivityLog createActivityLog(User user, boolean successful) {
        return ActivityLog.builderWithTimestamp()
                .user(user)
                .actionType(((Enum<?>) this).name())  // Cast needed for sealed interface
                .actionCategory(this.getCategory())
                .successful(successful)
                .build();
    }

    /**
     * Factory method to create ActivityLog with context
     */
    default ActivityLog createActivityLogWithContext(User user, boolean successful, RequestContext context) {
        ActivityLog log = createActivityLog(user, successful);

        if (context != null) {
            log.setIpAddress(context.getClientIp());
            log.setLocation(null); // À enrichir par le service
            log.setSessionId(context.getSessionId());
            log.setUserAgent(context.getUserAgent());
        }

        // Auto-enrich based on action type
        this.enrichActivityLog(log);

        return log;
    }

    // ================== STATIC UTILITY METHODS ==================

    /**
     * Convert string representation back to sealed type
     * This is the key method for reconstructing types from database
     */
    static ActionLogType fromString(String actionName, String category) {
        return switch (category.toUpperCase()) {
            case "AUTH" -> AuthAction.valueOf(actionName);
            // TODO: Add other domains when implemented
            // case "ADMIN" -> AdminAction.valueOf(actionName);
            // case "ACCOUNT" -> AccountAction.valueOf(actionName);
            // case "SECURITY" -> SecurityAction.valueOf(actionName);
            default -> throw new IllegalArgumentException(
                    "Unknown action category: " + category + " for action: " + actionName);
        };
    }
}
package be.steby.CoreProject.dl.enums.actionLogTypes;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.bll.common.models.RequestContext;

/**
 * Sealed interface for all activity log action types - KISS VERSION.
 * Only essential methods, no over-engineering.
 *
 * Domains: AUTH, ADMIN, ACCOUNT, EMAIL, PASSWORD, DEVICE
 * (SecurityAction removed - redundant)
 */
public sealed interface ActionLogType
        permits AuthAction, AdminAction, AccountAction, EmailAction, PasswordAction, DeviceAction {

    // ================== CORE METHODS ONLY ==================

    /**
     * Human-readable description of the action
     */
    String getDescription();

    /**
     * Domain category (AUTH, ADMIN, ACCOUNT, EMAIL, PASSWORD, DEVICE)
     */
    String getCategory();

    /**
     * Risk level (1=low, 5=critical) - simple default implementation
     */
    default int getDefaultRiskLevel() {
        return 1; // Safe default
    }

    /**
     * Simple factory method to create ActivityLog
     */
    default ActivityLog createActivityLog(User user, boolean successful) {
        return ActivityLog.builderWithTimestamp()
                .user(user)
                .actionType(((Enum<?>) this).name())
                .actionCategory(this.getCategory())
                .successful(successful)
                .riskLevel(this.getDefaultRiskLevel())
                .build();
    }

    /**
     * Factory method with context - KISS version
     */
    default ActivityLog createActivityLogWithContext(User user, boolean successful, RequestContext context) {
        ActivityLog log = createActivityLog(user, successful);

        if (context != null) {
            log.setIpAddress(context.getClientIp());
            log.setSessionId(context.getSessionId());
            log.setUserAgent(context.getUserAgent());
        }

        return log;
    }
}
package be.steby.CoreProject.dl.enums.action_log_type;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;

/**
 * Sealed interface for all activity log action types - KISS Version
 * Only essential methods, no over-engineering.
 */
public sealed interface ActionLogType
        permits AuthAction, PasswordAction {
    // TODO: Add other domain enums: PasswordAction, EmailAction, AccountAction, AdminAction, DeviceAction

    // ================== CORE METHODS ONLY ==================

    /**
     * Human-readable description of the action
     */
    String getDescription();

    /**
     * Domain category (AUTH, PASSWORD, EMAIL, ACCOUNT, ADMIN, DEVICE)
     */
    String getCategory();

    /**
     * Get the enum name (for database storage)
     */
    default String getName() {
        return ((Enum<?>) this).name();
    }

    /**
     * Simple factory method to create ActivityLog
     */
    default ActivityLog createActivityLog(User user, boolean successful) {
        return ActivityLog.builderWithTimestamp()
                .user(user)
                .actionType(this.getName())
                .actionCategory(this.getCategory())
                .successful(successful)
                .build();
    }
}
package be.steby.CoreProject.dl.enums.action_log_type;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;

/**
 * Sealed interface for all activity log action types - KISS Version
 * Only essential methods, no over-engineering.
 */
public sealed interface ActionLogType
        permits AuthAction, SecurityAction {

    /**
     * Get the enum name (for database storage) - e.g. "LOGIN", "LOGOUT"
     */
    String getName();

    /**
     * Human-readable description of the action
     */
    String getDescription();

    /**
     * Domain category - e.g. "AUTH", "PASSWORD", "EMAIL", "ACCOUNT", "ADMIN", "DEVICE"
     */
    String getCategory();

    /**
     * Simple factory method to create ActivityLog with basic fields
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

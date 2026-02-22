package be.steby.CoreProject.dl.enums.action_log_type;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;

/**
 * Sealed interface for all activity log action types.
 *
 * <p>Each permitted type corresponds to one business domain:
 * <ul>
 *   <li>{@link AuthAction}     — authentication (login, logout, 2FA, locking)</li>
 *   <li>{@link SecurityAction} — security events (brute-force, IP blocks)</li>
 *   <li>{@link AccountAction}  — account lifecycle (signup, activation, deactivation, reactivation)</li>
 *   <li>{@link DeviceAction}   — device management (confirmation, trust level, disconnection)</li>
 *   <li>{@link PasswordAction} — password changes and resets</li>
 *   <li>{@link AdminAction}    — admin operations (user management, roles, security, audit)</li>
 * </ul>
 *
 * <p>Future domains to add: {@code EmailAction}.
 */
public sealed interface ActionLogType
        permits AuthAction, SecurityAction, AccountAction, DeviceAction, PasswordAction, AdminAction {

    /**
     * Enum constant name used for database storage — e.g. "LOGIN", "PASSWORD_CHANGED".
     */
    String getName();

    /**
     * Human-readable description of the action.
     */
    String getDescription();

    /**
     * Domain category stored in {@code action_category} column
     * — e.g. "AUTH", "SECURITY", "PASSWORD", "ADMIN_USER".
     */
    String getCategory();

    /**
     * Factory helper to build an {@link ActivityLog} with the minimum required fields.
     * Domain-specific services may enrich the entry (failureReason, actionDetails, device…)
     * before persisting.
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
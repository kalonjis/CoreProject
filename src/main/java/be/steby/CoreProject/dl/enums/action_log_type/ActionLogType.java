package be.steby.CoreProject.dl.enums.action_log_type;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;

/**
 * Sealed interface for all activity log action types.
 *
 * <p>Each permitted type corresponds to one business domain:
 * <ul>
 *   <li>{@link AuthAction}    — authentication (login, logout, 2FA, locking)</li>
 *   <li>{@link SecurityAction} — security events (brute-force, IP blocks)</li>
 *   <li>{@link AccountAction} — account lifecycle (signup, activation, deactivation, reactivation)</li>
 * </ul>
 *
 * <p>Future domains to add: {@code PasswordAction}, {@code DeviceAction}, {@code EmailAction}.
 */
public sealed interface ActionLogType
        permits AuthAction, SecurityAction, AccountAction {

    /**
     * Enum constant name used for database storage — e.g. "LOGIN", "ACCOUNT_DEACTIVATED".
     */
    String getName();

    /**
     * Human-readable description of the action.
     */
    String getDescription();

    /**
     * Domain category stored in {@code action_category} column
     * — e.g. "AUTH", "SECURITY", "ACCOUNT".
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
package be.steby.CoreProject.dl.enums.actionLogTypes;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.bll.common.models.RequestContext;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Sealed interface for all activity log action types.
 * Replaces the monolithic enum with domain-specific enums.
 *
 * Complete action domains (focused on business logic):
 * - AuthAction (authentication domain) ✓
 * - AdminAction (administration domain) ✓
 * - AccountAction (account management domain) ✓
 * - SecurityAction (security domain) ✓
 * - EmailAction (email domain) ✓
 * - PasswordAction (password domain) ✓
 * - DeviceAction (device management domain) ✓
 */
public sealed interface ActionLogType
        permits AuthAction, AdminAction, AccountAction, SecurityAction,
        EmailAction, PasswordAction, DeviceAction {

    // ================== ABSTRACT METHODS (must be implemented by all enums) ==================

    /**
     * Human-readable description of the action
     */
    String getDescription();

    /**
     * Category/domain of the action (AUTH, ADMIN, ACCOUNT, SECURITY, etc.)
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
            case AdminAction admin -> admin.processAdminAction(log);
            case AccountAction account -> account.processAccountAction(log);
            case SecurityAction security -> security.processSecurityAction(log);
            case EmailAction email -> email.processEmailAction(log);
            case PasswordAction password -> password.processPasswordAction(log);
            case DeviceAction device -> device.processDeviceAction(log);
        };
    }

    /**
     * Central enrichment dispatcher - calls the appropriate domain-specific enrichment
     */
    default void enrichActivityLog(ActivityLog log) {
        switch (this) {
            case AuthAction auth -> auth.enrichAuthLog(log);
            case AdminAction admin -> admin.enrichAdminLog(log);
            case AccountAction account -> account.enrichAccountLog(log);
            case SecurityAction security -> security.enrichSecurityLog(log);
            case EmailAction email -> email.enrichEmailLog(log);
            case PasswordAction password -> password.enrichPasswordLog(log);
            case DeviceAction device -> device.enrichDeviceLog(log);
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
            case "ADMIN" -> AdminAction.valueOf(actionName);
            case "ACCOUNT" -> AccountAction.valueOf(actionName);
            case "SECURITY" -> SecurityAction.valueOf(actionName);
            case "EMAIL" -> EmailAction.valueOf(actionName);
            case "PASSWORD" -> PasswordAction.valueOf(actionName);
            case "DEVICE" -> DeviceAction.valueOf(actionName);
            default -> throw new IllegalArgumentException(
                    "Unknown action category: " + category + " for action: " + actionName);
        };
    }

    // ================== DOMAIN FILTERING METHODS ==================

    /**
     * Get all authentication actions
     */
    static List<ActionLogType> getAuthActions() {
        return List.of(AuthAction.values());
    }

    /**
     * Get all administrative actions
     */
    static List<ActionLogType> getAdminActions() {
        return List.of(AdminAction.values());
    }

    /**
     * Get all account management actions
     */
    static List<ActionLogType> getAccountActions() {
        return List.of(AccountAction.values());
    }

    /**
     * Get all security-related actions
     */
    static List<ActionLogType> getSecurityActions() {
        return List.of(SecurityAction.values());
    }

    /**
     * Get all email-related actions
     */
    static List<ActionLogType> getEmailActions() {
        return List.of(EmailAction.values());
    }

    /**
     * Get all password-related actions
     */
    static List<ActionLogType> getPasswordActions() {
        return List.of(PasswordAction.values());
    }

    /**
     * Get all device-related actions
     */
    static List<ActionLogType> getDeviceActions() {
        return List.of(DeviceAction.values());
    }

    /**
     * Get high-risk actions across all domains
     */
    static List<ActionLogType> getHighRiskActions() {
        return List.of(
                // Auth high-risk
                AuthAction.LOGIN_FAILED, AuthAction.ACCOUNT_LOCKED_ATTEMPTS,
                // Admin high-risk
                AdminAction.USER_DELETED, AdminAction.ROLE_GRANTED, AdminAction.ROLE_REVOKED,
                AdminAction.EMERGENCY_ACCESS_GRANTED, AdminAction.SECURITY_POLICY_UPDATED,
                // Account high-risk
                AccountAction.ACCOUNT_DELETED, AccountAction.TWO_FACTOR_DISABLED,
                AccountAction.DATA_DELETION_REQUESTED,
                // Security high-risk
                SecurityAction.DATA_BREACH_DETECTED, SecurityAction.PRIVILEGE_ESCALATION_ATTEMPT,
                SecurityAction.MALWARE_UPLOAD_ATTEMPT, SecurityAction.SYSTEM_FILE_MODIFICATION,
                // Email high-risk
                EmailAction.EMAIL_CHANGE_CONFIRMED, EmailAction.EMAIL_MARKED_SPAM,
                // Password high-risk
                PasswordAction.PASSWORD_COMPROMISED_DETECTED, PasswordAction.PASSWORD_LOCKOUT_TRIGGERED,
                PasswordAction.PASSWORD_RESET_COMPLETED, PasswordAction.MASTER_PASSWORD_CHANGED,
                // Device high-risk
                DeviceAction.SUSPICIOUS_DEVICE_DETECTED, DeviceAction.DEVICE_FINGERPRINT_CHANGED
        );
    }

    /**
     * Get actions that typically require immediate review
     */
    static List<ActionLogType> getReviewRequiredActions() {
        return List.of(
                // Admin critical actions
                AdminAction.USER_DELETED, AdminAction.EMERGENCY_ACCESS_GRANTED,
                AdminAction.DATABASE_MAINTENANCE, AdminAction.AUDIT_LOG_PURGED,
                // Security critical actions
                SecurityAction.DATA_BREACH_DETECTED, SecurityAction.PRIVILEGE_ESCALATION_ATTEMPT,
                SecurityAction.MALWARE_UPLOAD_ATTEMPT, SecurityAction.UNAUTHORIZED_ADMIN_ACCESS,
                // Account critical actions
                AccountAction.ACCOUNT_DELETED, AccountAction.DATA_DELETION_REQUESTED,
                // Email critical actions
                EmailAction.EMAIL_MARKED_SPAM, EmailAction.EMAIL_CHANGE_CONFIRMED,
                // Password critical actions
                PasswordAction.PASSWORD_COMPROMISED_DETECTED, PasswordAction.PASSWORD_BREACH_CHECK,
                PasswordAction.MASTER_PASSWORD_CHANGED,
                // Device critical actions
                DeviceAction.SUSPICIOUS_DEVICE_DETECTED, DeviceAction.DEVICE_FINGERPRINT_CHANGED
        );
    }

    /**
     * Get actions by risk level
     */
    static List<ActionLogType> getActionsByRiskLevel(int riskLevel) {
        return getAllActions().stream()
                .filter(action -> action.getDefaultRiskLevel() == riskLevel)
                .toList();
    }

    /**
     * Get all actions across all domains
     */
    static List<ActionLogType> getAllActions() {
        List<ActionLogType> allActions = new ArrayList<>();

        // Add each domain's actions explicitly
        allActions.addAll(Arrays.asList(AuthAction.values()));
        allActions.addAll(Arrays.asList(AdminAction.values()));
        allActions.addAll(Arrays.asList(AccountAction.values()));
        allActions.addAll(Arrays.asList(SecurityAction.values()));
        allActions.addAll(Arrays.asList(EmailAction.values()));
        allActions.addAll(Arrays.asList(PasswordAction.values()));
        allActions.addAll(Arrays.asList(DeviceAction.values()));

        return Collections.unmodifiableList(allActions);
    }
}
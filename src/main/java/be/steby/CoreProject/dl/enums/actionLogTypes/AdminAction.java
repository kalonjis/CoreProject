package be.steby.CoreProject.dl.enums.actionLogTypes;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;

/**
 * Administrative domain actions with specialized business logic.
 * Contains all administration-related actions and their specific behaviors.
 */
public enum AdminAction implements ActionLogType {

    // ================== USER MANAGEMENT ACTIONS ==================
    USER_CREATED("User account created by administrator"),
    USER_UPDATED("User account updated by administrator"),
    USER_DELETED("User account deleted by administrator"),
    USER_SUSPENDED("User account suspended by administrator"),
    USER_UNSUSPENDED("User account unsuspended by administrator"),
    USER_PASSWORD_RESET("User password reset by administrator"),
    USER_FORCED_LOGOUT("User forced to logout by administrator"),
    USER_PROFILE_VIEWED("User profile viewed by administrator"),
    USER_ACTIVITY_REVIEWED("User activity reviewed by administrator"),

    // ================== ROLE & PERMISSION MANAGEMENT ==================
    ROLE_GRANTED("Role granted to user by administrator"),
    ROLE_REVOKED("Role revoked from user by administrator"),
    PERMISSION_GRANTED("Permission granted to user by administrator"),
    PERMISSION_REVOKED("Permission revoked from user by administrator"),
    ROLE_CREATED("New role created by administrator"),
    ROLE_UPDATED("Role updated by administrator"),
    ROLE_DELETED("Role deleted by administrator"),

    // ================== SYSTEM ADMINISTRATION ==================
    SYSTEM_CONFIGURATION_UPDATED("System configuration updated"),
    MAINTENANCE_MODE_ENABLED("Maintenance mode enabled"),
    MAINTENANCE_MODE_DISABLED("Maintenance mode disabled"),
    BACKUP_INITIATED("System backup initiated"),
    BACKUP_COMPLETED("System backup completed"),
    BACKUP_FAILED("System backup failed"),
    DATABASE_MAINTENANCE("Database maintenance performed"),

    // ================== SECURITY ADMINISTRATION ==================
    SECURITY_POLICY_UPDATED("Security policy updated"),
    IP_WHITELIST_UPDATED("IP whitelist updated"),
    RATE_LIMIT_UPDATED("Rate limit configuration updated"),
    FIREWALL_RULE_ADDED("Firewall rule added"),
    FIREWALL_RULE_REMOVED("Firewall rule removed"),

    // ================== AUDIT & MONITORING ==================
    AUDIT_LOG_EXPORTED("Audit log exported"),
    AUDIT_LOG_PURGED("Old audit logs purged"),
    SYSTEM_HEALTH_CHECK("System health check performed"),
    PERFORMANCE_REPORT_GENERATED("Performance report generated"),
    ALERT_CONFIGURATION_UPDATED("Alert configuration updated"),

    // ================== CONTENT MANAGEMENT ==================
    CONTENT_MODERATED("Content moderated by administrator"),
    CONTENT_DELETED("Content deleted by administrator"),
    CONTENT_RESTORED("Content restored by administrator"),
    BULK_OPERATION_PERFORMED("Bulk operation performed"),

    // ================== API & INTEGRATION MANAGEMENT ==================
    API_KEY_GENERATED("API key generated for user"),
    API_KEY_REVOKED("API key revoked"),
    WEBHOOK_CONFIGURED("Webhook configuration updated"),
    INTEGRATION_ENABLED("Integration enabled"),
    INTEGRATION_DISABLED("Integration disabled"),

    // ================== EMERGENCY ACTIONS ==================
    EMERGENCY_ACCESS_GRANTED("Emergency access granted"),
    EMERGENCY_LOCKDOWN_INITIATED("Emergency system lockdown initiated"),
    EMERGENCY_LOCKDOWN_LIFTED("Emergency system lockdown lifted"),
    INCIDENT_RESPONSE_ACTIVATED("Incident response activated");

    private final String description;

    AdminAction(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return "ADMIN";
    }

    @Override
    public int getDefaultRiskLevel() {
        return switch (this) {
            // High risk actions
            case USER_DELETED, ROLE_GRANTED, ROLE_REVOKED, EMERGENCY_ACCESS_GRANTED,
                 EMERGENCY_LOCKDOWN_INITIATED, SECURITY_POLICY_UPDATED, DATABASE_MAINTENANCE -> 5;

            // Medium-high risk actions
            case USER_SUSPENDED, USER_PASSWORD_RESET, SYSTEM_CONFIGURATION_UPDATED,
                 MAINTENANCE_MODE_ENABLED, IP_WHITELIST_UPDATED, AUDIT_LOG_PURGED -> 4;

            // Medium risk actions
            case USER_CREATED, USER_UPDATED, PERMISSION_GRANTED, PERMISSION_REVOKED,
                 ROLE_CREATED, ROLE_UPDATED, API_KEY_GENERATED -> 3;

            // Low-medium risk actions
            case USER_UNSUSPENDED, USER_FORCED_LOGOUT, BACKUP_INITIATED,
                 CONTENT_MODERATED, API_KEY_REVOKED -> 2;

            // Low risk actions (monitoring, viewing, reporting)
            default -> 1;
        };
    }

    // ================== DOMAIN-SPECIFIC PROCESSING ==================

    /**
     * Administration-specific action processing with detailed logic
     */
    public String processAdminAction(ActivityLog log) {
        String adminName = log.getUser() != null ? log.getUser().getUsername() : "system";
        String details = log.getActionDetails() != null ? log.getActionDetails() : "";

        return switch (this) {
            // User Management
            case USER_CREATED -> log.isSuccessful() ?
                    "✅ User account created by admin: " + adminName :
                    "❌ Failed to create user account - admin: " + adminName;

            case USER_UPDATED -> log.isSuccessful() ?
                    "📝 User account updated by admin: " + adminName + " - " + details :
                    "❌ Failed to update user account - admin: " + adminName;

            case USER_DELETED -> log.isSuccessful() ?
                    "🗑️ User account deleted by admin: " + adminName + " - " + details :
                    "❌ Failed to delete user account - admin: " + adminName;

            case USER_SUSPENDED -> log.isSuccessful() ?
                    "🚫 User suspended by admin: " + adminName + " - " + details :
                    "❌ Failed to suspend user - admin: " + adminName;

            case USER_UNSUSPENDED -> log.isSuccessful() ?
                    "✅ User unsuspended by admin: " + adminName + " - " + details :
                    "❌ Failed to unsuspend user - admin: " + adminName;

            // Role Management
            case ROLE_GRANTED -> log.isSuccessful() ?
                    "👑 Role granted by admin: " + adminName + " - " + details :
                    "❌ Failed to grant role - admin: " + adminName;

            case ROLE_REVOKED -> log.isSuccessful() ?
                    "👑 Role revoked by admin: " + adminName + " - " + details :
                    "❌ Failed to revoke role - admin: " + adminName;

            // System Administration
            case SYSTEM_CONFIGURATION_UPDATED -> log.isSuccessful() ?
                    "⚙️ System configuration updated by admin: " + adminName :
                    "❌ Failed to update system configuration - admin: " + adminName;

            case MAINTENANCE_MODE_ENABLED -> log.isSuccessful() ?
                    "🔧 Maintenance mode enabled by admin: " + adminName :
                    "❌ Failed to enable maintenance mode - admin: " + adminName;

            case MAINTENANCE_MODE_DISABLED -> log.isSuccessful() ?
                    "✅ Maintenance mode disabled by admin: " + adminName :
                    "❌ Failed to disable maintenance mode - admin: " + adminName;

            // Security
            case SECURITY_POLICY_UPDATED -> log.isSuccessful() ?
                    "🛡️ Security policy updated by admin: " + adminName :
                    "❌ Failed to update security policy - admin: " + adminName;

            case IP_WHITELIST_UPDATED -> log.isSuccessful() ?
                    "🔐 IP whitelist updated by admin: " + adminName + " - " + details :
                    "❌ Failed to update IP whitelist - admin: " + adminName;

            // Emergency Actions
            case EMERGENCY_ACCESS_GRANTED -> log.isSuccessful() ?
                    "🚨 Emergency access granted by admin: " + adminName + " - " + details :
                    "❌ Failed to grant emergency access - admin: " + adminName;

            case EMERGENCY_LOCKDOWN_INITIATED -> log.isSuccessful() ?
                    "🔒 Emergency lockdown initiated by admin: " + adminName :
                    "❌ Failed to initiate emergency lockdown - admin: " + adminName;

            // Default for other actions
            default -> log.isSuccessful() ?
                    "✅ " + this.getDescription() + " by admin: " + adminName :
                    "❌ Failed: " + this.getDescription() + " - admin: " + adminName;
        };
    }

    /**
     * Administration-specific log enrichment
     */
    public void enrichAdminLog(ActivityLog log) {
        // Set higher risk level for admin actions
        log.setRiskLevel(this.getDefaultRiskLevel());

        // Mark as triggering alert for high-risk actions
        if (this.getDefaultRiskLevel() >= 4) {
            log.setTriggeredAlert(true);
        }

        // Set compliance category for admin actions
        log.setComplianceCategory("ADMIN");

        // Add admin-specific metadata if not already present
        if (log.getMetadata() == null || log.getMetadata().isEmpty()) {
            log.setMetadata("{}");
        }
    }
}
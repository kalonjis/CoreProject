package be.steby.CoreProject.dl.enums.actionLogTypes;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;

/**
 * Administrative domain actions - COMPLETE enum aligned with ActivityListeners.
 * Contains all administration-related actions used in listeners and services.
 */
public enum AdminAction implements ActionLogType {

    // ================== USER MANAGEMENT ACTIONS ==================
    USER_CREATED("User account created by administrator"),
    ADMIN_USER_CREATED("User account created by administrator"), // Used in AdminActivityLogService
    USER_UPDATED("User account updated by administrator"),
    USER_DELETED("User account deleted by administrator"),
    ADMIN_USER_DELETION("User account deleted by administrator"), // Used in AdminActivityLogService
    USER_SUSPENDED("User account suspended by administrator"),
    USER_UNSUSPENDED("User account unsuspended by administrator"),
    USER_PASSWORD_RESET("User password reset by administrator"),
    ADMIN_PASSWORD_RESET("User password reset by administrator"), // Used in AdminActivityLogService
    USER_FORCED_LOGOUT("User forced to logout by administrator"),
    USER_PROFILE_VIEWED("User profile viewed by administrator"),
    USER_ACTIVITY_REVIEWED("User activity reviewed by administrator"),
    ADMIN_USER_SEARCH("User search performed by administrator"), // Used in AdminActivityLogService

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

    // ================== AUDIT & SECURITY ==================
    ADMIN_AUDIT_ACCESS("Administrative audit access"), // Used in AdminActivityLogService
    ADMIN_DATA_EXPORT("Administrative data export"), // Used in AdminActivityLogService
    SECURITY_POLICY_UPDATED("Security policy updated"),
    EMERGENCY_ACCESS_GRANTED("Emergency access granted"),
    EMERGENCY_LOCKDOWN_INITIATED("Emergency lockdown initiated"),

    // ================== MONITORING & LOGGING ==================
    ADMIN_LOGIN("Administrator login"),
    ADMIN_LOGOUT("Administrator logout"),
    ADMIN_SESSION_EXPIRED("Administrator session expired"),
    AUDIT_LOG_ACCESSED("Audit log accessed"),
    AUDIT_LOG_EXPORTED("Audit log exported"),
    AUDIT_LOG_PURGED("Audit log purged"),

    // ================== API & SYSTEM MANAGEMENT ==================
    API_KEY_GENERATED("API key generated"),
    API_KEY_REVOKED("API key revoked"),
    IP_WHITELIST_UPDATED("IP whitelist updated"),
    SYSTEM_CONFIG_CHANGED("System configuration changed"), // Used in AdminActivityLogService
    API_ACCESS("API access logged"), // Used in AdminActivityLogService

    // ================== CONTENT & USER MODERATION ==================
    CONTENT_MODERATED("Content moderated by administrator"),
    USER_CONTENT_DELETED("User content deleted by administrator"),
    MASS_ACTION_PERFORMED("Mass action performed"),

    // ================== INCIDENT RESPONSE ==================
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
            // Critical risk actions (5)
            case USER_DELETED, ADMIN_USER_DELETION, ROLE_GRANTED, ROLE_REVOKED,
                 EMERGENCY_ACCESS_GRANTED, EMERGENCY_LOCKDOWN_INITIATED,
                 SECURITY_POLICY_UPDATED, DATABASE_MAINTENANCE -> 5;

            // High risk actions (4)
            case USER_SUSPENDED, USER_PASSWORD_RESET, ADMIN_PASSWORD_RESET,
                 SYSTEM_CONFIGURATION_UPDATED, MAINTENANCE_MODE_ENABLED,
                 IP_WHITELIST_UPDATED, AUDIT_LOG_PURGED, ADMIN_AUDIT_ACCESS -> 4;

            // Medium risk actions (3)
            case USER_CREATED, ADMIN_USER_CREATED, USER_UPDATED, PERMISSION_GRANTED,
                 PERMISSION_REVOKED, ROLE_CREATED, ROLE_UPDATED, API_KEY_GENERATED,
                 ADMIN_DATA_EXPORT, MASS_ACTION_PERFORMED -> 3;

            // Low-medium risk actions (2)
            case USER_UNSUSPENDED, USER_FORCED_LOGOUT, BACKUP_INITIATED,
                 CONTENT_MODERATED, API_KEY_REVOKED, ADMIN_USER_SEARCH -> 2;

            // Low risk actions (1)
            default -> 1;
        };
    }

    public String processAdminAction(ActivityLog log) {
        String adminName = log.getUser() != null ? log.getUser().getUsername() : "system";
        String details = log.getActionDetails() != null ? log.getActionDetails() : "";

        return switch (this) {
            // User Management
            case USER_CREATED, ADMIN_USER_CREATED -> log.isSuccessful() ?
                    "✅ User account created by admin: " + adminName + " - " + details :
                    "❌ Failed to create user account - admin: " + adminName;

            case USER_DELETED, ADMIN_USER_DELETION -> log.isSuccessful() ?
                    "🗑️ User account deleted by admin: " + adminName + " - " + details :
                    "❌ Failed to delete user account - admin: " + adminName;

            case USER_PASSWORD_RESET, ADMIN_PASSWORD_RESET -> log.isSuccessful() ?
                    "🔐 Password reset by admin: " + adminName + " - " + details :
                    "❌ Failed to reset password - admin: " + adminName;

            case ADMIN_USER_SEARCH -> log.isSuccessful() ?
                    "🔍 User search performed by admin: " + adminName + " - " + details :
                    "❌ User search failed - admin: " + adminName;

            // Role Management
            case ROLE_GRANTED -> log.isSuccessful() ?
                    "✅ Role granted by admin: " + adminName + " - " + details :
                    "❌ Failed to grant role - admin: " + adminName;

            case ROLE_REVOKED -> log.isSuccessful() ?
                    "❌ Role revoked by admin: " + adminName + " - " + details :
                    "❌ Failed to revoke role - admin: " + adminName;

            // Audit & Security
            case ADMIN_AUDIT_ACCESS -> log.isSuccessful() ?
                    "🔍 Audit access by admin: " + adminName :
                    "❌ Audit access failed - admin: " + adminName;

            case ADMIN_DATA_EXPORT -> log.isSuccessful() ?
                    "📤 Data export by admin: " + adminName + " - " + details :
                    "❌ Data export failed - admin: " + adminName;

            case SECURITY_POLICY_UPDATED -> log.isSuccessful() ?
                    "🛡️ Security policy updated by admin: " + adminName :
                    "❌ Failed to update security policy - admin: " + adminName;

            // System Management
            case SYSTEM_CONFIG_CHANGED -> log.isSuccessful() ?
                    "⚙️ System configuration changed by admin: " + adminName + " - " + details :
                    "❌ Failed to change system config - admin: " + adminName;

            case API_ACCESS -> "🔌 API access logged for admin: " + adminName;

            // Default for other actions
            default -> log.isSuccessful() ?
                    "⚙️ " + this.getDescription() + " by admin: " + adminName :
                    "❌ Failed: " + this.getDescription() + " by admin: " + adminName;
        };
    }

    public void enrichAdminLog(ActivityLog log) {
        log.setRiskLevel(this.getDefaultRiskLevel());
        log.setComplianceCategory("ADMIN");

        // High-risk admin actions trigger alerts
        if (this.getDefaultRiskLevel() >= 4) {
            log.setTriggeredAlert(true);
        }

        // Critical admin actions need immediate review
        if (this.getDefaultRiskLevel() == 5) {
            log.setMetadata("{\"requiresReview\":true,\"adminCritical\":true}");
        }

        // Add admin-specific metadata if not already present
        if (log.getMetadata() == null || log.getMetadata().isEmpty()) {
            log.setMetadata("{}");
        }
    }
}
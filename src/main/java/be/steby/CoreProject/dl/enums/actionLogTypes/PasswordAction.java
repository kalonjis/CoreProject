package be.steby.CoreProject.dl.enums.actionLogTypes;

import be.steby.CoreProject.dl.entities.ActivityLog;

/**
 * Password domain actions - COMPLETE enum aligned with ActivityListeners.
 * Contains all password-related actions used in listeners and services.
 */
public enum PasswordAction implements ActionLogType {

    // ================== PASSWORD LIFECYCLE ==================
    PASSWORD_CREATED("Password created"),
    PASSWORD_UPDATED("Password updated"),
    PASSWORD_CHANGED("Password changed by user"),
    PASSWORD_EXPIRED("Password expired"),
    PASSWORD_FORCE_EXPIRED("Password force expired by admin"),

    // ================== PASSWORD RESET PROCESS ==================
    PASSWORD_RESET_REQUESTED("Password reset requested"),
    PASSWORD_RESET_TOKEN_REQUESTED("Password reset token requested"), // Used by PasswordActivityLogListener
    PASSWORD_RESET_COMPLETED("Password reset completed"),
    PASSWORD_RESET_EXPIRED("Password reset token expired"),
    PASSWORD_RESET_CANCELLED("Password reset cancelled"),
    PASSWORD_RESET_FAILED("Password reset failed"),
    TEMPORARY_PASSWORD_ISSUED("Temporary password issued"),
    TEMPORARY_PASSWORD_USED("Temporary password used"),

    // ================== PASSWORD VALIDATION ==================
    PASSWORD_STRENGTH_CHECKED("Password strength checked"),
    PASSWORD_HISTORY_VIOLATION("Password reuse detected"),
    PASSWORD_COMPLEXITY_FAILED("Password complexity requirements failed"),
    PASSWORD_TOO_WEAK("Password too weak"),
    PASSWORD_COMPROMISED_DETECTED("Compromised password detected"),
    PASSWORD_BREACH_CHECK("Password checked against breach database"),
    WEAK_PASSWORD_DETECTED("Weak password attempt detected"),
    PASSWORD_POLICY_VIOLATION("Password policy violation"),

    // ================== PASSWORD SECURITY ==================
    PASSWORD_LOCKOUT_TRIGGERED("Password lockout triggered"),
    PASSWORD_ATTEMPT_FAILED("Password attempt failed"),
    PASSWORD_RETRY_LIMIT_EXCEEDED("Password retry limit exceeded"),
    PASSWORD_HINT_REQUESTED("Password hint requested"),
    PASSWORD_RECOVERY_INITIATED("Password recovery initiated"),

    // ================== PASSWORD POLICY ==================
    PASSWORD_POLICY_UPDATED("Password policy updated"),
    PASSWORD_EXPIRY_WARNING_SENT("Password expiry warning sent"),
    PASSWORD_ROTATION_REQUIRED("Password rotation required"),
    MASTER_PASSWORD_CHANGED("Master password changed"),

    // ================== PASSWORD ADMIN ACTIONS ==================
    PASSWORD_ADMIN_RESET("Password reset by administrator"),
    PASSWORD_ADMIN_UNLOCK("Password unlock by administrator"),
    PASSWORD_ADMIN_EXPIRE("Password expired by administrator");

    private final String description;

    PasswordAction(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return "PASSWORD";
    }

    @Override
    public int getDefaultRiskLevel() {
        return switch (this) {
            // Critical security risks (5)
            case PASSWORD_COMPROMISED_DETECTED, PASSWORD_BREACH_CHECK,
                 PASSWORD_ADMIN_RESET, MASTER_PASSWORD_CHANGED -> 5;

            // High security risks (4)
            case PASSWORD_LOCKOUT_TRIGGERED, PASSWORD_HISTORY_VIOLATION,
                 PASSWORD_RESET_COMPLETED, PASSWORD_RETRY_LIMIT_EXCEEDED -> 4;

            // Medium risks (3)
            case PASSWORD_CHANGED, PASSWORD_UPDATED, TEMPORARY_PASSWORD_ISSUED,
                 PASSWORD_RESET_REQUESTED, PASSWORD_RESET_TOKEN_REQUESTED,
                 PASSWORD_FORCE_EXPIRED -> 3;

            // Low-medium risks (2)
            case PASSWORD_ATTEMPT_FAILED, PASSWORD_COMPLEXITY_FAILED,
                 WEAK_PASSWORD_DETECTED, PASSWORD_POLICY_VIOLATION -> 2;

            // Low risks (1)
            default -> 1;
        };
    }

    public String processPasswordAction(ActivityLog log) {
        String userName = log.getUser() != null ? log.getUser().getUsername() : "unknown";
        String details = log.getActionDetails() != null ? log.getActionDetails() : "";

        return switch (this) {
            // Password Lifecycle
            case PASSWORD_UPDATED -> log.isSuccessful() ?
                    "🔑 Password updated for user: " + userName :
                    "❌ Failed to update password for user: " + userName;

            case PASSWORD_CHANGED -> log.isSuccessful() ?
                    "🔑 Password changed by user: " + userName :
                    "❌ Failed to change password for user: " + userName;

            case PASSWORD_EXPIRED -> "⏰ Password expired for user: " + userName;

            // Password Reset Process
            case PASSWORD_RESET_REQUESTED -> log.isSuccessful() ?
                    "🔄 Password reset requested for user: " + userName :
                    "❌ Failed to request password reset for user: " + userName;

            case PASSWORD_RESET_TOKEN_REQUESTED -> log.isSuccessful() ?
                    "🎫 Password reset token requested for user: " + userName :
                    "❌ Failed to generate password reset token for user: " + userName;

            case PASSWORD_RESET_COMPLETED -> log.isSuccessful() ?
                    "✅ Password reset completed for user: " + userName :
                    "❌ Password reset failed for user: " + userName;

            case TEMPORARY_PASSWORD_ISSUED -> log.isSuccessful() ?
                    "🔄 Temporary password issued to user: " + userName :
                    "❌ Failed to issue temporary password to user: " + userName;

            // Password Validation
            case PASSWORD_COMPROMISED_DETECTED -> "🚨 Compromised password detected for user: " + userName;

            case PASSWORD_HISTORY_VIOLATION -> "⚠️ Password reuse attempt by user: " + userName;

            case PASSWORD_COMPLEXITY_FAILED -> "❌ Password complexity failed for user: " + userName + " - " + details;

            case WEAK_PASSWORD_DETECTED -> "⚠️ Weak password attempt detected for user: " + userName;

            case PASSWORD_BREACH_CHECK -> log.isSuccessful() ?
                    "🔍 Password breach check passed for user: " + userName :
                    "🚨 Password found in breach database for user: " + userName;

            // Password Security
            case PASSWORD_LOCKOUT_TRIGGERED -> "🔒 Password lockout triggered for user: " + userName + " - " + details;

            case PASSWORD_ATTEMPT_FAILED -> "❌ Password attempt failed for user: " + userName;

            case PASSWORD_RETRY_LIMIT_EXCEEDED -> "🚫 Password retry limit exceeded for user: " + userName;

            // Password Policy
            case PASSWORD_POLICY_UPDATED -> log.isSuccessful() ?
                    "📋 Password policy updated - " + details :
                    "❌ Failed to update password policy";

            case PASSWORD_EXPIRY_WARNING_SENT -> "⚠️ Password expiry warning sent to user: " + userName;

            // Admin Actions
            case PASSWORD_ADMIN_RESET -> log.isSuccessful() ?
                    "👨‍💼 Password reset by administrator for user: " + userName :
                    "❌ Failed admin password reset for user: " + userName;

            case MASTER_PASSWORD_CHANGED -> log.isSuccessful() ?
                    "🔐 Master password changed for user: " + userName :
                    "❌ Failed to change master password for user: " + userName;

            // Default for other actions
            default -> log.isSuccessful() ?
                    "🔐 " + this.getDescription() + " for user: " + userName :
                    "❌ Failed: " + this.getDescription() + " for user: " + userName;
        };
    }

    public void enrichPasswordLog(ActivityLog log) {
        log.setRiskLevel(this.getDefaultRiskLevel());
        log.setComplianceCategory("PASSWORD");

        // Never log actual passwords in metadata - security rule
        log.setMetadata("{\"containsActualPassword\":false}");

        // High-risk password events trigger alerts
        if (this.getDefaultRiskLevel() >= 4) {
            log.setTriggeredAlert(true);
        }

        // Critical security events need immediate attention
        if (this.getDefaultRiskLevel() == 5) {
            log.setErrorDetails("CRITICAL PASSWORD SECURITY EVENT - Review immediately");
        }
    }
}
package be.steby.CoreProject.dl.enums.actionLogTypes;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;

/**
 * Account management domain actions with specialized business logic.
 * Contains all user account-related actions and their specific behaviors.
 */
public enum AccountAction implements ActionLogType {

    // ================== ACCOUNT LIFECYCLE ==================
    ACCOUNT_CREATED("User account created"),
    ACCOUNT_ACTIVATED("User account activated"),
    ACCOUNT_DEACTIVATED("User account deactivated"),
    ACCOUNT_SUSPENDED("User account suspended"),
    ACCOUNT_LOCKED("User account locked"),
    ACCOUNT_UNLOCKED("User account unlocked"),
    ACCOUNT_DELETED("User account marked for deletion"),
    ACCOUNT_RESTORED("User account restored from deletion"),

    // ================== PROFILE MANAGEMENT ==================
    PROFILE_UPDATED("User profile updated"),
    PROFILE_PICTURE_CHANGED("Profile picture changed"),
    PROFILE_PICTURE_REMOVED("Profile picture removed"),
    PERSONAL_INFO_UPDATED("Personal information updated"),
    CONTACT_INFO_UPDATED("Contact information updated"),
    PREFERENCES_UPDATED("User preferences updated"),
    LANGUAGE_CHANGED("User language preference changed"),
    TIMEZONE_CHANGED("User timezone changed"),



    // ================== SECURITY SETTINGS ==================
    TWO_FACTOR_ENABLED("Two-factor authentication enabled"),
    TWO_FACTOR_DISABLED("Two-factor authentication disabled"),
    RECOVERY_CODES_GENERATED("Recovery codes generated"),
    RECOVERY_CODE_USED("Recovery code used"),
    BACKUP_EMAIL_ADDED("Backup email address added"),
    BACKUP_EMAIL_REMOVED("Backup email address removed"),
    SECURITY_QUESTION_SET("Security question set"),
    SECURITY_QUESTION_UPDATED("Security question updated"),

    // ================== PRIVACY SETTINGS ==================
    PRIVACY_SETTINGS_UPDATED("Privacy settings updated"),
    DATA_EXPORT_REQUESTED("User data export requested"),
    DATA_EXPORT_COMPLETED("User data export completed"),
    DATA_DELETION_REQUESTED("User data deletion requested"),
    DATA_PORTABILITY_REQUESTED("Data portability requested"),
    CONSENT_UPDATED("User consent updated"),
    MARKETING_PREFERENCES_UPDATED("Marketing preferences updated"),

    // ================== NOTIFICATION SETTINGS ==================
    NOTIFICATION_SETTINGS_UPDATED("Notification settings updated"),
    EMAIL_NOTIFICATIONS_ENABLED("Email notifications enabled"),
    EMAIL_NOTIFICATIONS_DISABLED("Email notifications disabled"),
    SMS_NOTIFICATIONS_ENABLED("SMS notifications enabled"),
    SMS_NOTIFICATIONS_DISABLED("SMS notifications disabled"),
    PUSH_NOTIFICATIONS_ENABLED("Push notifications enabled"),
    PUSH_NOTIFICATIONS_DISABLED("Push notifications disabled"),

    // ================== SUBSCRIPTION & BILLING ==================
    SUBSCRIPTION_CREATED("Subscription created"),
    SUBSCRIPTION_UPDATED("Subscription updated"),
    SUBSCRIPTION_CANCELLED("Subscription cancelled"),
    SUBSCRIPTION_RENEWED("Subscription renewed"),
    SUBSCRIPTION_EXPIRED("Subscription expired"),
    PAYMENT_METHOD_ADDED("Payment method added"),
    PAYMENT_METHOD_UPDATED("Payment method updated"),
    PAYMENT_METHOD_REMOVED("Payment method removed"),
    BILLING_ADDRESS_UPDATED("Billing address updated"),

    // ================== DEVICE MANAGEMENT ==================
    DEVICE_REGISTERED("New device registered"),
    DEVICE_UNREGISTERED("Device unregistered"),
    DEVICE_TRUSTED("Device marked as trusted"),
    DEVICE_UNTRUSTED("Device marked as untrusted"),
    DEVICE_LIMIT_REACHED("Device limit reached"),

    // ================== ACCOUNT VERIFICATION ==================
    PHONE_VERIFICATION_REQUESTED("Phone verification requested"),
    PHONE_VERIFIED("Phone number verified"),
    PHONE_VERIFICATION_FAILED("Phone verification failed"),
    IDENTITY_VERIFICATION_REQUESTED("Identity verification requested"),
    IDENTITY_VERIFIED("Identity verification completed"),
    IDENTITY_VERIFICATION_FAILED("Identity verification failed"),

    // ================== ACCOUNT RECOVERY ==================
    ACCOUNT_RECOVERY_INITIATED("Account recovery process initiated"),
    ACCOUNT_RECOVERY_COMPLETED("Account recovery completed"),
    ACCOUNT_RECOVERY_FAILED("Account recovery failed"),
    BACKUP_RECOVERY_USED("Backup recovery method used");

    private final String description;

    AccountAction(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return "ACCOUNT";
    }

    @Override
    public int getDefaultRiskLevel() {
        return switch (this) {
            // High risk actions
            case ACCOUNT_DELETED, TWO_FACTOR_DISABLED, DATA_DELETION_REQUESTED -> 5;

            // Medium-high risk actions
            case ACCOUNT_DEACTIVATED, RECOVERY_CODE_USED, BACKUP_EMAIL_REMOVED -> 4;

            // Medium risk actions
            case PROFILE_UPDATED, PERSONAL_INFO_UPDATED, CONTACT_INFO_UPDATED,
                 TWO_FACTOR_ENABLED, SECURITY_QUESTION_SET, DEVICE_REGISTERED -> 3;

            // Low-medium risk actions
            case PREFERENCES_UPDATED, PRIVACY_SETTINGS_UPDATED, NOTIFICATION_SETTINGS_UPDATED,
                 SUBSCRIPTION_UPDATED, PAYMENT_METHOD_ADDED -> 2;

            // Low risk actions (viewing, minor updates)
            default -> 1;
        };
    }

    // ================== DOMAIN-SPECIFIC PROCESSING ==================

    /**
     * Account-specific action processing with detailed logic
     */
    public String processAccountAction(ActivityLog log) {
        String userName = log.getUser() != null ? log.getUser().getUsername() : "unknown";
        String details = log.getActionDetails() != null ? log.getActionDetails() : "";

        return switch (this) {
            // Account Lifecycle
            case ACCOUNT_CREATED -> log.isSuccessful() ?
                    "🎉 Account created for user: " + userName :
                    "❌ Failed to create account for user: " + userName;

            case ACCOUNT_ACTIVATED -> log.isSuccessful() ?
                    "✅ Account activated for user: " + userName :
                    "❌ Failed to activate account for user: " + userName;

            case ACCOUNT_DEACTIVATED -> log.isSuccessful() ?
                    "⏸️ Account deactivated for user: " + userName + " - " + details :
                    "❌ Failed to deactivate account for user: " + userName;

            case ACCOUNT_LOCKED -> log.isSuccessful() ?
                    "🔒 Account locked for user: " + userName + " - " + details :
                    "❌ Failed to lock account for user: " + userName;

            // Profile Management
            case PROFILE_UPDATED -> log.isSuccessful() ?
                    "📝 Profile updated for user: " + userName :
                    "❌ Failed to update profile for user: " + userName;

            case PROFILE_PICTURE_CHANGED -> log.isSuccessful() ?
                    "📸 Profile picture changed for user: " + userName :
                    "❌ Failed to change profile picture for user: " + userName;

            // Security Settings
            case TWO_FACTOR_ENABLED -> log.isSuccessful() ?
                    "🔐 Two-factor authentication enabled for user: " + userName :
                    "❌ Failed to enable 2FA for user: " + userName;

            case TWO_FACTOR_DISABLED -> log.isSuccessful() ?
                    "🔓 Two-factor authentication disabled for user: " + userName :
                    "❌ Failed to disable 2FA for user: " + userName;

            case RECOVERY_CODES_GENERATED -> log.isSuccessful() ?
                    "🔑 Recovery codes generated for user: " + userName :
                    "❌ Failed to generate recovery codes for user: " + userName;

            // Privacy & Data
            case DATA_EXPORT_REQUESTED -> log.isSuccessful() ?
                    "📦 Data export requested for user: " + userName :
                    "❌ Failed to request data export for user: " + userName;

            case DATA_DELETION_REQUESTED -> log.isSuccessful() ?
                    "🗑️ Data deletion requested for user: " + userName :
                    "❌ Failed to request data deletion for user: " + userName;

            // Subscriptions
            case SUBSCRIPTION_CREATED -> log.isSuccessful() ?
                    "💳 Subscription created for user: " + userName + " - " + details :
                    "❌ Failed to create subscription for user: " + userName;

            case SUBSCRIPTION_CANCELLED -> log.isSuccessful() ?
                    "❌ Subscription cancelled for user: " + userName :
                    "❌ Failed to cancel subscription for user: " + userName;

            // Device Management
            case DEVICE_REGISTERED -> log.isSuccessful() ?
                    "📱 New device registered for user: " + userName + " - " + details :
                    "❌ Failed to register device for user: " + userName;

            case DEVICE_TRUSTED -> log.isSuccessful() ?
                    "✅ Device marked as trusted for user: " + userName :
                    "❌ Failed to mark device as trusted for user: " + userName;

            // Default for other actions
            default -> log.isSuccessful() ?
                    "✅ " + this.getDescription() + " for user: " + userName :
                    "❌ Failed: " + this.getDescription() + " for user: " + userName;
        };
    }

    /**
     * Account-specific log enrichment
     */
    public void enrichAccountLog(ActivityLog log) {
        // Set risk level
        log.setRiskLevel(this.getDefaultRiskLevel());

        // Mark sensitive account changes for triggering alerts
        if (this.getDefaultRiskLevel() >= 4) {
            log.setTriggeredAlert(true);
        }

        // Set compliance category for account actions
        log.setComplianceCategory("ACCOUNT");

        // Mark PII-related actions
        switch (this) {
            case PERSONAL_INFO_UPDATED, CONTACT_INFO_UPDATED, IDENTITY_VERIFIED ->
                    log.setMetadata("{\"containsPii\":true}");
        }

        // Add account-specific metadata if not already present
        if (log.getMetadata() == null || log.getMetadata().isEmpty()) {
            log.setMetadata("{}");
        }
    }
}
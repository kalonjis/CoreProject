package be.steby.CoreProject.dl.enums.actionLogTypes;

import be.steby.CoreProject.dl.entities.ActivityLog;

/**
 * Email domain actions with specialized business logic.
 */
public enum EmailAction implements ActionLogType {

    // ================== EMAIL LIFECYCLE ==================
    EMAIL_SENT("Email sent"),
    EMAIL_DELIVERED("Email delivered"),
    EMAIL_BOUNCED("Email bounced"),
    EMAIL_FAILED("Email delivery failed"),
    EMAIL_QUEUED("Email queued for sending"),
    EMAIL_OPENED("Email opened"),
    EMAIL_CLICKED("Email link clicked"),
    EMAIL_REPLIED("Email replied to"),

    // ================== EMAIL MANAGEMENT ==================
    EMAIL_CHANGE_REQUESTED("Email change requested"),
    EMAIL_CHANGE_CONFIRMED("Email change confirmed"),
    EMAIL_CHANGE_CANCELLED("Email change cancelled"),
    EMAIL_VERIFICATION_SENT("Email verification sent"),
    EMAIL_VERIFIED("Email address verified"),
    EMAIL_VERIFICATION_FAILED("Email verification failed"),
    EMAIL_UNVERIFIED("Email marked as unverified"),
    EMAIL_VERIFICATION_EXPIRED("Email verification expired"),

    // ================== SUBSCRIPTION MANAGEMENT ==================
    EMAIL_UNSUBSCRIBED("User unsubscribed from emails"),
    EMAIL_RESUBSCRIBED("User resubscribed to emails"),
    EMAIL_MARKED_SPAM("Email marked as spam"),
    EMAIL_PREFERENCES_UPDATED("Email preferences updated"),
    NEWSLETTER_SUBSCRIBED("Newsletter subscription"),
    NEWSLETTER_UNSUBSCRIBED("Newsletter unsubscription"),
    MARKETING_EMAIL_SENT("Marketing email sent"),
    TRANSACTIONAL_EMAIL_SENT("Transactional email sent"),

    // ================== EMAIL TEMPLATES & CAMPAIGNS ==================
    EMAIL_TEMPLATE_CREATED("Email template created"),
    EMAIL_TEMPLATE_UPDATED("Email template updated"),
    EMAIL_TEMPLATE_DELETED("Email template deleted"),
    EMAIL_CAMPAIGN_CREATED("Email campaign created"),
    EMAIL_CAMPAIGN_SENT("Email campaign sent"),
    EMAIL_CAMPAIGN_CANCELLED("Email campaign cancelled"),
    BULK_EMAIL_SENT("Bulk email sent"),

    // ================== NOTIFICATIONS ==================
    NOTIFICATION_EMAIL_SENT("Notification email sent"),
    ALERT_EMAIL_SENT("Alert email sent"),
    REMINDER_EMAIL_SENT("Reminder email sent"),
    WELCOME_EMAIL_SENT("Welcome email sent"),
    CONFIRMATION_EMAIL_SENT("Confirmation email sent");

    private final String description;

    EmailAction(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return "EMAIL";
    }

    @Override
    public int getDefaultRiskLevel() {
        return switch (this) {
            // High risk actions
            case EMAIL_MARKED_SPAM, EMAIL_CHANGE_CONFIRMED -> 4;

            // Medium-high risk actions
            case EMAIL_CHANGE_REQUESTED, EMAIL_VERIFICATION_FAILED, EMAIL_BOUNCED -> 3;

            // Medium risk actions
            case BULK_EMAIL_SENT, EMAIL_CAMPAIGN_SENT, EMAIL_UNSUBSCRIBED -> 2;

            // Low risk actions (routine email operations)
            default -> 1;
        };
    }

    public String processEmailAction(ActivityLog log) {
        String userName = log.getUser() != null ? log.getUser().getUsername() : "system";
        String details = log.getActionDetails() != null ? log.getActionDetails() : "";

        return switch (this) {
            // Email Lifecycle
            case EMAIL_SENT -> log.isSuccessful() ?
                    "📧 Email sent to user: " + userName :
                    "❌ Failed to send email to user: " + userName;

            case EMAIL_DELIVERED -> "✅ Email delivered to user: " + userName;

            case EMAIL_BOUNCED -> "⚠️ Email bounced for user: " + userName + " - " + details;

            case EMAIL_OPENED -> "👁️ Email opened by user: " + userName + " - " + details;

            case EMAIL_CLICKED -> "🖱️ Email link clicked by user: " + userName + " - " + details;

            // Email Management
            case EMAIL_CHANGE_REQUESTED -> log.isSuccessful() ?
                    "📧 Email change requested for user: " + userName :
                    "❌ Failed to request email change for user: " + userName;

            case EMAIL_CHANGE_CONFIRMED -> log.isSuccessful() ?
                    "✅ Email change confirmed for user: " + userName + " - " + details :
                    "❌ Email change confirmation failed for user: " + userName;

            case EMAIL_VERIFIED -> log.isSuccessful() ?
                    "✅ Email verified for user: " + userName :
                    "❌ Email verification failed for user: " + userName;

            case EMAIL_VERIFICATION_SENT -> log.isSuccessful() ?
                    "📤 Email verification sent to user: " + userName :
                    "❌ Failed to send email verification to user: " + userName;

            // Subscription Management
            case EMAIL_MARKED_SPAM -> "🚫 Email marked as spam by user: " + userName;

            case EMAIL_UNSUBSCRIBED -> "👋 User unsubscribed: " + userName + " - " + details;

            case EMAIL_RESUBSCRIBED -> "📧 User resubscribed: " + userName + " - " + details;

            case NEWSLETTER_SUBSCRIBED -> "📰 Newsletter subscription for user: " + userName;

            // Campaign Management
            case EMAIL_CAMPAIGN_SENT -> log.isSuccessful() ?
                    "📢 Email campaign sent - " + details :
                    "❌ Email campaign failed - " + details;

            case BULK_EMAIL_SENT -> log.isSuccessful() ?
                    "📬 Bulk email sent - " + details :
                    "❌ Bulk email failed - " + details;

            // Notifications
            case WELCOME_EMAIL_SENT -> log.isSuccessful() ?
                    "🎉 Welcome email sent to user: " + userName :
                    "❌ Failed to send welcome email to user: " + userName;

            case CONFIRMATION_EMAIL_SENT -> log.isSuccessful() ?
                    "✉️ Confirmation email sent to user: " + userName :
                    "❌ Failed to send confirmation email to user: " + userName;

            // Default for other actions
            default -> log.isSuccessful() ?
                    "📨 " + this.getDescription() + " for user: " + userName :
                    "❌ Failed: " + this.getDescription() + " for user: " + userName;
        };
    }

    public void enrichEmailLog(ActivityLog log) {
        log.setRiskLevel(this.getDefaultRiskLevel());
        log.setComplianceCategory("EMAIL");

        // Mark spam reports for review
        if (this == EMAIL_MARKED_SPAM || this == EMAIL_BOUNCED) {
            log.setTriggeredAlert(true);
        }

        // Mark email change actions as containing PII
        switch (this) {
            case EMAIL_CHANGE_REQUESTED, EMAIL_CHANGE_CONFIRMED, EMAIL_VERIFIED ->
                    log.setMetadata("{\"containsPii\":true}");
        }

        // Add email-specific metadata if not already present
        if (log.getMetadata() == null || log.getMetadata().isEmpty()) {
            log.setMetadata("{}");
        }
    }
}
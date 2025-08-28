package be.steby.CoreProject.bll.domains.emailAddress.services;

import be.steby.CoreProject.bll.common.logs.AbstractActivityLogService;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.actionLogTypes.EmailAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Email management domain activity logging service.
 * PURE logging service - records email events, nothing more.
 *
 * Principle: "We log email events, analysts analyze them"
 *
 * @author Steby Core Project Team
 * @version 2.0 - Pure KISS Edition
 */
@Service
@Slf4j
public class EmailActivityLogService extends AbstractActivityLogService {

    // ================== CONSTRUCTOR ==================

    public EmailActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "EMAIL";
    }

    // ================== EMAIL ADDRESS MANAGEMENT LOGGING ==================

    /**
     * Logs email address change request
     */
    @Transactional
    public ActivityLog logEmailChangeRequested(User user, String oldEmail, String newEmail, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, EmailAction.EMAIL_CHANGE_REQUESTED, context);
        activityLog.setActionDetails("From: " + oldEmail + " To: " + newEmail);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs email address change confirmation
     */
    @Transactional
    public ActivityLog logEmailChangeConfirmed(User user, String newEmail, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, EmailAction.EMAIL_CHANGE_CONFIRMED, context);
        activityLog.setActionDetails("New email confirmed: " + newEmail);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs email address change cancellation
     */
    @Transactional
    public ActivityLog logEmailChangeCancelled(User user, String pendingEmail, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, EmailAction.EMAIL_CHANGE_CANCELLED, context);
        activityLog.setActionDetails("Cancelled change to: " + pendingEmail);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs email address change failure
     */
    @Transactional
    public ActivityLog logEmailChangeFailed(User user, String reason, Device device, RequestContext context) {
        return logFailure(user, device, EmailAction.EMAIL_CHANGE_FAILED, reason, context);
    }

    // ================== EMAIL VERIFICATION LOGGING ==================

    /**
     * Logs email verification request
     */
    @Transactional
    public ActivityLog logEmailVerificationRequested(User user, String email, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, EmailAction.EMAIL_VERIFICATION_REQUESTED, context);
        activityLog.setActionDetails("Verification requested for: " + email);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs email verification completion
     */
    @Transactional
    public ActivityLog logEmailVerificationCompleted(User user, String email, boolean successful, Device device, RequestContext context) {
        if (successful) {
            ActivityLog activityLog = logSuccess(user, device, EmailAction.EMAIL_VERIFICATION_COMPLETED, context);
            activityLog.setActionDetails("Email verified: " + email);
            return activityLogRepository.save(activityLog);
        } else {
            return logFailure(user, device, EmailAction.EMAIL_VERIFICATION_FAILED, "Verification failed for: " + email, context);
        }
    }

    /**
     * Logs email verification token expiration
     */
    @Transactional
    public ActivityLog logEmailVerificationExpired(User user, String email, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, EmailAction.EMAIL_VERIFICATION_EXPIRED, context);
        activityLog.setActionDetails("Verification expired for: " + email);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs email verification resend
     */
    @Transactional
    public ActivityLog logEmailVerificationResent(User user, String email, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, EmailAction.EMAIL_VERIFICATION_RESENT, context);
        activityLog.setActionDetails("Resent verification for: " + email);
        return activityLogRepository.save(activityLog);
    }

    // ================== EMAIL NOTIFICATIONS LOGGING ==================

    /**
     * Logs notification email sent
     */
    @Transactional
    public ActivityLog logNotificationSent(User user, String notificationType, String recipient, boolean successful, Device device, RequestContext context) {
        if (successful) {
            ActivityLog activityLog = logSuccess(user, device, EmailAction.NOTIFICATION_EMAIL_SENT, context);
            activityLog.setActionDetails("Type: " + notificationType + " To: " + recipient);
            return activityLogRepository.save(activityLog);
        } else {
            return logFailure(user, device, EmailAction.NOTIFICATION_EMAIL_FAILED,
                    "Failed to send " + notificationType + " to " + recipient, context);
        }
    }

    /**
     * Logs welcome email sent
     */
    @Transactional
    public ActivityLog logWelcomeEmailSent(User user, boolean successful, Device device, RequestContext context) {
        if (successful) {
            return logSuccess(user, device, EmailAction.WELCOME_EMAIL_SENT, context);
        } else {
            return logFailure(user, device, EmailAction.WELCOME_EMAIL_FAILED, "Welcome email delivery failed", context);
        }
    }

    /**
     * Logs password reset email sent
     */
    @Transactional
    public ActivityLog logPasswordResetEmailSent(User user, boolean successful, Device device, RequestContext context) {
        if (successful) {
            return logSuccess(user, device, EmailAction.PASSWORD_RESET_EMAIL_SENT, context);
        } else {
            return logFailure(user, device, EmailAction.PASSWORD_RESET_EMAIL_FAILED, "Password reset email delivery failed", context);
        }
    }

    /**
     * Logs security alert email sent
     */
    @Transactional
    public ActivityLog logSecurityAlertEmailSent(User user, String alertType, boolean successful, Device device, RequestContext context) {
        if (successful) {
            ActivityLog activityLog = logSuccess(user, device, EmailAction.SECURITY_ALERT_EMAIL_SENT, context);
            activityLog.setActionDetails("Alert type: " + alertType);
            return activityLogRepository.save(activityLog);
        } else {
            return logFailure(user, device, EmailAction.SECURITY_ALERT_EMAIL_FAILED,
                    "Security alert email failed: " + alertType, context);
        }
    }

    // ================== EMAIL PREFERENCES LOGGING ==================

    /**
     * Logs email preferences update
     */
    @Transactional
    public ActivityLog logEmailPreferencesUpdated(User user, String updatedPreferences, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, EmailAction.EMAIL_PREFERENCES_UPDATED, context);
        activityLog.setActionDetails("Updated preferences: " + updatedPreferences);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs subscription to email notifications
     */
    @Transactional
    public ActivityLog logEmailSubscription(User user, String subscriptionType, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, EmailAction.EMAIL_SUBSCRIBED, context);
        activityLog.setActionDetails("Subscribed to: " + subscriptionType);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs unsubscription from email notifications
     */
    @Transactional
    public ActivityLog logEmailUnsubscription(User user, String subscriptionType, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, EmailAction.EMAIL_UNSUBSCRIBED, context);
        activityLog.setActionDetails("Unsubscribed from: " + subscriptionType);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs email frequency change
     */
    @Transactional
    public ActivityLog logEmailFrequencyChanged(User user, String emailType, String oldFrequency, String newFrequency, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, EmailAction.EMAIL_FREQUENCY_CHANGED, context);
        activityLog.setActionDetails("Email: " + emailType + " Changed from " + oldFrequency + " to " + newFrequency);
        return activityLogRepository.save(activityLog);
    }

    // ================== EMAIL SECURITY LOGGING ==================

    /**
     * Logs suspicious email activity
     */
    @Transactional
    public ActivityLog logSuspiciousEmailActivity(User user, String activity, Device device, RequestContext context) {
        ActivityLog activityLog = logFailure(user, device, EmailAction.SUSPICIOUS_EMAIL_ACTIVITY, "Suspicious activity detected", context);
        activityLog.setActionDetails("Activity: " + activity);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs email marked as spam
     */
    @Transactional
    public ActivityLog logEmailMarkedSpam(User user, String emailType, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, EmailAction.EMAIL_MARKED_SPAM, context);
        activityLog.setActionDetails("Marked as spam: " + emailType);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs email delivery bounced
     */
    @Transactional
    public ActivityLog logEmailBounced(User user, String emailAddress, String bounceReason, Device device, RequestContext context) {
        ActivityLog activityLog = logFailure(user, device, EmailAction.EMAIL_DELIVERY_BOUNCED, bounceReason, context);
        activityLog.setActionDetails("Bounced email to: " + emailAddress);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs email blocked by provider
     */
    @Transactional
    public ActivityLog logEmailBlocked(User user, String provider, String reason, Device device, RequestContext context) {
        ActivityLog activityLog = logFailure(user, device, EmailAction.EMAIL_BLOCKED_BY_PROVIDER, reason, context);
        activityLog.setActionDetails("Blocked by: " + provider);
        return activityLogRepository.save(activityLog);
    }

    // ================== EMAIL TEMPLATE LOGGING ==================

    /**
     * Logs email template usage
     */
    @Transactional
    public ActivityLog logEmailTemplateUsed(User user, String templateName, String recipient, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, EmailAction.EMAIL_TEMPLATE_USED, context);
        activityLog.setActionDetails("Template: " + templateName + " Sent to: " + recipient);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs custom email sent
     */
    @Transactional
    public ActivityLog logCustomEmailSent(User user, String recipient, boolean successful, Device device, RequestContext context) {
        if (successful) {
            ActivityLog activityLog = logSuccess(user, device, EmailAction.CUSTOM_EMAIL_SENT, context);
            activityLog.setActionDetails("Sent to: " + recipient);
            return activityLogRepository.save(activityLog);
        } else {
            return logFailure(user, device, EmailAction.CUSTOM_EMAIL_FAILED, "Custom email failed to: " + recipient, context);
        }
    }
}
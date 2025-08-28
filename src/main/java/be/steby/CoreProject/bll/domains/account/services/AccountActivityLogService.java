package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.bll.common.logs.AbstractActivityLogService;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.actionLogTypes.AccountAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account management domain activity logging service.
 * PURE logging service - records account events, nothing more.
 *
 * Principle: "We log account events, analysts analyze them"
 *
 * @author Steby Core Project Team
 * @version 2.0 - Pure KISS Edition
 */
@Service
@Slf4j
public class AccountActivityLogService extends AbstractActivityLogService {

    // ================== CONSTRUCTOR ==================

    public AccountActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "ACCOUNT";
    }

    // ================== ACCOUNT LIFECYCLE LOGGING ==================

    /**
     * Logs account creation
     */
    @Transactional
    public ActivityLog logAccountCreated(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.ACCOUNT_CREATED, context);
    }

    /**
     * Logs account activation
     */
    @Transactional
    public ActivityLog logAccountActivated(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.ACCOUNT_ACTIVATED, context);
    }

    /**
     * Logs account deactivation
     */
    @Transactional
    public ActivityLog logAccountDeactivated(User user, String reason, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, AccountAction.ACCOUNT_DEACTIVATED, context);
        activityLog.setActionDetails("Deactivation reason: " + reason);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs account suspension
     */
    @Transactional
    public ActivityLog logAccountSuspended(User user, String reason, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, AccountAction.ACCOUNT_SUSPENDED, context);
        activityLog.setActionDetails("Suspension reason: " + reason);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs account locking
     */
    @Transactional
    public ActivityLog logAccountLocked(User user, String reason, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, AccountAction.ACCOUNT_LOCKED, context);
        activityLog.setActionDetails("Lock reason: " + reason);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs account unlocking
     */
    @Transactional
    public ActivityLog logAccountUnlocked(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.ACCOUNT_UNLOCKED, context);
    }

    /**
     * Logs account deletion
     */
    @Transactional
    public ActivityLog logAccountDeleted(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.ACCOUNT_DELETED, context);
    }

    /**
     * Logs account restoration
     */
    @Transactional
    public ActivityLog logAccountRestored(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.ACCOUNT_RESTORED, context);
    }

    // ================== PROFILE MANAGEMENT LOGGING ==================

    /**
     * Logs profile update
     */
    @Transactional
    public ActivityLog logProfileUpdated(User user, String updatedFields, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, AccountAction.PROFILE_UPDATED, context);
        activityLog.setActionDetails("Updated fields: " + updatedFields);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs profile picture change
     */
    @Transactional
    public ActivityLog logProfilePictureChanged(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.PROFILE_PICTURE_CHANGED, context);
    }

    /**
     * Logs profile picture removal
     */
    @Transactional
    public ActivityLog logProfilePictureRemoved(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.PROFILE_PICTURE_REMOVED, context);
    }

    /**
     * Logs personal information update
     */
    @Transactional
    public ActivityLog logPersonalInfoUpdated(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.PERSONAL_INFO_UPDATED, context);
    }

    /**
     * Logs contact information update
     */
    @Transactional
    public ActivityLog logContactInfoUpdated(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.CONTACT_INFO_UPDATED, context);
    }

    /**
     * Logs user preferences update
     */
    @Transactional
    public ActivityLog logPreferencesUpdated(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.PREFERENCES_UPDATED, context);
    }

    /**
     * Logs language preference change
     */
    @Transactional
    public ActivityLog logLanguageChanged(User user, String oldLanguage, String newLanguage, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, AccountAction.LANGUAGE_CHANGED, context);
        activityLog.setActionDetails("Changed from " + oldLanguage + " to " + newLanguage);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs timezone change
     */
    @Transactional
    public ActivityLog logTimezoneChanged(User user, String oldTimezone, String newTimezone, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, AccountAction.TIMEZONE_CHANGED, context);
        activityLog.setActionDetails("Changed from " + oldTimezone + " to " + newTimezone);
        return activityLogRepository.save(activityLog);
    }

    // ================== SECURITY SETTINGS LOGGING ==================

    /**
     * Logs two-factor authentication enabled
     */
    @Transactional
    public ActivityLog logTwoFactorEnabled(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.TWO_FACTOR_ENABLED, context);
    }

    /**
     * Logs two-factor authentication disabled
     */
    @Transactional
    public ActivityLog logTwoFactorDisabled(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.TWO_FACTOR_DISABLED, context);
    }

    /**
     * Logs recovery codes generation
     */
    @Transactional
    public ActivityLog logRecoveryCodesGenerated(User user, int numberOfCodes, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, AccountAction.RECOVERY_CODES_GENERATED, context);
        activityLog.setActionDetails("Generated " + numberOfCodes + " recovery codes");
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs recovery code usage
     */
    @Transactional
    public ActivityLog logRecoveryCodeUsed(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.RECOVERY_CODE_USED, context);
    }

    /**
     * Logs backup email addition
     */
    @Transactional
    public ActivityLog logBackupEmailAdded(User user, String backupEmail, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, AccountAction.BACKUP_EMAIL_ADDED, context);
        activityLog.setActionDetails("Added backup email: " + backupEmail);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs backup email removal
     */
    @Transactional
    public ActivityLog logBackupEmailRemoved(User user, String backupEmail, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(user, device, AccountAction.BACKUP_EMAIL_REMOVED, context);
        activityLog.setActionDetails("Removed backup email: " + backupEmail);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs security question setup
     */
    @Transactional
    public ActivityLog logSecurityQuestionSet(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.SECURITY_QUESTION_SET, context);
    }

    /**
     * Logs security question update
     */
    @Transactional
    public ActivityLog logSecurityQuestionUpdated(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.SECURITY_QUESTION_UPDATED, context);
    }

    // ================== PRIVACY SETTINGS LOGGING ==================

    /**
     * Logs privacy settings update
     */
    @Transactional
    public ActivityLog logPrivacySettingsUpdated(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.PRIVACY_SETTINGS_UPDATED, context);
    }

    /**
     * Logs data export request
     */
    @Transactional
    public ActivityLog logDataExportRequested(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.DATA_EXPORT_REQUESTED, context);
    }

    /**
     * Logs data export completion
     */
    @Transactional
    public ActivityLog logDataExportCompleted(User user, boolean successful, Device device, RequestContext context) {
        if (successful) {
            return logSuccess(user, device, AccountAction.DATA_EXPORT_COMPLETED, context);
        } else {
            return logFailure(user, device, AccountAction.DATA_EXPORT_COMPLETED, "Export failed", context);
        }
    }

    /**
     * Logs data deletion request
     */
    @Transactional
    public ActivityLog logDataDeletionRequested(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AccountAction.DATA_DELETION_REQUESTED, context);
    }
}
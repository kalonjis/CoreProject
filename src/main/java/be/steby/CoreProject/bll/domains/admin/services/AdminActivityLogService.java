package be.steby.CoreProject.bll.domains.admin.services;

import be.steby.CoreProject.bll.common.logs.AbstractActivityLogService;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.actionLogTypes.AdminAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administration domain activity logging service.
 * PURE logging service - records admin actions, nothing more.
 *
 * Principle: "We log admin events, analysts analyze them"
 *
 * @author Steby Core Project Team
 * @version 2.0 - Pure KISS Edition
 */
@Service
@Slf4j
public class AdminActivityLogService extends AbstractActivityLogService {

    // ================== CONSTRUCTOR ==================

    public AdminActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "ADMIN";
    }

    // ================== USER MANAGEMENT LOGGING ==================

    /**
     * Logs user creation by admin
     */
    @Transactional
    public ActivityLog logUserCreated(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.USER_CREATED, context);
        activityLog.setActionDetails("Created user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs user update by admin
     */
    @Transactional
    public ActivityLog logUserUpdated(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.USER_UPDATED, context);
        activityLog.setActionDetails("Updated user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs user deletion by admin
     */
    @Transactional
    public ActivityLog logUserDeleted(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.USER_DELETED, context);
        activityLog.setActionDetails("Deleted user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs user suspension by admin
     */
    @Transactional
    public ActivityLog logUserSuspended(User admin, User targetUser, String reason, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.USER_SUSPENDED, context);
        activityLog.setActionDetails("Suspended user: " + targetUser.getUsername() + " - Reason: " + reason);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs user unsuspension by admin
     */
    @Transactional
    public ActivityLog logUserUnsuspended(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.USER_UNSUSPENDED, context);
        activityLog.setActionDetails("Unsuspended user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs password reset by admin
     */
    @Transactional
    public ActivityLog logUserPasswordReset(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.USER_PASSWORD_RESET, context);
        activityLog.setActionDetails("Reset password for user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs forced logout by admin
     */
    @Transactional
    public ActivityLog logUserForcedLogout(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.USER_FORCED_LOGOUT, context);
        activityLog.setActionDetails("Forced logout for user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }

    // ================== ROLE & PERMISSION MANAGEMENT LOGGING ==================

    /**
     * Logs role granted to user
     */
    @Transactional
    public ActivityLog logRoleGranted(User admin, User targetUser, String roleName, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.ROLE_GRANTED, context);
        activityLog.setActionDetails("Granted role '" + roleName + "' to user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs role revoked from user
     */
    @Transactional
    public ActivityLog logRoleRevoked(User admin, User targetUser, String roleName, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.ROLE_REVOKED, context);
        activityLog.setActionDetails("Revoked role '" + roleName + "' from user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs permission granted to user
     */
    @Transactional
    public ActivityLog logPermissionGranted(User admin, User targetUser, String permission, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.PERMISSION_GRANTED, context);
        activityLog.setActionDetails("Granted permission '" + permission + "' to user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs permission revoked from user
     */
    @Transactional
    public ActivityLog logPermissionRevoked(User admin, User targetUser, String permission, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.PERMISSION_REVOKED, context);
        activityLog.setActionDetails("Revoked permission '" + permission + "' from user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs role creation
     */
    @Transactional
    public ActivityLog logRoleCreated(User admin, String roleName, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.ROLE_CREATED, context);
        activityLog.setActionDetails("Created role: " + roleName);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs role update
     */
    @Transactional
    public ActivityLog logRoleUpdated(User admin, String roleName, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.ROLE_UPDATED, context);
        activityLog.setActionDetails("Updated role: " + roleName);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs role deletion
     */
    @Transactional
    public ActivityLog logRoleDeleted(User admin, String roleName, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.ROLE_DELETED, context);
        activityLog.setActionDetails("Deleted role: " + roleName);
        return activityLogRepository.save(activityLog);
    }

    // ================== SYSTEM ADMINISTRATION LOGGING ==================

    /**
     * Logs system configuration update
     */
    @Transactional
    public ActivityLog logSystemConfigurationUpdated(User admin, String configKey, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.SYSTEM_CONFIGURATION_UPDATED, context);
        activityLog.setActionDetails("Updated system configuration: " + configKey);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs maintenance mode enabled
     */
    @Transactional
    public ActivityLog logMaintenanceModeEnabled(User admin, Device device, RequestContext context) {
        return logSuccess(admin, device, AdminAction.MAINTENANCE_MODE_ENABLED, context);
    }

    /**
     * Logs maintenance mode disabled
     */
    @Transactional
    public ActivityLog logMaintenanceModeDisabled(User admin, Device device, RequestContext context) {
        return logSuccess(admin, device, AdminAction.MAINTENANCE_MODE_DISABLED, context);
    }

    /**
     * Logs backup initiation
     */
    @Transactional
    public ActivityLog logBackupInitiated(User admin, String backupType, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.BACKUP_INITIATED, context);
        activityLog.setActionDetails("Backup type: " + backupType);
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs backup completion
     */
    @Transactional
    public ActivityLog logBackupCompleted(User admin, String backupType, boolean successful, Device device, RequestContext context) {
        if (successful) {
            ActivityLog activityLog = logSuccess(admin, device, AdminAction.BACKUP_COMPLETED, context);
            activityLog.setActionDetails("Backup completed successfully: " + backupType);
            return activityLogRepository.save(activityLog);
        } else {
            return logFailure(admin, device, AdminAction.BACKUP_FAILED, "Backup failed: " + backupType, context);
        }
    }

    // ================== AUDIT & SECURITY LOGGING ==================

    /**
     * Logs user profile viewing by admin
     */
    @Transactional
    public ActivityLog logUserProfileViewed(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.USER_PROFILE_VIEWED, context);
        activityLog.setActionDetails("Viewed profile of user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }

    /**
     * Logs user activity review by admin
     */
    @Transactional
    public ActivityLog logUserActivityReviewed(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog activityLog = logSuccess(admin, device, AdminAction.USER_ACTIVITY_REVIEWED, context);
        activityLog.setActionDetails("Reviewed activity of user: " + targetUser.getUsername());
        return activityLogRepository.save(activityLog);
    }
}
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
 * Administration domain activity logging service - KISS VERSION
 * Uses ONLY AdminAction enum values
 */
@Service
@Slf4j
public class AdminActivityLogService extends AbstractActivityLogService {

    public AdminActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "ADMIN";
    }

    // ================== ADMIN DOMAIN METHODS ONLY ==================

    @Transactional
    public ActivityLog logUserCreated(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog log = logSuccess(admin, device, AdminAction.USER_CREATED, context);
        log.setActionDetails("Created user: " + targetUser.getUsername());
        return activityLogRepository.save(log);
    }

    @Transactional
    public ActivityLog logUserUpdated(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog log = logSuccess(admin, device, AdminAction.USER_UPDATED, context);
        log.setActionDetails("Updated user: " + targetUser.getUsername());
        return activityLogRepository.save(log);
    }

    @Transactional
    public ActivityLog logUserDeleted(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog log = logSuccess(admin, device, AdminAction.USER_DELETED, context);
        log.setActionDetails("Deleted user: " + targetUser.getUsername());
        return activityLogRepository.save(log);
    }

    @Transactional
    public ActivityLog logUserSuspended(User admin, User targetUser, String reason, Device device, RequestContext context) {
        ActivityLog log = logSuccess(admin, device, AdminAction.USER_SUSPENDED, context);
        log.setActionDetails("Suspended user: " + targetUser.getUsername() + ". Reason: " + reason);
        return activityLogRepository.save(log);
    }

    @Transactional
    public ActivityLog logUserPasswordReset(User admin, User targetUser, Device device, RequestContext context) {
        ActivityLog log = logSuccess(admin, device, AdminAction.USER_PASSWORD_RESET, context);
        log.setActionDetails("Reset password for user: " + targetUser.getUsername());
        return activityLogRepository.save(log);
    }

    @Transactional
    public ActivityLog logRoleGranted(User admin, User targetUser, String role, Device device, RequestContext context) {
        ActivityLog log = logSuccess(admin, device, AdminAction.ROLE_GRANTED, context);
        log.setActionDetails("Granted role '" + role + "' to user: " + targetUser.getUsername());
        return activityLogRepository.save(log);
    }

    @Transactional
    public ActivityLog logRoleRevoked(User admin, User targetUser, String role, Device device, RequestContext context) {
        ActivityLog log = logSuccess(admin, device, AdminAction.ROLE_REVOKED, context);
        log.setActionDetails("Revoked role '" + role + "' from user: " + targetUser.getUsername());
        return activityLogRepository.save(log);
    }

    @Transactional
    public ActivityLog logEmergencyAccessGranted(User admin, String reason, Device device, RequestContext context) {
        ActivityLog log = logSuccess(admin, device, AdminAction.EMERGENCY_ACCESS_GRANTED, context);
        log.setActionDetails("Emergency access granted. Reason: " + reason);
        return activityLogRepository.save(log);
    }
}
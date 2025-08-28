package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.common.logs.AbstractActivityLogService;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.actionLogTypes.PasswordAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Password domain activity logging service - KISS VERSION
 * Uses ONLY PasswordAction enum values
 */
@Service
@Slf4j
public class PasswordActivityLogService extends AbstractActivityLogService {

    public PasswordActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "PASSWORD";
    }

    // ================== PASSWORD DOMAIN METHODS ONLY ==================

    @Transactional
    public ActivityLog logPasswordChanged(User user, Device device, RequestContext context) {
        return logSuccess(user, device, PasswordAction.PASSWORD_CHANGED, context);
    }

    @Transactional
    public ActivityLog logPasswordResetRequested(User user, Device device, RequestContext context) {
        return logSuccess(user, device, PasswordAction.PASSWORD_RESET_REQUESTED, context);
    }

    @Transactional
    public ActivityLog logPasswordResetCompleted(User user, boolean successful, String reason, Device device, RequestContext context) {
        return successful ?
                logSuccess(user, device, PasswordAction.PASSWORD_RESET_COMPLETED, context) :
                logFailure(user, device, PasswordAction.PASSWORD_RESET_COMPLETED, reason, context);
    }

    @Transactional
    public ActivityLog logPasswordCompromisedDetected(User user, String reason, Device device, RequestContext context) {
        return logFailure(user, device, PasswordAction.PASSWORD_COMPROMISED_DETECTED, reason, context);
    }

    @Transactional
    public ActivityLog logPasswordLockoutTriggered(User user, String reason, Device device, RequestContext context) {
        return logFailure(user, device, PasswordAction.PASSWORD_LOCKOUT_TRIGGERED, reason, context);
    }
}

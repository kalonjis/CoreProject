package be.steby.CoreProject.bll.domains.auth.services;

import be.steby.CoreProject.bll.common.logs.AbstractActivityLogService;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.actionLogTypes.AuthAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication domain activity logging service - KISS VERSION
 * Uses ONLY AuthAction enum values
 */
@Service
@Slf4j
public class AuthActivityLogService extends AbstractActivityLogService {

    public AuthActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "AUTH";
    }

    // ================== AUTH DOMAIN METHODS ONLY ==================

    @Transactional
    public ActivityLog logLogin(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AuthAction.LOGIN, context);
    }

    @Transactional
    public ActivityLog logLoginFailed(User user, Device device, String reason, RequestContext context) {
        return logFailure(user, device, AuthAction.LOGIN_FAILED, reason, context);
    }

    @Transactional
    public ActivityLog logLogout(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AuthAction.LOGOUT, context);
    }

    @Transactional
    public ActivityLog logTokenRefresh(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AuthAction.TOKEN_REFRESH, context);
    }

    @Transactional
    public ActivityLog logTwoFactorSetup(User user, Device device, RequestContext context) {
        return logSuccess(user, device, AuthAction.TWO_FA_SETUP, context);
    }

    @Transactional
    public ActivityLog logTwoFactorVerification(User user, Device device, boolean successful, RequestContext context) {
        return successful ?
                logSuccess(user, device, AuthAction.TWO_FA_VERIFICATION, context) :
                logFailure(user, device, AuthAction.TWO_FA_VERIFICATION, "2FA verification failed", context);
    }

    @Transactional
    public ActivityLog logAccountLocked(User user, String reason, Device device, RequestContext context) {
        return logFailure(user, device, AuthAction.ACCOUNT_LOCKED, reason, context);
    }
}

package be.steby.CoreProject.bll.domains.auth.services.activity_log;

import be.steby.CoreProject.bll.common.services.activitylog.ActivityLogService;
import be.steby.CoreProject.bll.domains.auth.events.UserLoggedInEvent;
import be.steby.CoreProject.bll.domains.auth.events.UserLoginFailedEvent;
import be.steby.CoreProject.bll.domains.auth.events.UserLogoutEvent;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.enums.action_log_type.AuthAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Auth domain specific activity log service - DDD principle
 * Handles authentication-related activity logging
 */
@Service
@Slf4j
public class AuthActivityLogService extends ActivityLogService {

    public AuthActivityLogService(ActivityLogRepository activityLogRepository) {
        super(activityLogRepository);
    }

    @Override
    protected String getDomainName() {
        return "AUTH";
    }

    /**
     * Log successful user login
     * @param event The successful login event
     */
    public void logSuccessfulLogin(UserLoggedInEvent event) {
        logUserActivity(event.user(), event.device(), AuthAction.LOGIN, true);
        log.debug("Successful login logged for user: {} from device: {}",
                event.user().getUsername(), event.device().getId());
    }

    /**
     * Log failed login for known user - Important for security
     * @param event The failed login event for an existing user
     */
    public void logFailedLogin(UserLoginFailedEvent event) {
        logUserActivity(event.user(), event.device(), AuthAction.LOGIN_FAILED, false, event.failureReason());
        log.debug("Failed login logged for known user: {} with reason: {}",
                event.user().getUsername(), event.failureReason());
    }

    /**
     * Log user logout activity from logout event
     * @param event The logout event containing user and device information
     */
    public void logUserLogout(UserLogoutEvent event) {
        logUserActivity(event.user(), event.device(), AuthAction.LOGOUT, true);
        log.debug("Logout logged for user: {} from device: {}",
                event.user().getUsername(), event.device() != null ? event.device().getId() : "unknown");
    }
}
package be.steby.CoreProject.bll.domains.auth.services;

import be.steby.CoreProject.bll.common.services.activitylog.ActivityLogService;
import be.steby.CoreProject.bll.domains.auth.events.UserLoggedInEvent;
import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
import be.steby.CoreProject.dl.entities.Device;
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
     * Log user login activity from login event
     * @param event The login event containing user and success information
     */
    public void logUserLogin(UserLoggedInEvent event) {
        if (event.successful()) {
            logUserActivity(event.user(), event.device(), AuthAction.LOGIN, true);
            log.debug("Successful login logged for user: {} from device: {}",
                    event.user().getUsername(), event.device() != null ? event.device().getId() : "unknown");
        } else {
            logUserActivity(event.user(), event.device(), AuthAction.LOGIN_FAILED, false, event.failureReason());
            log.debug("Failed login logged for user: {} with reason: {}",
                    event.user().getUsername(), event.failureReason());
        }
    }
}
package be.steby.CoreProject.bll.domains.auth.listeners;

import be.steby.CoreProject.bll.domains.auth.events.UserLoggedInEvent;
import be.steby.CoreProject.bll.domains.auth.events.UserLogoutEvent;
import be.steby.CoreProject.bll.domains.auth.services.AuthActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Auth domain listener - APPELS DIRECTS au service domaine
 * Principe KISS : Pas de couche intermédiaire !
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthActivityLogListener {

    // ✅ INJECTION DIRECTE du service domaine
    private final AuthActivityLogService authActivityLogService;

    @EventListener
    @Async("activityLogExecutor")
    public void handleLogin(UserLoggedInEvent event) {
        // ✅ APPEL DIRECT - Pas de délégation !
        if (event.successful()) {
            authActivityLogService.logLogin(event.user(), event.device(), event.requestContext());
        } else {
            authActivityLogService.logLoginFailed(event.user(), event.device(),
                    event.failureReason(), event.requestContext());
        }
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handleLogout(UserLogoutEvent event) {
        // ✅ APPEL DIRECT
        authActivityLogService.logLogout(event.user(), event.device(), event.requestContext());
    }
}

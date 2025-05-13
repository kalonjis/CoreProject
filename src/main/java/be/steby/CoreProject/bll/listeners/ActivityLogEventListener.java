package be.steby.CoreProject.bll.listeners;

import be.steby.CoreProject.bll.events.device.DeviceTrustLevelChangedEvent;
import be.steby.CoreProject.bll.events.security.UserLoggedInEvent;
import be.steby.CoreProject.bll.events.security.UserLogoutEvent;
import be.steby.CoreProject.bll.events.security.password_events.PasswordChangedEvent;
import be.steby.CoreProject.bll.events.security.password_events.RequestPasswordResetEvent;
import be.steby.CoreProject.bll.services.ActivityLogService;
import be.steby.CoreProject.bll.services.DeviceService;
import be.steby.CoreProject.dl.entities.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Order(10) // Priorité élevée pour la journalisation
@Slf4j
public class ActivityLogEventListener {
    private final ActivityLogService activityLogService;
    private final DeviceService deviceService;



    @EventListener
    @Async("activityLogExecutor")
    public void handleTrustLevelChange(DeviceTrustLevelChangedEvent event) {
        activityLogService.logDeviceTrustLevelChange(
                event.user(),
                event.device(),
                event.oldLevel(),
                event.newTrustLevel().name(),
                event.request()
        );
    }


    @EventListener
    @Async("activityLogExecutor")
    public  void handleLogin(UserLoggedInEvent event){
        activityLogService.logLogin(
                event.user(),
                event.device(),
                event.successful(),
                event.failureReason(),
                event.requestContext()
        );
    }


    @EventListener
    @Async("activityLogExecutor")
    public void handleLogout(UserLogoutEvent event){
        activityLogService.logLogout(
                event.user(),
                event.device(),
                event.requestContext()
        );

    }


    @EventListener
    @Async("activityLogExecutor")
    public void handleRequestPasswordReset(RequestPasswordResetEvent event) {
       activityLogService.logPasswordResetRequest(
               event.user(),
               event.device(),
               event.requestContext()
       );
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handlePasswordChangedEvent(PasswordChangedEvent event) {
        activityLogService.logPasswordChange(
                event.user(),
                event.device(),
                true,
                event.requestContext()
        );
    }


}

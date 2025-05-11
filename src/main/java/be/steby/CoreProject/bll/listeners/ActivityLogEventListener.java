package be.steby.CoreProject.bll.listeners;

import be.steby.CoreProject.bll.events.device.DeviceTrustLevelChangedEvent;
import be.steby.CoreProject.bll.events.security.UserLoggedInEvent;
import be.steby.CoreProject.bll.events.security.UserLogoutEvent;
import be.steby.CoreProject.bll.services.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Order(10) // Priorité élevée pour la journalisation
public class ActivityLogEventListener {
    private final ActivityLogService activityLogService;



    @EventListener
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
    public  void handleLogin(UserLoggedInEvent event){
        activityLogService.logLogin(
                event.user(),
                event.device(),
                event.successful(),
                event.failureReason(),
                event.request()
        );
    }


    @EventListener
    public void handleLogout(UserLogoutEvent event){
        activityLogService.logLogout(
                event.user(),
                event.device(),
                event.request()
        );

    }

    // Autres méthodes de journalisation pour différents types d'événements...
//    @EventListener
//    public void handleUserLogin(UserLoginEvent event) {
//        activityLogService.logLogin(/*...*/);
//    }
//
//    @EventListener
//    public void handleDeviceRegistration(DeviceRegisteredEvent event) {
//        activityLogService.logDeviceRegistration(/*...*/);
//    }
}

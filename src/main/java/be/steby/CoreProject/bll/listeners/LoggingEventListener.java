package be.steby.CoreProject.bll.listeners;

import be.steby.CoreProject.bll.events.DeviceTrustLevelChangedEvent;
import be.steby.CoreProject.bll.services.ConnectionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
@Order(10) // Priorité élevée pour la journalisation
public class LoggingEventListener {
    private final ConnectionLogService connectionLogService;



    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTrustLevelChange(DeviceTrustLevelChangedEvent event) {
        connectionLogService.logDeviceTrustLevelChange(
                event.user(),
                event.device(),
                event.oldLevel(),
                event.newTrustLevel().name(),
                event.request()
        );
    }

    // Autres méthodes de journalisation pour différents types d'événements...
//    @EventListener
//    public void handleUserLogin(UserLoginEvent event) {
//        connectionLogService.logLogin(/*...*/);
//    }
//
//    @EventListener
//    public void handleDeviceRegistration(DeviceRegisteredEvent event) {
//        connectionLogService.logDeviceRegistration(/*...*/);
//    }
}

package be.steby.CoreProject.bll.domains.password.listeners;


import be.steby.CoreProject.bll.common.utils.DeviceDetectionHelper;
import be.steby.CoreProject.bll.domains.password.events.PasswordChangedEvent;
import be.steby.CoreProject.bll.domains.password.events.RequestPasswordResetEvent;
import be.steby.CoreProject.bll.domains.password.events.RequestPasswordTokenEvent;
import be.steby.CoreProject.bll.domains.password.services.PasswordActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener pour enregistrer les événements dans les logs d'activité.
 * Utilise un executeur asynchrone dédié pour ne pas bloquer le thread principal.
 */
@RequiredArgsConstructor
@Component
@Order(10) // Priorité élevée pour la journalisation
@Slf4j
public class PasswordActivityLogListener {

    private final PasswordActivityLogService passwordActivityLogService;
    private final DeviceDetectionHelper deviceDetectionHelper;


    @EventListener
    @Async("activityLogExecutor")
    public void handleRequestPasswordReset(RequestPasswordResetEvent event) {
        deviceDetectionHelper.executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> passwordActivityLogService.logPasswordResetRequested(
                        event.user(), device, event.requestContext())
        );
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handlePasswordChangedEvent(PasswordChangedEvent event) {
        deviceDetectionHelper.executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> passwordActivityLogService.logPasswordChanged(
                        event.user(), device, event.requestContext())
        );
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handleRequestPasswordToken(RequestPasswordTokenEvent event) {
        deviceDetectionHelper.executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> passwordActivityLogService.logRequestPasswordToken(
                        event.user(), device, event.requestContext())
        );
    }

}
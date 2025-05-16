package be.steby.CoreProject.bll.domain.password.listeners;


import be.steby.CoreProject.bll.domain.password.events.PasswordChangedEvent;
import be.steby.CoreProject.bll.domain.password.events.RequestPasswordResetEvent;
import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.bll.services.ActivityLogService;
import be.steby.CoreProject.bll.services.DeviceService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Listener pour enregistrer les événements dans les logs d'activité.
 * Utilise un executeur asynchrone dédié pour ne pas bloquer le thread principal.
 */
@RequiredArgsConstructor
@Component
@Order(10) // Priorité élevée pour la journalisation
@Slf4j
public class PasswordActivityLogListener {

    private final ActivityLogService activityLogService;
    private final DeviceService deviceService;


    @EventListener
    @Async("activityLogExecutor")
    public void handleRequestPasswordReset(RequestPasswordResetEvent event) {
        executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> activityLogService.logPasswordResetRequest(
                        event.user(), device, event.requestContext())
        );
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handlePasswordChangedEvent(PasswordChangedEvent event) {
        executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> activityLogService.logPasswordChange(
                        event.user(), device, true, event.requestContext())
        );
    }



    // =============== MÉTHODES UTILITAIRES ===============

    /**
     * Exécute une action de logging après avoir tenté de détecter le device.
     * Si la détection échoue, l'action est quand même exécutée avec un device null.
     *
     * @param user L'utilisateur concerné
     * @param requestContext Le contexte de la requête pour la détection du device
     * @param loggingAction L'action de logging à exécuter avec le device (peut être null)
     */
    private void executeWithDeviceDetection(User user,
                                            RequestContext requestContext,
                                            Consumer<Device> loggingAction) {
        Device device = null;

        try {
            device = deviceService.detectFromRequestContext(requestContext, user);
        } catch (Exception e) {
            log.warn("Impossible de détecter le device pour l'utilisateur {} : {}",
                    user.getUsername(), e.getMessage());
            // On continue avec un device null
        }

        // Exécute l'action de logging avec le device (qui peut être null)
        loggingAction.accept(device);
    }
}
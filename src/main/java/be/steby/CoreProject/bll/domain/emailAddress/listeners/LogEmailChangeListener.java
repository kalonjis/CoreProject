package be.steby.CoreProject.bll.domain.emailAddress.listeners;


import be.steby.CoreProject.bll.domain.emailAddress.events.EmailChangeCancellationEvent;
import be.steby.CoreProject.bll.domain.emailAddress.events.EmailChangeConfirmationEvent;
import be.steby.CoreProject.bll.domain.emailAddress.events.EmailChangeRequestEvent;
import be.steby.CoreProject.bll.domain.emailAddress.events.EmailChangeVerificationEvent;
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
public class LogEmailChangeListener {

    private final ActivityLogService activityLogService;
    private final DeviceService deviceService;


    @EventListener
    @Async("activityLogExecutor")
    public void handleChangeEmailRequestEvent(EmailChangeRequestEvent event) {
        executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> activityLogService.logEmailChangeRequest(
                        event.user(), device, event.email(), event.requestContext())
        );
    }



    @EventListener
    @Async("activityLogExecutor")
    public void handleEmailChangeCancellationEvent(EmailChangeCancellationEvent event) {
        executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> activityLogService.logEmailChangeCancellation(
                        event.user(), device, event.newEmailAddress(), event.requestContext())
        );
    }


    @EventListener
    @Async("activityLogExecutor")
    public void handleChangeEmailVerificationEvent(EmailChangeVerificationEvent event) {
        executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> activityLogService.logEmailChangeVerification(
                        event.user(), device, event.email(), event.requestContext())
        );
    }


    @EventListener
    @Async("activityLogExecutor")
    public void handleEmailAddressConfirmation(EmailChangeConfirmationEvent event) {
        executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> activityLogService.logEmailChangeComplete(
                        event.user(), device, event.oldAddress(), event.newAddress(), event.requestContext())
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
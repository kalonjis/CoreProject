package be.steby.CoreProject.bll.listeners;

import be.steby.CoreProject.bll.domains.account.events.ConfirmNewUserAccountEvent;
import be.steby.CoreProject.bll.events.account.SignupEvent;
import be.steby.CoreProject.bll.domains.device.events.DeviceTrustLevelChangedEvent;
import be.steby.CoreProject.bll.domains.auth.events.UserLoggedInEvent;
import be.steby.CoreProject.bll.domains.auth.events.UserLogoutEvent;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.services.ActivityLogService;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
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
public class ActivityLogEventListener {

    private final ActivityLogService activityLogService;
    private final DeviceService deviceService;

    // =============== ÉVÉNEMENTS AVEC DEVICE FOURNI ===============

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
    public void handleLogin(UserLoggedInEvent event) {
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
    public void handleLogout(UserLogoutEvent event) {
        activityLogService.logLogout(
                event.user(),
                event.device(),
                event.requestContext()
        );
    }

    // =============== ÉVÉNEMENTS NÉCESSITANT DÉTECTION DU DEVICE ===============


    @EventListener
    @Async("activityLogExecutor")
    public void handleSignupEvent(SignupEvent event) {
        executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> activityLogService.logAccountCreation(
                        event.user(), device, event.requestContext())
        );
    }

    @EventListener
    @Async("activityLogExecutor")
    public void handleConfirmNewUserAccountEvent(ConfirmNewUserAccountEvent event) {
        executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> activityLogService.logNewAccountActivation(
                        event.user(), device, event.requestContext())
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
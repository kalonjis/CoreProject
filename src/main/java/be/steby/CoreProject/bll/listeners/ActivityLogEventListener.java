//package be.steby.CoreProject.bll.listeners;
//
//import be.steby.CoreProject.bll.domains.account.events.AccountConfirmationEvent;
//import be.steby.CoreProject.bll.events.account.SignupEvent;
//import be.steby.CoreProject.bll.domains.device.events.DeviceTrustLevelChangedEvent;
//import be.steby.CoreProject.bll.domains.auth.events.UserLoggedInEvent;
//import be.steby.CoreProject.bll.domains.auth.events.UserLogoutEvent;
//import be.steby.CoreProject.bll.common.models.RequestContext;
//import be.steby.CoreProject.bll.services.ActivityLogService;
//import be.steby.CoreProject.bll.domains.device.services.DeviceService;
//import be.steby.CoreProject.dl.entities.Device;
//import be.steby.CoreProject.dl.entities.User;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.events.EventListener;
//import org.springframework.core.annotation.Order;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//
//import java.util.function.Consumer;
//
///**
// * Listener pour enregistrer les événements dans les logs d'activité.
// * Utilise un executeur asynchrone dédié pour ne pas bloquer le thread principal.
// */
//@RequiredArgsConstructor
//@Component
//@Order(10) // Priorité élevée pour la journalisation
//@Slf4j
//public class ActivityLogEventListener {
//
//    private final ActivityLogService activityLogService;
//    private final DeviceService deviceService;
//
//    // =============== ÉVÉNEMENTS AVEC DEVICE FOURNI ===============
//
//    @EventListener
//    @Async("activityLogExecutor")
//    public void handleTrustLevelChange(DeviceTrustLevelChangedEvent events) {
//        activityLogService.logDeviceTrustLevelChange(
//                events.user(),
//                events.device(),
//                events.oldLevel(),
//                events.newTrustLevel().name(),
//                events.request()
//        );
//    }
//
//    @EventListener
//    @Async("activityLogExecutor")
//    public void handleLogin(UserLoggedInEvent events) {
//        activityLogService.logLogin(
//                events.user(),
//                events.device(),
//                events.successful(),
//                events.failureReason(),
//                events.requestContext()
//        );
//    }
//
//    @EventListener
//    @Async("activityLogExecutor")
//    public void handleLogout(UserLogoutEvent events) {
//        activityLogService.logLogout(
//                events.user(),
//                events.device(),
//                events.requestContext()
//        );
//    }
//
//    // =============== ÉVÉNEMENTS NÉCESSITANT DÉTECTION DU DEVICE ===============
//
//
//    @EventListener
//    @Async("activityLogExecutor")
//    public void handleSignupEvent(SignupEvent events) {
//        executeWithDeviceDetection(
//                events.user(),
//                events.requestContext(),
//                device -> activityLogService.logAccountCreation(
//                        events.user(), device, events.requestContext())
//        );
//    }
//
//
//
//     // =============== MÉTHODES UTILITAIRES ===============
//
//    /**
//     * Exécute une action de logging après avoir tenté de détecter le device.
//     * Si la détection échoue, l'action est quand même exécutée avec un device null.
//     *
//     * @param user L'utilisateur concerné
//     * @param requestContext Le contexte de la requête pour la détection du device
//     * @param loggingAction L'action de logging à exécuter avec le device (peut être null)
//     */
//    private void executeWithDeviceDetection(User user,
//                                            RequestContext requestContext,
//                                            Consumer<Device> loggingAction) {
//        Device device = null;
//
//        try {
//            device = deviceService.detectFromRequestContext(requestContext, user);
//        } catch (Exception e) {
//            log.warn("Impossible de détecter le device pour l'utilisateur {} : {}",
//                    user.getUsername(), e.getMessage());
//            // On continue avec un device null
//        }
//
//        // Exécute l'action de logging avec le device (qui peut être null)
//        loggingAction.accept(device);
//    }
//}
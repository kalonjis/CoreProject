//package be.steby.CoreProject.bll.common.listeners.users;
//
//import java.util.function.Consumer;
//
//import be.steby.CoreProject.bll.domains.device.services.DeviceService;
//import be.steby.CoreProject.dl.entities.User;
//import be.steby.CoreProject.dl.entities.Device;
//import be.steby.CoreProject.bll.common.models.RequestContext;
//import be.steby.CoreProject.bll.common.events.user.AdminUserCreatedEvent;
//import be.steby.CoreProject.bll.common.events.user.SelfSignupUserCreatedEvent;
//import be.steby.CoreProject.bll.common.events.user.SystemUserCreatedEvent;
//import be.steby.CoreProject.bll.services.ActivityLogService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.event.EventListener;
//import org.springframework.core.annotation.Order;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//@Order(10) // Priorité élevée pour la journalisation
//@Slf4j
//public class UserCreationActivityLogListener {
//
//    private final ActivityLogService activityLogService;
//    private final DeviceService deviceService;
//
//
//    /**
//     * Journalise l'auto-inscription
//     */
//    @EventListener
//    @Async("activityLogExecutor")
//    public void handleSelfSignupUserCreated(SelfSignupUserCreatedEvent event) {
//        executeWithDeviceDetection(
//                event.user(),
//                event.requestContext(),
//                device -> activityLogService.logAccountCreation(
//                        event.user(), device, event.requestContext())
//        );
//    }
//
//    /**
//     * Journalise la création par admin
//     */
//    @EventListener
//    @Async("activityLogExecutor")
//    public void handleAdminUserCreated(AdminUserCreatedEvent event) {
//        executeWithDeviceDetection(
//                event.user(),
//                event.requestContext(),
//                device -> activityLogService.logAccountCreation(
//                        event.user(), device, event.requestContext())
//        );
//    }
//
//    /**
//     * Journalise la création système
//     */
//    @EventListener
//    @Async("activityLogExecutor")
//    public void handleSystemUserCreated(SystemUserCreatedEvent event) {
//        activityLogService.logAccountCreation(
//            event.user(), null, event.requestContext()
//        );
//    }
//
//    // ← Ajouter cette méthode (copiée d'ActivityLogEventListener)
//    private void executeWithDeviceDetection(User user,
//                                            RequestContext requestContext,
//                                            Consumer<Device> loggingAction) {
//        Device device = null;
//        try {
//            device = deviceService.detectFromRequestContext(requestContext, user);
//        } catch (Exception e) {
//            log.warn("Impossible de détecter le device pour l'utilisateur {} : {}",
//                    user.getUsername(), e.getMessage());
//        }
//        loggingAction.accept(device);
//    }
//}

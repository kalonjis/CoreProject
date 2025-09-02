//package be.steby.CoreProject.bll.domains.password.listeners;
//
//// ← Ajoutez ces imports manquants
//import java.util.function.Consumer;
//import be.steby.CoreProject.dl.entities.User;
//import be.steby.CoreProject.dl.entities.Device;
//import be.steby.CoreProject.bll.common.models.RequestContext;
//
//// Vos imports existants
//import be.steby.CoreProject.bll.domains.device.services.DeviceService;
//import be.steby.CoreProject.bll.domains.password.events.PasswordChangedEvent;
//import be.steby.CoreProject.bll.domains.password.events.RequestPasswordResetEvent;
//import be.steby.CoreProject.bll.domains.password.events.RequestPasswordTokenEvent;
//import be.steby.CoreProject.bll.domains.password.logs.PasswordActivityLogService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.event.EventListener;
//import org.springframework.core.annotation.Order;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//
//@RequiredArgsConstructor
//@Component
//@Order(10)
//@Slf4j
//public class PasswordActivityLogListener {
//
//    private final PasswordActivityLogService passwordActivityLogService;
//    private final DeviceService deviceService;
//
//    @EventListener
//    @Async("activityLogExecutor")
//    public void handleRequestPasswordReset(RequestPasswordResetEvent event) {
//        executeWithDeviceDetection(
//                event.user(),
//                event.requestContext(),
//                device -> passwordActivityLogService.logPasswordResetRequest(
//                        event.user(), device, event.requestContext())
//        );
//    }
//
//    @EventListener
//    @Async("activityLogExecutor")
//    public void handlePasswordChangedEvent(PasswordChangedEvent event) {
//        executeWithDeviceDetection(
//                event.user(),
//                event.requestContext(),
//                device -> passwordActivityLogService.logPasswordChange(
//                        event.user(), device, true, event.requestContext())
//        );
//    }
//
//    @EventListener
//    @Async("activityLogExecutor")
//    public void handleRequestPasswordToken(RequestPasswordTokenEvent event) {
//        executeWithDeviceDetection(
//                event.user(),
//                event.requestContext(),
//                device -> passwordActivityLogService.logRequestPasswordToken(
//                        event.user(), device, event.requestContext())
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
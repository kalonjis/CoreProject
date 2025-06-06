package be.steby.CoreProject.bll.common.listeners.users;

import be.steby.CoreProject.bll.common.event.user.AdminUserCreatedEvent;
import be.steby.CoreProject.bll.common.event.user.SelfSignupUserCreatedEvent;
import be.steby.CoreProject.bll.common.event.user.SystemUserCreatedEvent;
import be.steby.CoreProject.bll.common.utils.DeviceDetectionHelper;
import be.steby.CoreProject.bll.services.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(10) // Priorité élevée pour la journalisation
@Slf4j
public class UserCreationActivityLogListener {

    private final ActivityLogService activityLogService;
    private final DeviceDetectionHelper deviceDetectionHelper;

    /**
     * Journalise l'auto-inscription
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleSelfSignupUserCreated(SelfSignupUserCreatedEvent event) {
        deviceDetectionHelper.executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> activityLogService.logAccountCreation(
                        event.user(), device, event.requestContext())
        );
    }

    /**
     * Journalise la création par admin
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleAdminUserCreated(AdminUserCreatedEvent event) {
        deviceDetectionHelper.executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> activityLogService.logAccountCreation(
                        event.user(), device, event.requestContext())
        );
    }

    /**
     * Journalise la création système
     */
    @EventListener
    @Async("activityLogExecutor")
    public void handleSystemUserCreated(SystemUserCreatedEvent event) {
        deviceDetectionHelper.executeWithDeviceDetection(
                event.user(),
                event.requestContext(),
                device -> activityLogService.logAccountCreation(
                        event.user(), device, event.requestContext())
        );
    }
}

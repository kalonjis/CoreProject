package be.steby.CoreProject.bll.domains.password.listeners;

import be.steby.CoreProject.bll.domains.admin.events.password.AdminPasswordResetTriggeredEvent;
import be.steby.CoreProject.bll.domains.notification.models.NotificationRequest;
import be.steby.CoreProject.bll.domains.notification.services.NotificationService;
import be.steby.CoreProject.bll.domains.password.events.PasswordChangedEvent;
import be.steby.CoreProject.bll.domains.password.events.PasswordResetCompletedEvent;
import be.steby.CoreProject.dl.enums.notification.NotificationPriority;
import be.steby.CoreProject.dl.enums.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener responsible for sending in-app (SSE) security notifications
 * on password-related events.
 *
 * <p>This listener is intentionally separate from {@link PasswordNotificationListener}
 * which handles email delivery. The split keeps email and in-app concerns decoupled
 * and allows each to evolve independently.</p>
 *
 * <h3>Covered events:</h3>
 * <ul>
 *   <li>{@link PasswordChangedEvent}              → SECURITY — password changed by the user</li>
 *   <li>{@link PasswordResetCompletedEvent}       → SECURITY — password reset via recovery flow</li>
 *   <li>{@link AdminPasswordResetTriggeredEvent}  → SECURITY — password reset triggered by an admin</li>
 * </ul>
 *
 * <p>All notifications are delivered immediately (no scheduling) and run on the
 * {@code notificationExecutor} thread pool at {@code @Order(10)} to execute before
 * activity log listeners ({@code @Order(100)}).</p>
 *
 * @see PasswordNotificationListener — email counterpart
 * @see NotificationService
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordInAppNotificationListener {

    private final NotificationService notificationService;

    private static final String SOURCE_DOMAIN = "password";
    private static final String ACTION_URL = "/account/security";

    // =========================================================================
    // Password changed (authenticated user)
    // =========================================================================

    /**
     * Sends a SECURITY in-app notification when an authenticated user changes
     * their own password.
     *
     * <p>Allows the user to react immediately if the change was not initiated by them.</p>
     *
     * @param event the password changed event
     */
    @EventListener
    @Async("notificationExecutor")
    @Order(10)
    public void handlePasswordChanged(PasswordChangedEvent event) {
        log.debug("Sending in-app notification for password change — user: {}",
                event.user().getUsername());
        try {
            NotificationRequest request = NotificationRequest.builder()
                    .recipient(event.user())
                    .type(NotificationType.SECURITY)
                    .title("🔑 Mot de passe modifié")
                    .body("Votre mot de passe vient d'être modifié. Si vous n'êtes pas à l'origine de cette action, sécurisez votre compte immédiatement.")
                    .actionUrl(ACTION_URL)
                    .sourceDomain(SOURCE_DOMAIN)
                    .build();

            notificationService.send(request);
            log.info("✅ In-app notification sent for password change — user: {}",
                    event.user().getUsername());
        } catch (Exception e) {
            log.error("❌ Failed to send in-app notification for password change — user: {}: {}",
                    event.user().getUsername(), e.getMessage(), e);
        }
    }

    // =========================================================================
    // Password reset completed (token-based recovery flow)
    // =========================================================================

    /**
     * Sends a SECURITY in-app notification when a user successfully completes
     * a password reset via the recovery flow (email link or SMS code).
     *
     * <p>Distinct from {@link #handlePasswordChanged} which covers authenticated
     * password changes. This covers the unauthenticated recovery path.</p>
     *
     * @param event the password reset completed event
     */
    @EventListener
    @Async("notificationExecutor")
    @Order(10)
    public void handlePasswordResetCompleted(PasswordResetCompletedEvent event) {
        log.debug("Sending in-app notification for password reset completion — user: {}",
                event.user().getUsername());
        try {
            NotificationRequest request = NotificationRequest.builder()
                    .recipient(event.user())
                    .type(NotificationType.SECURITY)
                    .title("🔑 Mot de passe réinitialisé")
                    .body("Votre mot de passe a été réinitialisé avec succès. Si vous n'êtes pas à l'origine de cette action, contactez le support.")
                    .actionUrl(ACTION_URL)
                    .sourceDomain(SOURCE_DOMAIN)
                    .build();

            notificationService.send(request);
            log.info("✅ In-app notification sent for password reset completion — user: {}",
                    event.user().getUsername());
        } catch (Exception e) {
            log.error("❌ Failed to send in-app notification for password reset completion — user: {}: {}",
                    event.user().getUsername(), e.getMessage(), e);
        }
    }

    // =========================================================================
    // Admin-initiated password reset
    // =========================================================================

    /**
     * Sends a SECURITY in-app notification when an administrator triggers a
     * password reset for a user.
     *
     * <p>Priority is elevated to {@link NotificationPriority#URGENT} for
     * {@code SECURITY_BREACH} resets with active session invalidation, since
     * the user is likely being disconnected in the same operation.</p>
     *
     * @param event the admin password reset triggered event
     */
    @EventListener
    @Async("notificationExecutor")
    @Order(10)
    public void handleAdminPasswordReset(AdminPasswordResetTriggeredEvent event) {
        log.debug("Sending in-app notification for admin password reset — target: {}",
                event.getTargetUsername());
        try {
            NotificationPriority priority = event.isUrgentReset()
                    ? NotificationPriority.URGENT
                    : NotificationPriority.HIGH;

            String body = event.isSecurityBreach()
                    ? "Un administrateur a réinitialisé votre mot de passe pour des raisons de sécurité. Vos sessions actives ont été révoquées."
                    : "Un administrateur a initié une réinitialisation de votre mot de passe. Consultez votre email pour les instructions.";

            NotificationRequest request = NotificationRequest.builder()
                    .recipient(event.targetUser())
                    .type(NotificationType.SECURITY)
                    .priority(priority)
                    .title("🔒 Réinitialisation de mot de passe par un administrateur")
                    .body(body)
                    .actionUrl(ACTION_URL)
                    .sourceDomain(SOURCE_DOMAIN)
                    .build();

            notificationService.send(request);
            log.info("✅ In-app notification sent for admin password reset — target: {}, strategy: {}, priority: {}",
                    event.getTargetUsername(), event.strategy(), priority);
        } catch (Exception e) {
            log.error("❌ Failed to send in-app notification for admin password reset — target: {}: {}",
                    event.getTargetUsername(), e.getMessage(), e);
        }
    }
}
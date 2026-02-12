package be.steby.CoreProject.bll.domains.notification.config;

import be.steby.CoreProject.dl.enums.notification.NotificationChannel;
import be.steby.CoreProject.dl.enums.notification.NotificationPriority;
import be.steby.CoreProject.dl.enums.notification.NotificationType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration properties for the notification system.
 *
 * <p>Binds to properties under the {@code app.notification} prefix in application config.
 * Provides typed access to notification settings for services and schedulers.</p>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * @Autowired
 * private NotificationConfiguration config;
 *
 * long timeout = config.getSse().getTimeout();
 * Set<NotificationChannel> channels = config.getDefaults().getChannels().get(NotificationType.REMINDER);
 * }</pre>
 *
 * @see be.steby.CoreProject.bll.domains.notification.services.NotificationService
 * @see be.steby.CoreProject.bll.domains.notification.scheduler.NotificationScheduler
 */
@Component
@ConfigurationProperties(prefix = "app.notification")
@Getter
@Setter
public class NotificationConfiguration {

    /**
     * Global enable/disable for notification system.
     */
    private boolean enabled = true;

    /**
     * Scheduler configuration.
     */
    private SchedulerConfig scheduler = new SchedulerConfig();

    /**
     * SSE (Server-Sent Events) configuration.
     */
    private SseConfig sse = new SseConfig();

    /**
     * Channel-specific configurations.
     */
    private ChannelsConfig channels = new ChannelsConfig();

    /**
     * Default preferences configuration.
     */
    private DefaultsConfig defaults = new DefaultsConfig();

    /**
     * Rate limiting configuration.
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    /**
     * Quiet hours configuration.
     */
    private QuietHoursConfig quietHours = new QuietHoursConfig();

    /**
     * Digest configuration.
     */
    private DigestConfig digest = new DigestConfig();

    // =========================================================================
    // Nested Configuration Classes
    // =========================================================================

    /**
     * Scheduler settings for notification processing and cleanup.
     */
    @Getter
    @Setter
    public static class SchedulerConfig {

        /**
         * Enable/disable the notification scheduler.
         */
        private boolean enabled = true;

        /**
         * Interval for processing scheduled notifications (milliseconds).
         */
        private long processInterval = 60000L;

        /**
         * Cron expression for cleanup job.
         */
        private String cleanupCron = "0 0 3 * * ?";

        /**
         * Days to keep dismissed notifications.
         */
        private int retentionDays = 90;

        /**
         * Days to keep read notifications (0 = forever).
         */
        private int readRetentionDays = 0;
    }

    /**
     * SSE connection settings.
     */
    @Getter
    @Setter
    public static class SseConfig {

        /**
         * Connection timeout in milliseconds.
         */
        private long timeout = 1800000L; // 30 minutes

        /**
         * Heartbeat interval in milliseconds.
         */
        private long heartbeatInterval = 30000L; // 30 seconds

        /**
         * Enable heartbeat mechanism.
         */
        private boolean heartbeatEnabled = true;
    }

    /**
     * Channel-specific configurations.
     */
    @Getter
    @Setter
    public static class ChannelsConfig {

        private InAppChannelConfig inApp = new InAppChannelConfig();
        private EmailChannelConfig email = new EmailChannelConfig();
        private PushChannelConfig push = new PushChannelConfig();
        private SmsChannelConfig sms = new SmsChannelConfig();
    }

    /**
     * In-App (SSE) channel configuration.
     */
    @Getter
    @Setter
    public static class InAppChannelConfig {
        private boolean enabled = true;
    }

    /**
     * Email channel configuration.
     */
    @Getter
    @Setter
    public static class EmailChannelConfig {

        private boolean enabled = true;

        /**
         * Template base path (relative to templates/emails/).
         */
        private String templateBase = "notification/";

        /**
         * Templates for different notification types.
         */
        private EmailTemplatesConfig templates = new EmailTemplatesConfig();
    }

    /**
     * Email template mappings.
     */
    @Getter
    @Setter
    public static class EmailTemplatesConfig {
        private String generic = "notification-generic";
        private String reminder = "notification-reminder";
        private String security = "notification-security";
    }

    /**
     * Push notification channel configuration.
     */
    @Getter
    @Setter
    public static class PushChannelConfig {

        private boolean enabled = false;

        // VAPID keys for Web Push (future implementation)
        // private String vapidPublicKey;
        // private String vapidPrivateKey;
    }

    /**
     * SMS channel configuration.
     */
    @Getter
    @Setter
    public static class SmsChannelConfig {

        private boolean enabled = false;

        /**
         * Only send SMS for critical notifications.
         */
        private boolean criticalOnly = true;
    }

    /**
     * Default preferences configuration.
     */
    @Getter
    @Setter
    public static class DefaultsConfig {

        /**
         * Default channels per notification type.
         */
        private Map<NotificationType, Set<NotificationChannel>> channels = new EnumMap<>(NotificationType.class);
    }

    /**
     * Rate limiting configuration.
     */
    @Getter
    @Setter
    public static class RateLimitConfig {

        /**
         * Maximum notifications per user per hour (0 = unlimited).
         */
        private int maxPerHour = 100;

        /**
         * Maximum emails per user per day.
         */
        private int maxEmailsPerDay = 50;

        /**
         * Cooldown between similar notifications (seconds).
         */
        private int cooldownSeconds = 60;
    }

    /**
     * Quiet hours configuration.
     */
    @Getter
    @Setter
    public static class QuietHoursConfig {

        /**
         * Enable quiet hours feature.
         */
        private boolean enabled = true;

        /**
         * Default quiet hours start time.
         */
        private LocalTime defaultStart = LocalTime.of(22, 0);

        /**
         * Default quiet hours end time.
         */
        private LocalTime defaultEnd = LocalTime.of(8, 0);

        /**
         * Priorities that bypass quiet hours.
         */
        private List<NotificationPriority> bypassPriorities = List.of(NotificationPriority.URGENT);
    }

    /**
     * Digest configuration.
     */
    @Getter
    @Setter
    public static class DigestConfig {

        /**
         * Enable digest feature.
         */
        private boolean enabled = true;

        /**
         * Time to send daily digest.
         */
        private LocalTime dailySendTime = LocalTime.of(8, 0);

        /**
         * Day to send weekly digest (1=Monday, 7=Sunday).
         */
        private int weeklySendDay = 1;

        /**
         * Minimum notifications to include in digest.
         */
        private int minNotifications = 3;
    }

    // =========================================================================
    // Convenience Methods
    // =========================================================================

    /**
     * Checks if a specific channel is enabled.
     *
     * @param channel the channel to check
     * @return true if the channel is enabled
     */
    public boolean isChannelEnabled(NotificationChannel channel) {
        return switch (channel) {
            case IN_APP -> channels.getInApp().isEnabled();
            case EMAIL -> channels.getEmail().isEnabled();
            case PUSH -> channels.getPush().isEnabled();
            case SMS -> channels.getSms().isEnabled();
        };
    }

    /**
     * Gets the email template for a notification type.
     *
     * @param type the notification type
     * @return the template path
     */
    public String getEmailTemplate(NotificationType type) {
        String templateBase = channels.getEmail().getTemplateBase();
        String templateName = switch (type) {
            case SECURITY -> channels.getEmail().getTemplates().getSecurity();
            case REMINDER -> channels.getEmail().getTemplates().getReminder();
            default -> channels.getEmail().getTemplates().getGeneric();
        };
        return templateBase + templateName;
    }

    /**
     * Gets the default channels for a notification type.
     *
     * @param type the notification type
     * @return set of default channels
     */
    public Set<NotificationChannel> getDefaultChannels(NotificationType type) {
        return defaults.getChannels().getOrDefault(type, Set.of(NotificationChannel.IN_APP));
    }
}
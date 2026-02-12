package be.steby.CoreProject.bll.domains.notification.services;

import be.steby.CoreProject.bll.domains.notification.config.NotificationConfiguration;
import be.steby.CoreProject.dal.repositories.NotificationPreferenceRepository;
import be.steby.CoreProject.dl.entities.NotificationPreference;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.notification.NotificationChannel;
import be.steby.CoreProject.dl.enums.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;

/**
 * Service for managing user notification preferences.
 *
 * <p>Handles all operations related to user notification preferences,
 * including querying enabled channels, checking quiet hours, and
 * updating preference settings.</p>
 *
 * <h4>Default Behavior:</h4>
 * <p>When no explicit preference exists for a user/type/channel combination,
 * the system applies defaults from configuration (notification.yml):</p>
 * <ul>
 *   <li><b>IN_APP:</b> Always enabled by default</li>
 *   <li><b>EMAIL:</b> Enabled by default for CONFIRMATION, SECURITY, SYSTEM</li>
 *   <li><b>PUSH:</b> Enabled by default for REMINDER, ALERT, SECURITY</li>
 *   <li><b>SMS:</b> Disabled by default (explicit opt-in required)</li>
 * </ul>
 *
 * @see NotificationPreference
 * @see NotificationConfiguration
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationConfiguration notificationConfig;

    // =========================================================================
    // Channel Queries
    // =========================================================================

    /**
     * Gets the enabled channels for a user and notification type.
     *
     * <p>Merges explicit user preferences with defaults from config:</p>
     * <ol>
     *   <li>Start with default channels for the type (from notification.yml)</li>
     *   <li>Remove explicitly disabled channels</li>
     *   <li>Add explicitly enabled channels</li>
     *   <li>Filter out globally disabled channels</li>
     * </ol>
     *
     * @param user the user
     * @param type the notification type
     * @return set of enabled channels
     */
    public Set<NotificationChannel> getEnabledChannels(User user, NotificationType type) {
        // Start with defaults from configuration
        Set<NotificationChannel> channels = new HashSet<>(
                notificationConfig.getDefaultChannels(type)
        );

        // Get user's explicit preferences
        Set<NotificationChannel> disabled = preferenceRepository
                .findDisabledChannelsByUserAndType(user, type);
        Set<NotificationChannel> enabled = preferenceRepository
                .findEnabledChannelsByUserAndType(user, type);

        // Apply user preferences
        channels.removeAll(disabled);
        channels.addAll(enabled);

        // Filter out globally disabled channels
        channels.removeIf(channel -> !notificationConfig.isChannelEnabled(channel));

        return channels;
    }

    /**
     * Checks if a specific channel is enabled for a user and type.
     *
     * @param user    the user
     * @param type    the notification type
     * @param channel the channel to check
     * @return true if the channel is enabled
     */
    public boolean isChannelEnabled(User user, NotificationType type, NotificationChannel channel) {
        // First check if channel is globally enabled
        if (!notificationConfig.isChannelEnabled(channel)) {
            return false;
        }

        // Check explicit preference first
        Optional<NotificationPreference> preference = preferenceRepository
                .findByUserAndNotificationTypeAndChannel(user, type, channel);

        if (preference.isPresent()) {
            return preference.get().isEnabled();
        }

        // Fall back to default from configuration
        return notificationConfig.getDefaultChannels(type).contains(channel);
    }

    // =========================================================================
    // Quiet Hours
    // =========================================================================

    /**
     * Checks if the user is currently in quiet hours for a channel.
     *
     * <p>First checks user's explicit quiet hours, then falls back to
     * global defaults from configuration if quiet hours feature is enabled.</p>
     *
     * @param user    the user
     * @param channel the channel to check
     * @param time    the current time
     * @return true if in quiet hours
     */
    public boolean isInQuietHours(User user, NotificationChannel channel, LocalTime time) {
        // Check user's explicit quiet hours first
        List<NotificationPreference> preferences = preferenceRepository
                .findByUserAndChannel(user, channel);

        boolean hasUserQuietHours = preferences.stream()
                .anyMatch(NotificationPreference::hasQuietHours);

        if (hasUserQuietHours) {
            return preferences.stream()
                    .filter(NotificationPreference::hasQuietHours)
                    .anyMatch(p -> p.isInQuietHours(time));
        }

        // Fall back to global defaults if quiet hours feature is enabled
        if (notificationConfig.getQuietHours().isEnabled()) {
            LocalTime defaultStart = notificationConfig.getQuietHours().getDefaultStart();
            LocalTime defaultEnd = notificationConfig.getQuietHours().getDefaultEnd();
            return isTimeInRange(time, defaultStart, defaultEnd);
        }

        return false;
    }

    /**
     * Checks if a time is within a range (handles overnight ranges).
     */
    private boolean isTimeInRange(LocalTime time, LocalTime start, LocalTime end) {
        if (start.isAfter(end)) {
            // Overnight (e.g., 22:00 - 08:00)
            return time.isAfter(start) || time.isBefore(end);
        } else {
            return !time.isBefore(start) && time.isBefore(end);
        }
    }

    /**
     * Gets quiet hours configuration for a user and channel.
     *
     * @param user    the user
     * @param channel the channel
     * @return optional containing quiet hours if configured
     */
    public Optional<QuietHoursConfig> getQuietHours(User user, NotificationChannel channel) {
        // Check user preferences first
        Optional<QuietHoursConfig> userConfig = preferenceRepository
                .findByUserAndChannel(user, channel).stream()
                .filter(NotificationPreference::hasQuietHours)
                .findFirst()
                .map(p -> new QuietHoursConfig(p.getQuietHoursStart(), p.getQuietHoursEnd()));

        if (userConfig.isPresent()) {
            return userConfig;
        }

        // Return global defaults if enabled
        if (notificationConfig.getQuietHours().isEnabled()) {
            return Optional.of(new QuietHoursConfig(
                    notificationConfig.getQuietHours().getDefaultStart(),
                    notificationConfig.getQuietHours().getDefaultEnd()
            ));
        }

        return Optional.empty();
    }

    /**
     * Quiet hours configuration record.
     */
    public record QuietHoursConfig(LocalTime start, LocalTime end) {
        public boolean isActive(LocalTime time) {
            if (start.isAfter(end)) {
                // Overnight (e.g., 22:00 - 08:00)
                return time.isAfter(start) || time.isBefore(end);
            } else {
                return !time.isBefore(start) && time.isBefore(end);
            }
        }
    }

    // =========================================================================
    // Preference Management
    // =========================================================================

    /**
     * Gets all preferences for a user.
     *
     * @param user the user
     * @return list of user's preferences
     */
    public List<NotificationPreference> getAllPreferences(User user) {
        return preferenceRepository.findByUser(user);
    }

    /**
     * Gets the full preference matrix for a user.
     *
     * <p>Returns a map of type → channel → enabled status,
     * including defaults for unset preferences.</p>
     *
     * @param user the user
     * @return the complete preference matrix
     */
    public Map<NotificationType, Map<NotificationChannel, Boolean>> getPreferenceMatrix(User user) {
        Map<NotificationType, Map<NotificationChannel, Boolean>> matrix = new EnumMap<>(NotificationType.class);

        // Initialize with defaults from configuration
        for (NotificationType type : NotificationType.values()) {
            Map<NotificationChannel, Boolean> channelMap = new EnumMap<>(NotificationChannel.class);
            Set<NotificationChannel> defaults = notificationConfig.getDefaultChannels(type);

            for (NotificationChannel channel : NotificationChannel.values()) {
                // Channel is enabled if: globally enabled AND in defaults
                boolean enabled = notificationConfig.isChannelEnabled(channel)
                        && defaults.contains(channel);
                channelMap.put(channel, enabled);
            }
            matrix.put(type, channelMap);
        }

        // Override with explicit user preferences
        List<NotificationPreference> preferences = preferenceRepository.findByUser(user);
        for (NotificationPreference pref : preferences) {
            // Only apply if channel is globally enabled
            if (notificationConfig.isChannelEnabled(pref.getChannel())) {
                matrix.get(pref.getNotificationType())
                        .put(pref.getChannel(), pref.isEnabled());
            }
        }

        return matrix;
    }

    /**
     * Sets a preference for a user.
     *
     * @param user    the user
     * @param type    the notification type
     * @param channel the channel
     * @param enabled whether to enable the channel
     * @return the created or updated preference
     */
    @Transactional
    public NotificationPreference setPreference(
            User user,
            NotificationType type,
            NotificationChannel channel,
            boolean enabled) {

        NotificationPreference preference = preferenceRepository
                .findByUserAndNotificationTypeAndChannel(user, type, channel)
                .orElseGet(() -> NotificationPreference.builder()
                        .user(user)
                        .notificationType(type)
                        .channel(channel)
                        .build());

        preference.setEnabled(enabled);
        preference = preferenceRepository.save(preference);

        log.debug("Preference updated: user={}, type={}, channel={}, enabled={}",
                user.getPublicId(), type, channel, enabled);

        return preference;
    }

    /**
     * Sets quiet hours for a user and channel.
     *
     * @param user    the user
     * @param channel the channel
     * @param start   quiet hours start time
     * @param end     quiet hours end time
     */
    @Transactional
    public void setQuietHours(
            User user,
            NotificationChannel channel,
            LocalTime start,
            LocalTime end) {

        List<NotificationPreference> preferences = preferenceRepository
                .findByUserAndChannel(user, channel);

        if (preferences.isEmpty()) {
            // Create preferences to hold quiet hours
            for (NotificationType type : NotificationType.values()) {
                NotificationPreference pref = NotificationPreference.builder()
                        .user(user)
                        .notificationType(type)
                        .channel(channel)
                        .enabled(isChannelEnabled(user, type, channel))
                        .quietHoursStart(start)
                        .quietHoursEnd(end)
                        .build();
                preferenceRepository.save(pref);
            }
        } else {
            for (NotificationPreference pref : preferences) {
                pref.setQuietHoursStart(start);
                pref.setQuietHoursEnd(end);
                preferenceRepository.save(pref);
            }
        }

        log.debug("Quiet hours set for user={}, channel={}: {} - {}",
                user.getPublicId(), channel, start, end);
    }

    /**
     * Clears quiet hours for a user and channel.
     *
     * @param user    the user
     * @param channel the channel
     */
    @Transactional
    public void clearQuietHours(User user, NotificationChannel channel) {
        List<NotificationPreference> preferences = preferenceRepository
                .findByUserAndChannel(user, channel);

        for (NotificationPreference pref : preferences) {
            pref.setQuietHoursStart(null);
            pref.setQuietHoursEnd(null);
            preferenceRepository.save(pref);
        }

        log.debug("Quiet hours cleared for user={}, channel={}", user.getPublicId(), channel);
    }

    /**
     * Enables all channels for a notification type.
     *
     * @param user the user
     * @param type the notification type
     */
    @Transactional
    public void enableAllChannels(User user, NotificationType type) {
        for (NotificationChannel channel : NotificationChannel.values()) {
            if (notificationConfig.isChannelEnabled(channel)) {
                setPreference(user, type, channel, true);
            }
        }
        log.debug("All channels enabled for user={}, type={}", user.getPublicId(), type);
    }

    /**
     * Disables all channels for a notification type (mute).
     *
     * @param user the user
     * @param type the notification type
     */
    @Transactional
    public void disableAllChannels(User user, NotificationType type) {
        for (NotificationChannel channel : NotificationChannel.values()) {
            setPreference(user, type, channel, false);
        }
        log.debug("All channels disabled (muted) for user={}, type={}", user.getPublicId(), type);
    }

    /**
     * Resets all preferences to defaults for a user.
     *
     * @param user the user
     */
    @Transactional
    public void resetToDefaults(User user) {
        preferenceRepository.deleteByUser(user);
        log.debug("Preferences reset to defaults for user={}", user.getPublicId());
    }
}
package be.steby.CoreProject.bll.domains.notification.services;

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
 * the system applies sensible defaults:</p>
 * <ul>
 *   <li><b>IN_APP:</b> Always enabled by default</li>
 *   <li><b>EMAIL:</b> Enabled by default for CONFIRMATION, SECURITY, SYSTEM</li>
 *   <li><b>PUSH:</b> Enabled by default for REMINDER, ALERT, SECURITY</li>
 *   <li><b>SMS:</b> Disabled by default (explicit opt-in required)</li>
 * </ul>
 *
 * <h4>Usage Example:</h4>
 * <pre>{@code
 * // Get enabled channels for a notification
 * Set<NotificationChannel> channels = preferenceService.getEnabledChannels(
 *     user, NotificationType.REMINDER);
 *
 * // Check if in quiet hours
 * boolean quiet = preferenceService.isInQuietHours(
 *     user, NotificationChannel.PUSH, LocalTime.now());
 *
 * // Update a preference
 * preferenceService.setPreference(
 *     user, NotificationType.SOCIAL, NotificationChannel.EMAIL, false);
 * }</pre>
 *
 * @see NotificationPreference
 * @see NotificationType
 * @see NotificationChannel
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    // =========================================================================
    // Default Channel Configuration
    // =========================================================================

    /**
     * Default enabled channels per notification type.
     *
     * <p>Applied when no explicit user preference exists.</p>
     */
    private static final Map<NotificationType, Set<NotificationChannel>> DEFAULT_CHANNELS = Map.of(
            NotificationType.REMINDER, Set.of(
                    NotificationChannel.IN_APP,
                    NotificationChannel.PUSH
            ),
            NotificationType.CONFIRMATION, Set.of(
                    NotificationChannel.IN_APP,
                    NotificationChannel.EMAIL
            ),
            NotificationType.ALERT, Set.of(
                    NotificationChannel.IN_APP,
                    NotificationChannel.PUSH,
                    NotificationChannel.EMAIL
            ),
            NotificationType.SECURITY, Set.of(
                    NotificationChannel.IN_APP,
                    NotificationChannel.PUSH,
                    NotificationChannel.EMAIL
            ),
            NotificationType.SOCIAL, Set.of(
                    NotificationChannel.IN_APP,
                    NotificationChannel.PUSH
            ),
            NotificationType.INFO, Set.of(
                    NotificationChannel.IN_APP
            ),
            NotificationType.SYSTEM, Set.of(
                    NotificationChannel.IN_APP,
                    NotificationChannel.EMAIL
            )
    );

    // =========================================================================
    // Channel Queries
    // =========================================================================

    /**
     * Gets the enabled channels for a user and notification type.
     *
     * <p>Merges explicit user preferences with defaults:</p>
     * <ol>
     *   <li>Start with default channels for the type</li>
     *   <li>Remove explicitly disabled channels</li>
     *   <li>Add explicitly enabled channels</li>
     * </ol>
     *
     * @param user the user
     * @param type the notification type
     * @return set of enabled channels
     */
    public Set<NotificationChannel> getEnabledChannels(User user, NotificationType type) {
        // Start with defaults
        Set<NotificationChannel> channels = new HashSet<>(
                DEFAULT_CHANNELS.getOrDefault(type, Set.of(NotificationChannel.IN_APP))
        );

        // Get user's explicit preferences
        Set<NotificationChannel> disabled = preferenceRepository
                .findDisabledChannelsByUserAndType(user, type);
        Set<NotificationChannel> enabled = preferenceRepository
                .findEnabledChannelsByUserAndType(user, type);

        // Apply preferences
        channels.removeAll(disabled);
        channels.addAll(enabled);

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
        // Check explicit preference first
        Optional<NotificationPreference> preference = preferenceRepository
                .findByUserAndNotificationTypeAndChannel(user, type, channel);

        if (preference.isPresent()) {
            return preference.get().isEnabled();
        }

        // Fall back to default
        Set<NotificationChannel> defaults = DEFAULT_CHANNELS.getOrDefault(
                type, Set.of(NotificationChannel.IN_APP));
        return defaults.contains(channel);
    }

    // =========================================================================
    // Quiet Hours
    // =========================================================================

    /**
     * Checks if the user is currently in quiet hours for a channel.
     *
     * @param user    the user
     * @param channel the channel to check
     * @param time    the current time
     * @return true if in quiet hours
     */
    public boolean isInQuietHours(User user, NotificationChannel channel, LocalTime time) {
        List<NotificationPreference> preferences = preferenceRepository
                .findByUserAndChannel(user, channel);

        return preferences.stream()
                .filter(NotificationPreference::hasQuietHours)
                .anyMatch(p -> p.isInQuietHours(time));
    }

    /**
     * Gets quiet hours configuration for a user and channel.
     *
     * @param user    the user
     * @param channel the channel
     * @return optional containing quiet hours if configured
     */
    public Optional<QuietHoursConfig> getQuietHours(User user, NotificationChannel channel) {
        return preferenceRepository.findByUserAndChannel(user, channel).stream()
                .filter(NotificationPreference::hasQuietHours)
                .findFirst()
                .map(p -> new QuietHoursConfig(p.getQuietHoursStart(), p.getQuietHoursEnd()));
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

        // Initialize with defaults
        for (NotificationType type : NotificationType.values()) {
            Map<NotificationChannel, Boolean> channelMap = new EnumMap<>(NotificationChannel.class);
            Set<NotificationChannel> defaults = DEFAULT_CHANNELS.getOrDefault(
                    type, Set.of(NotificationChannel.IN_APP));

            for (NotificationChannel channel : NotificationChannel.values()) {
                channelMap.put(channel, defaults.contains(channel));
            }
            matrix.put(type, channelMap);
        }

        // Override with explicit preferences
        List<NotificationPreference> preferences = preferenceRepository.findByUser(user);
        for (NotificationPreference pref : preferences) {
            matrix.get(pref.getNotificationType())
                    .put(pref.getChannel(), pref.isEnabled());
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

        // Update all preferences for this channel
        List<NotificationPreference> preferences = preferenceRepository
                .findByUserAndChannel(user, channel);

        if (preferences.isEmpty()) {
            // Create a preference to hold quiet hours
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
            setPreference(user, type, channel, true);
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
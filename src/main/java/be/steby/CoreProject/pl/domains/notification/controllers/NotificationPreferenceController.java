package be.steby.CoreProject.pl.domains.notification.controllers;

import be.steby.CoreProject.bll.domains.notification.services.NotificationPreferenceService;
import be.steby.CoreProject.dl.entities.NotificationPreference;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.notification.NotificationChannel;
import be.steby.CoreProject.dl.enums.notification.NotificationType;
import be.steby.CoreProject.pl.domains.notification.models.requests.BulkUpdatePreferencesRequest;
import be.steby.CoreProject.pl.domains.notification.models.requests.MuteNotificationTypeRequest;
import be.steby.CoreProject.pl.domains.notification.models.requests.UpdatePreferenceRequest;
import be.steby.CoreProject.pl.domains.notification.models.requests.UpdateQuietHoursRequest;
import be.steby.CoreProject.pl.domains.notification.models.responses.NotificationPreferenceMatrixResponse;
import be.steby.CoreProject.pl.domains.notification.models.responses.NotificationPreferenceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for notification preference management.
 *
 * <p>Provides endpoints for:</p>
 * <ul>
 *   <li>Viewing current preferences</li>
 *   <li>Updating individual preferences</li>
 *   <li>Bulk preference updates</li>
 *   <li>Quiet hours configuration</li>
 *   <li>Muting/unmuting notification types</li>
 * </ul>
 *
 * <h4>Preference Matrix:</h4>
 * <p>Preferences are organized as a matrix of notification type × channel.
 * Each cell indicates whether that type should be delivered via that channel.</p>
 *
 * <h4>Default Behavior:</h4>
 * <p>If no explicit preference exists, the system uses sensible defaults.
 * See {@link NotificationPreferenceService} for default channel configurations.</p>
 *
 * @see NotificationPreferenceService
 * @see NotificationPreference
 */
@RestController
@RequestMapping("/api/notifications/preferences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Preferences", description = "Manage notification delivery preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    // =========================================================================
    // Read Preferences
    // =========================================================================

    /**
     * Gets all preferences for the authenticated user.
     *
     * <p>Returns a list of explicitly set preferences. Channels without
     * explicit preferences use system defaults.</p>
     *
     * @param user the authenticated user
     * @return list of preferences
     */
    @GetMapping
    @Operation(summary = "Get all preferences", description = "Returns user's notification preferences")
    public ResponseEntity<List<NotificationPreferenceResponse>> getPreferences(
            @AuthenticationPrincipal User user) {

        List<NotificationPreference> preferences = preferenceService.getAllPreferences(user);
        return ResponseEntity.ok(NotificationPreferenceResponse.from(preferences));
    }

    /**
     * Gets the complete preference matrix.
     *
     * <p>Returns all notification types × all channels with their enabled status.
     * This includes default values for preferences not explicitly set.</p>
     *
     * @param user the authenticated user
     * @return the complete preference matrix
     */
    @GetMapping("/matrix")
    @Operation(
            summary = "Get preference matrix",
            description = "Returns complete matrix of all types and channels with enabled status"
    )
    public ResponseEntity<NotificationPreferenceMatrixResponse> getPreferenceMatrix(
            @AuthenticationPrincipal User user) {

        Map<NotificationType, Map<NotificationChannel, Boolean>> matrix =
                preferenceService.getPreferenceMatrix(user);

        // Build quiet hours map
        Map<NotificationChannel, NotificationPreferenceMatrixResponse.QuietHoursResponse> quietHours =
                new HashMap<>();

        for (NotificationChannel channel : NotificationChannel.values()) {
            preferenceService.getQuietHours(user, channel).ifPresent(qh ->
                    quietHours.put(channel, new NotificationPreferenceMatrixResponse.QuietHoursResponse(
                            qh.start(), qh.end()
                    ))
            );
        }

        return ResponseEntity.ok(new NotificationPreferenceMatrixResponse(matrix, quietHours));
    }


    /**
     * Gets preferences for a specific channel.
     *
     * @param user    the authenticated user
     * @param channel the channel to query
     * @return list of preferences for the channel
     */
    @GetMapping("/channel/{channel}")
    @Operation(summary = "Get preferences by channel", description = "Returns all preferences for a specific channel")
    public ResponseEntity<List<NotificationPreferenceResponse>> getPreferencesByChannel(
            @AuthenticationPrincipal User user,
            @PathVariable NotificationChannel channel) {

        List<NotificationPreference> preferences = preferenceService.getPreferencesByChannel(user, channel);
        return ResponseEntity.ok(NotificationPreferenceResponse.from(preferences));
    }

    // =========================================================================
    // Update Preferences
    // =========================================================================

    /**
     * Updates a single preference.
     *
     * @param user    the authenticated user
     * @param request the preference update
     * @return the updated preference
     */
    @PutMapping
    @Operation(summary = "Update preference", description = "Updates a single notification preference")
    public ResponseEntity<NotificationPreferenceResponse> updatePreference(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdatePreferenceRequest request) {

        NotificationPreference preference = preferenceService.setPreference(
                user,
                request.notificationType(),
                request.channel(),
                request.enabled()
        );

        log.info("Preference updated for user {}: {}/{} = {}",
                user.getPublicId(), request.notificationType(), request.channel(), request.enabled());

        return ResponseEntity.ok(NotificationPreferenceResponse.from(preference));
    }

    /**
     * Updates multiple preferences at once.
     *
     * @param user    the authenticated user
     * @param request the bulk update request
     * @return count of updated preferences
     */
    @PutMapping("/bulk")
    @Operation(summary = "Bulk update preferences", description = "Updates multiple preferences at once")
    public ResponseEntity<Map<String, Integer>> bulkUpdatePreferences(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BulkUpdatePreferencesRequest request) {

        int count = 0;
        for (UpdatePreferenceRequest pref : request.preferences()) {
            preferenceService.setPreference(
                    user,
                    pref.notificationType(),
                    pref.channel(),
                    pref.enabled()
            );
            count++;
        }

        log.info("Bulk updated {} preferences for user {}", count, user.getPublicId());
        return ResponseEntity.ok(Map.of("updated", count));
    }

    // =========================================================================
    // Quiet Hours
    // =========================================================================

    /**
     * Sets quiet hours for a channel.
     *
     * <p>During quiet hours, non-urgent notifications are held for later
     * delivery. Set both start and end to null to disable.</p>
     *
     * @param user    the authenticated user
     * @param request the quiet hours configuration
     * @return success message
     */
    @PutMapping("/quiet-hours")
    @Operation(summary = "Set quiet hours", description = "Configures quiet hours for a channel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quiet hours updated"),
            @ApiResponse(responseCode = "400", description = "Invalid configuration")
    })
    public ResponseEntity<Map<String, String>> setQuietHours(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateQuietHoursRequest request) {

        if (!request.isValid()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Both start and end must be set, or both must be null"));
        }

        if (request.start() == null && request.end() == null) {
            preferenceService.clearQuietHours(user, request.channel());
            log.info("Quiet hours cleared for user {}, channel {}",
                    user.getPublicId(), request.channel());
            return ResponseEntity.ok(Map.of("message", "Quiet hours disabled"));
        }

        preferenceService.setQuietHours(user, request.channel(), request.start(), request.end());
        log.info("Quiet hours set for user {}, channel {}: {} - {}",
                user.getPublicId(), request.channel(), request.start(), request.end());

        return ResponseEntity.ok(Map.of("message", "Quiet hours updated"));
    }

    /**
     * Gets quiet hours for a specific channel.
     *
     * @param user    the authenticated user
     * @param channel the channel to query
     * @return quiet hours if configured
     */
    @GetMapping("/quiet-hours/{channel}")
    @Operation(summary = "Get quiet hours", description = "Gets quiet hours configuration for a channel")
    public ResponseEntity<Map<String, Object>> getQuietHours(
            @AuthenticationPrincipal User user,
            @PathVariable NotificationChannel channel) {

        return preferenceService.getQuietHours(user, channel)
                .map(qh -> ResponseEntity.ok(Map.<String, Object>of(
                        "channel", channel,
                        "start", qh.start().toString(),
                        "end", qh.end().toString(),
                        "enabled", true
                )))
                .orElse(ResponseEntity.ok(Map.of(
                        "channel", channel,
                        "enabled", false
                )));
    }


    /**
     * Clears quiet hours for a channel.
     *
     * @param user    the authenticated user
     * @param channel the channel to clear quiet hours for
     * @return success message
     */
    @DeleteMapping("/quiet-hours/{channel}")
    @Operation(summary = "Clear quiet hours", description = "Disables quiet hours for a channel")
    public ResponseEntity<Map<String, String>> clearQuietHours(
            @AuthenticationPrincipal User user,
            @PathVariable NotificationChannel channel) {

        preferenceService.clearQuietHours(user, channel);
        log.info("Quiet hours cleared for user {}, channel {}", user.getPublicId(), channel);

        return ResponseEntity.ok(Map.of("message", "Quiet hours disabled for " + channel));
    }

    // =========================================================================
    // Mute/Unmute
    // =========================================================================

    /**
     * Mutes or unmutes a notification type.
     *
     * <p>Muting disables all channels for the specified type.
     * Unmuting enables all channels (restores to defaults).</p>
     *
     * @param user    the authenticated user
     * @param request the mute request
     * @return success message
     */
    @PostMapping("/mute")
    @Operation(summary = "Mute/unmute type", description = "Mutes or unmutes all channels for a notification type")
    public ResponseEntity<Map<String, String>> muteNotificationType(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody MuteNotificationTypeRequest request) {

        if (request.muted()) {
            preferenceService.disableAllChannels(user, request.notificationType());
            log.info("Notification type {} muted for user {}",
                    request.notificationType(), user.getPublicId());
            return ResponseEntity.ok(Map.of(
                    "message", request.notificationType() + " notifications muted"
            ));
        } else {
            preferenceService.enableAllChannels(user, request.notificationType());
            log.info("Notification type {} unmuted for user {}",
                    request.notificationType(), user.getPublicId());
            return ResponseEntity.ok(Map.of(
                    "message", request.notificationType() + " notifications unmuted"
            ));
        }
    }

    // =========================================================================
    // Reset
    // =========================================================================

    /**
     * Resets all preferences to system defaults.
     *
     * <p>Removes all explicit preferences. The user will receive
     * notifications according to system defaults.</p>
     *
     * @param user the authenticated user
     * @return success message
     */
    @PostMapping("/reset")
    @Operation(summary = "Reset to defaults", description = "Resets all preferences to system defaults")
    public ResponseEntity<Map<String, String>> resetToDefaults(@AuthenticationPrincipal User user) {
        preferenceService.resetToDefaults(user);
        log.info("Preferences reset to defaults for user {}", user.getPublicId());
        return ResponseEntity.ok(Map.of("message", "Preferences reset to defaults"));
    }


    // =========================================================================
    // Type-Based Enable/Disable All
    // =========================================================================

    /**
     * Enables all channels for a notification type.
     *
     * @param user the authenticated user
     * @param type the notification type
     * @return success message
     */
    @PostMapping("/type/{type}/enable-all")
    @Operation(summary = "Enable all channels for type", description = "Enables all channels for a notification type")
    public ResponseEntity<Map<String, String>> enableAllChannelsForType(
            @AuthenticationPrincipal User user,
            @PathVariable NotificationType type) {

        preferenceService.enableAllChannels(user, type);
        log.info("All channels enabled for user {}, type {}", user.getPublicId(), type);

        return ResponseEntity.ok(Map.of("message", type + " notifications enabled on all channels"));
    }

    /**
     * Disables all channels for a notification type (mute).
     *
     * @param user the authenticated user
     * @param type the notification type
     * @return success message
     */
    @PostMapping("/type/{type}/disable-all")
    @Operation(summary = "Disable all channels for type", description = "Disables all channels for a notification type (mute)")
    public ResponseEntity<Map<String, String>> disableAllChannelsForType(
            @AuthenticationPrincipal User user,
            @PathVariable NotificationType type) {

        preferenceService.disableAllChannels(user, type);
        log.info("All channels disabled for user {}, type {}", user.getPublicId(), type);

        return ResponseEntity.ok(Map.of("message", type + " notifications muted"));
    }
}
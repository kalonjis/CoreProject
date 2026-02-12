package be.steby.CoreProject.pl.domains.notification.models.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for bulk updating preferences.
 *
 * <h4>Usage:</h4>
 * <pre>{@code
 * PUT /api/notifications/preferences/bulk
 * {
 *   "preferences": [
 *     { "notificationType": "REMINDER", "channel": "EMAIL", "enabled": false },
 *     { "notificationType": "REMINDER", "channel": "PUSH", "enabled": true },
 *     { "notificationType": "SOCIAL", "channel": "EMAIL", "enabled": false }
 *   ]
 * }
 * }</pre>
 *
 * @param preferences list of preference updates
 */
public record BulkUpdatePreferencesRequest(
        @NotNull(message = "Preferences list is required")
        @Size(min = 1, max = 50, message = "Between 1 and 50 preferences can be updated at once")
        @Valid
        List<UpdatePreferenceRequest> preferences
) {}
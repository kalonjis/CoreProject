package be.steby.CoreProject.pl.domains.notification.models.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for bulk notification operations.
 *
 * <h4>Usage:</h4>
 * <pre>{@code
 * POST /api/notifications/read
 * POST /api/notifications/delete
 * {
 *   "publicIds": ["uuid1", "uuid2", "uuid3"]
 * }
 * }</pre>
 *
 * @param publicIds list of notification public IDs to process
 */
public record BulkNotificationIdsRequest(
        @NotEmpty(message = "At least one notification ID is required")
        @Size(max = 100, message = "Cannot process more than 100 notifications at once")
        List<String> publicIds
) {}
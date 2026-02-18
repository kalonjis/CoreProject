package be.steby.CoreProject.pl.domains.activitylog.models.responses;

import be.steby.CoreProject.dl.entities.ActivityLog;

import java.time.Instant;

/**
 * Read-only response DTO for a single activity log entry.
 *
 * <p>Exposes only the fields relevant to the caller. Internal fields
 * ({@code id}, {@code user} entity, {@code device} entity) are never
 * included to avoid leaking persistence details or circular references.</p>
 *
 * @param publicId       public identifier of the log entry (safe to expose in URLs)
 * @param actionType     enum constant name, e.g. {@code "LOGIN"}, {@code "LOGIN_FAILED"}
 * @param actionCategory domain group, e.g. {@code "AUTH"}, {@code "SECURITY"}
 * @param successful     whether the action completed successfully
 * @param failureReason  human-readable reason when {@code successful} is false; null otherwise
 * @param deviceId       public id of the device involved, null for system events
 * @param timestamp      when the action occurred (UTC)
 */
public record ActivityLogResponse(
        String publicId,
        String actionType,
        String actionCategory,
        boolean successful,
        String failureReason,
        String deviceId,
        Instant timestamp
) {

    /**
     * Builds an {@code ActivityLogResponse} from an {@link ActivityLog} entity.
     *
     * @param log the entity to map; must not be null
     * @return the corresponding response record
     */
    public static ActivityLogResponse from(ActivityLog log) {
        return new ActivityLogResponse(
                log.getPublicId().toString(),
                log.getActionType(),
                log.getActionCategory(),
                log.isSuccessful(),
                log.getFailureReason(),
                log.getDevice() != null ? log.getDevice().getPublicId().toString() : null,
                log.getTimestamp()
        );
    }
}
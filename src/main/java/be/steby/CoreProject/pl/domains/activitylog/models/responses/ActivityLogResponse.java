package be.steby.CoreProject.pl.domains.activitylog.models.responses;

import be.steby.CoreProject.dl.entities.ActivityLog;

import java.time.Instant;

/**
 * Read-only response DTO for a single activity log entry.
 *
 * <p>Device fields follow the same semantics as the entity:</p>
 * <ul>
 *   <li>{@code actorDeviceId} — the device that initiated the action (the session origin).
 *       Null for system events or token-based flows.</li>
 *   <li>{@code targetDeviceId} — the device affected by the action (e.g. remote disconnection,
 *       trust level change). Null when the action does not involve a second device.</li>
 * </ul>
 *
 * <p>Target user information (e.g. admin acting on another user) is not exposed as a
 * dedicated field — it is embedded in {@code actionDetails} as a narrative string,
 * since the front-end does not need to act on it structurally.</p>
 *
 * @param publicId        public identifier of the log entry
 * @param actionType      enum constant name, e.g. {@code "LOGIN"}, {@code "DEVICE_TRUSTED"}
 * @param actionCategory  domain group, e.g. {@code "AUTH"}, {@code "DEVICE"}
 * @param successful      whether the action completed successfully
 * @param failureReason   human-readable reason when {@code successful} is false; null otherwise
 * @param actionDetails   contextual details when {@code successful} is true; null otherwise
 * @param actorDeviceId   public id of the device that initiated the action; null if not applicable
 * @param targetDeviceId  public id of the device targeted by the action; null if not applicable
 * @param ipAddress       IP address of the actor device at the time of the action; null if not applicable
 * @param location        location derived from the IP at the time of the action; null if not applicable
 * @param timestamp       when the action occurred (UTC)
 */
public record ActivityLogResponse(
        String publicId,
        String actionType,
        String actionCategory,
        boolean successful,
        String failureReason,
        String actionDetails,
        String actorDeviceId,
        String targetDeviceId,
        String ipAddress,
        String location,
        Instant timestamp
) {

    /**
     * Maps an {@link ActivityLog} entity to this response record.
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
                log.getActionDetails(),
                log.getDevice()       != null ? log.getDevice().getPublicId().toString()       : null,
                log.getTargetDevice() != null ? log.getTargetDevice().getPublicId().toString() : null,
                log.getIpAddress(),
                log.getLocation(),
                log.getTimestamp()
        );
    }
}
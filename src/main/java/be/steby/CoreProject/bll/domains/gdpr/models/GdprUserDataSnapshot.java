package be.steby.CoreProject.bll.domains.gdpr.models;

import java.time.Instant;
import java.util.List;

/**
 * Immutable snapshot of all personal data belonging to a user.
 * Serialized to JSON and packaged into the GDPR export archive.
 *
 * <p>Only public IDs are exposed — internal Long IDs are never included.
 * Each nested record maps to one JSON file inside the ZIP archive.
 *
 * <p>Archive structure:
 * <pre>
 * gdpr-export-{username}-{date}.zip
 *   ├── profile.json
 *   ├── addresses.json
 *   ├── devices.json
 *   ├── activity.json
 *   ├── notifications.json
 *   ├── calendar.json
 *   └── sport.json
 * </pre>
 */
public record GdprUserDataSnapshot(

        /** Export metadata (not personal data — informational only). */
        ExportMeta meta,

        /** Core profile data. */
        ProfileData profile,

        /** All addresses linked to the user. */
        List<AddressData> addresses,

        /** All registered devices. */
        List<DeviceData> devices,

        /** Activity/audit log entries. */
        List<ActivityData> activity,

        /** Notification history. */
        List<NotificationData> notifications,

        /** Calendar events owned by the user. */
        List<CalendarData> calendar,

        /** Sport tracks owned by the user. */
        List<SportData> sport

) {

    // =========================================================================
    // Nested records — one per domain / JSON file
    // =========================================================================

    public record ExportMeta(
            String exportedAt,
            String requestedBy,
            String legalBasis
    ) {}

    public record ProfileData(
            String publicId,
            String username,
            String email,
            String recoveryEmail,
            String firstname,
            String lastname,
            String phoneNumber,
            boolean phoneNumberVerified,
            boolean emailVerified,
            boolean twoFactorEnabled,
            String bio,
            String avatarUrl,
            String createdAt,
            String activatedAt,
            String passwordChangedAt,
            List<String> roles
    ) {}

    public record AddressData(
            String publicId,
            String type,
            String label,
            boolean isDefault,
            String streetNumber,
            String streetName,
            String complement,
            String postalCode,
            String city,
            String stateProvince,
            String countryCode,
            String formattedAddress,
            String validFrom,
            String validTo
    ) {}

    public record DeviceData(
            String publicId,
            String deviceName,
            String browser,
            String operatingSystem,
            String deviceType,
            String trustLevel,
            String ipAddress,
            String lastSeenAt,
            String registeredAt,
            boolean loggedOut
    ) {}

    public record ActivityData(
            String publicId,
            String actionType,
            String actionCategory,
            boolean successful,
            String failureReason,
            String actionDetails,
            String ipAddress,
            String location,
            String timestamp
    ) {}

    public record NotificationData(
            String publicId,
            String type,
            String priority,
            String title,
            String body,
            boolean read,
            boolean dismissed,
            String createdAt,
            String readAt
    ) {}

    public record CalendarData(
            String publicId,
            String title,
            String description,
            String location,
            String status,
            String recurrence,
            String startDateTime,
            String endDateTime,
            boolean allDay,
            String createdAt
    ) {}

    public record SportData(
            String publicId,
            String name,
            String sportType,
            Double distanceKm,
            Long durationSeconds,
            Double avgSpeedKmh,
            Double maxSpeedKmh,
            Double elevationGainMeters,
            String startedAt,
            String endedAt
    ) {}
}
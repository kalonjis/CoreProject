package be.steby.CoreProject.bll.domains.gdpr.services.data_collector;

import be.steby.CoreProject.bll.domains.gdpr.models.GdprUserDataSnapshot;
import be.steby.CoreProject.bll.domains.gdpr.models.GdprUserDataSnapshot.*;
import be.steby.CoreProject.dal.CalendarEventRepository;
import be.steby.CoreProject.dal.repositories.*;
import be.steby.CoreProject.dl.entities.*;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Implementation of {@link GdprDataCollectorService}.
 *
 * <p>Each domain section is collected independently and wrapped in a
 * try/catch so that a repository failure in one domain (e.g. sport)
 * never prevents the rest of the snapshot from being built.
 * Failed sections default to an empty list and a warning is logged.
 *
 * <p>All timestamps are serialized as ISO-8601 strings.
 * Internal Long IDs are never included — only publicIds are exposed.
 *
 * <p>NOTE: {@link ActivityLogRepository} requires a {@code findByUserOrderByTimestampDesc}
 * method. Add the following to the repository interface if not already present:
 * <pre>
 * {@code List<ActivityLog> findByUserOrderByTimestampDesc(User user);}
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GdprDataCollectorServiceImpl implements GdprDataCollectorService {

    private final DeviceRepository         deviceRepository;
    private final ActivityLogRepository    activityLogRepository;
    private final NotificationRepository   notificationRepository;
    private final CalendarEventRepository  calendarEventRepository;
    private final SportTrackRepository     sportTrackRepository;
    private final UserAddressRepository    userAddressRepository;

    // =========================================================================
    // Public API
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public GdprUserDataSnapshot collect(User user) {
        log.info("Collecting GDPR data for user: {}", user.getUsername());

        return new GdprUserDataSnapshot(
                buildMeta(user),
                buildProfile(user),
                collectAddresses(user),
                collectDevices(user),
                collectActivity(user),
                collectNotifications(user),
                collectCalendar(user),
                collectSport(user)
        );
    }

    // =========================================================================
    // Meta
    // =========================================================================

    private ExportMeta buildMeta(User user) {
        return new ExportMeta(
                Instant.now().toString(),
                user.getUsername(),
                "GDPR Article 20 — Right to data portability"
        );
    }

    // =========================================================================
    // Profile
    // =========================================================================

    private ProfileData buildProfile(User user) {
        return new ProfileData(
                user.getPublicId(),
                user.getUsername(),
                user.getEmail(),
                user.getRecoveryEmail(),
                user.getFirstname(),
                user.getLastname(),
                user.getPhoneNumber(),
                user.isPhoneNumberVerified(),
                user.isEmailVerified(),
                user.isTwoFactorEnabled(),
                user.getBio(),
                user.getAvatarUrl(),
                ts(user.getCreatedAt()),
                ts(user.getActivatedAt()),
                ts(user.getPasswordChangedAt()),
                user.getUserRoles().stream().map(UserRole::name).toList()
        );
    }

    // =========================================================================
    // Addresses
    // =========================================================================

    private List<AddressData> collectAddresses(User user) {
        try {
            return userAddressRepository.findByUser(user).stream()
                    .map(this::mapAddress)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to collect addresses for user {}: {}", user.getUsername(), e.getMessage());
            return List.of();
        }
    }

    private AddressData mapAddress(UserAddress ua) {
        Address a = ua.getAddress();
        return new AddressData(
                ua.getPublicId(),
                ua.getAddressType() != null ? ua.getAddressType().name() : null,
                ua.getLabel(),
                ua.isDefault(),
                a.getStreetNumber(),
                a.getStreetName(),
                a.getComplement(),
                a.getPostalCode(),
                a.getCity(),
                a.getStateProvince(),
                a.getCountryCode(),
                a.getFormattedAddress(),
                ts(ua.getValidFrom()),
                ts(ua.getValidTo())
        );
    }

    // =========================================================================
    // Devices
    // =========================================================================

    private List<DeviceData> collectDevices(User user) {
        try {
            return deviceRepository.findByUser(user).stream()
                    .map(this::mapDevice)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to collect devices for user {}: {}", user.getUsername(), e.getMessage());
            return List.of();
        }
    }

    private DeviceData mapDevice(Device d) {
        return new DeviceData(
                d.getPublicId(),
                d.getName(),
                d.getBrowser(),
                d.getOperatingSystem(),
                d.getDeviceType(),
                d.getDeviceTrustLevel() != null ? d.getDeviceTrustLevel().name() : null,
                d.getLastIpAddress(),
                ts(d.getLastSeen()),
                ts(d.getCreatedAt()),
                d.isLoggedOut()
        );
    }

    // =========================================================================
    // Activity logs
    // =========================================================================

    private List<ActivityData> collectActivity(User user) {
        try {
            // Requires: List<ActivityLog> findByUserOrderByTimestampDesc(User user);
            // in ActivityLogRepository — add it if not present.
            return activityLogRepository.findByUserOrderByTimestampDesc(user).stream()
                    .map(this::mapActivity)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to collect activity logs for user {}: {}", user.getUsername(), e.getMessage());
            return List.of();
        }
    }

    private ActivityData mapActivity(ActivityLog al) {
        return new ActivityData(
                al.getPublicId(),
                al.getActionType(),
                al.getActionCategory(),
                al.isSuccessful(),
                al.getFailureReason(),
                al.getActionDetails(),
                al.getIpAddress(),
                al.getLocation(),
                ts(al.getTimestamp())
        );
    }

    // =========================================================================
    // Notifications
    // =========================================================================

    private List<NotificationData> collectNotifications(User user) {
        try {
            // findAllByRecipient — uses recipient User entity (no pagination for export)
            return notificationRepository.findAllByRecipient(user).stream()
                    .map(this::mapNotification)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to collect notifications for user {}: {}", user.getUsername(), e.getMessage());
            return List.of();
        }
    }

    private NotificationData mapNotification(Notification n) {
        return new NotificationData(
                n.getPublicId(),
                n.getType() != null ? n.getType().name() : null,
                n.getPriority() != null ? n.getPriority().name() : null,
                n.getTitle(),
                n.getBody(),
                n.isRead(),
                n.isDismissed(),
                ts(n.getCreatedAt()),
                ts(n.getReadAt())
        );
    }

    // =========================================================================
    // Calendar
    // =========================================================================

    private List<CalendarData> collectCalendar(User user) {
        try {
            return calendarEventRepository.findByOwnerPublicId(user.getPublicId()).stream()
                    .map(this::mapCalendar)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to collect calendar events for user {}: {}", user.getUsername(), e.getMessage());
            return List.of();
        }
    }

    private CalendarData mapCalendar(CalendarEvent e) {
        return new CalendarData(
                e.getPublicId(),
                e.getTitle(),
                e.getDescription(),
                e.getLocation(),
                e.getStatus() != null ? e.getStatus().name() : null,
                e.getRecurrence() != null ? e.getRecurrence().name() : null,
                ts(e.getStartDateTime()),
                ts(e.getEndDateTime()),
                e.isAllDay(),
                ts(e.getCreatedAt())
        );
    }

    // =========================================================================
    // Sport
    // =========================================================================

    private List<SportData> collectSport(User user) {
        try {
            return sportTrackRepository.findByOwnerPublicIdOrderByStartedAtDesc(user.getPublicId()).stream()
                    .map(this::mapSport)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to collect sport tracks for user {}: {}", user.getUsername(), e.getMessage());
            return List.of();
        }
    }

    private SportData mapSport(SportTrack st) {
        return new SportData(
                st.getPublicId(),
                st.getName(),
                st.getSportType() != null ? st.getSportType().name() : null,
                st.getDistanceKm(),
                st.getDurationSeconds(),
                st.getAvgSpeedKmh(),
                st.getMaxSpeedKmh(),
                st.getElevationGain(),
                ts(st.getStartedAt()),
                ts(st.getEndedAt())
        );
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Null-safe Instant to ISO-8601 string. */
    private String ts(Instant instant) {
        return instant != null ? instant.toString() : null;
    }
}
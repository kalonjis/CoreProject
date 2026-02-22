package be.steby.CoreProject.dl.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Audit log entry for any user action across all domains.
 *
 * <p>Each row captures who did what, from which device, and optionally
 * which device was targeted. The {@code device} field is the actor device
 * (the session that triggered the action); {@code targetDevice} is only
 * populated when a second device is involved (e.g. remote disconnection,
 * trust level change).</p>
 */
@Entity
@Table(name = "activity_log", indexes = {
        @Index(name = "idx_log_public_id", columnList = "public_id"),
        @Index(name = "idx_log_user_timestamp", columnList = "user_id, timestamp"),
        @Index(name = "idx_log_action_type", columnList = "action_type"),
        @Index(name = "idx_log_action_category", columnList = "action_category")
})
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ActivityLog extends BaseEntity<Long> {

    /** User who performed the action; null for system-level security events. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Device from which the action was initiated; null for system or token-based events. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    /** Device targeted by the action; null when the action does not involve a second device. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_device_id")
    private Device targetDevice;

    /** Enum constant name identifying the action — e.g. {@code "LOGIN"}, {@code "DEVICE_CONFIRMED"}. */
    @Column(name = "action_type", length = 50, nullable = false)
    private String actionType;

    /** Domain group the action belongs to — e.g. {@code "AUTH"}, {@code "DEVICE"}, {@code "ACCOUNT"}. */
    @Column(name = "action_category", length = 20, nullable = false)
    private String actionCategory;

    /** UTC timestamp of when the action occurred. */
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    /** Whether the action completed successfully. */
    @Column(nullable = false)
    private boolean successful;

    /** Human-readable reason populated when {@code successful} is false; null otherwise. */
    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    /** Optional contextual details about the action — e.g. trust level transition {@code "BASIC → TRUSTED"}. */
    @Column(name = "action_details", length = 500)
    private String actionDetails;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "location", length = 255)
    private String location;

    /** Returns a builder with {@code timestamp} pre-set to now. */
    public static ActivityLogBuilder builderWithTimestamp() {
        return ActivityLog.builder().timestamp(Instant.now());
    }
}
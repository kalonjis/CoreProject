package be.steby.CoreProject.dl.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Activity Log Entity - KISS Version
 * Stores user actions with minimal fields: who + what + where + when
 */
@Entity
@Table(name = "activity_log", indexes = {
        @Index(name = "idx_log_user_timestamp", columnList = "user_id, timestamp"),
        @Index(name = "idx_log_action_type", columnList = "action_type"),
        @Index(name = "idx_log_action_category", columnList = "action_category"),
        @Index(name = "idx_log_device_timestamp", columnList = "device_id, timestamp"),
        @Index(name = "idx_log_successful", columnList = "successful")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivityLog extends BaseEntity<Long> {

    // ================== CORE FIELDS (who, what, where, when) ==================

    /**
     * User who performed the action (WHO)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Device used for the action (WHERE) - contains IP, UserAgent, etc.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    /**
     * Action name (WHAT) - e.g., "LOGIN", "PASSWORD_RESET_REQUEST"
     */
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    /**
     * Action category/domain (WHAT) - e.g., "AUTH", "PASSWORD", "EMAIL"
     */
    @Column(name = "action_category", nullable = false, length = 20)
    private String actionCategory;

    /**
     * When the action occurred (WHEN)
     */
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    /**
     * Whether the action was successful
     */
    @Column(nullable = false)
    private boolean successful;

    // ================== MINIMAL CONTEXT ==================

    /**
     * Additional action details (optional)
     */
    @Column(length = 1000)
    private String details;

    /**
     * Reason for failure (if unsuccessful)
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    // ================== BUILDER PATTERN EXTENSION ==================

    /**
     * Builder with pre-filled timestamp
     */
    public static ActivityLogBuilder builderWithTimestamp() {
        return ActivityLog.builder().timestamp(Instant.now());
    }
}
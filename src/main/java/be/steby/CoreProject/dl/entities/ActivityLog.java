package be.steby.CoreProject.dl.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Activity Log Entity - Pure Database Persistence
 * Stores user actions with minimal DB-focused fields only.
 * All business logic is handled in the sealed ActionLogType classes.
 */
@Entity
@Table(name = "activity_log", indexes = {
        @Index(name = "idx_log_user_timestamp", columnList = "user_id, timestamp"),
        @Index(name = "idx_log_action_type", columnList = "action_type"),
        @Index(name = "idx_log_action_category", columnList = "action_category"),
        @Index(name = "idx_log_ip_address", columnList = "ip_address"),
        @Index(name = "idx_log_risk_level", columnList = "risk_level"),
        @Index(name = "idx_log_successful", columnList = "successful")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivityLog extends BaseEntity<Long> {

    // ================== CORE BUSINESS FIELDS ==================

    /**
     * User who performed the action
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Device used for the action (optional)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    /**
     * When the action occurred
     */
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    /**
     * Whether the action was successful
     */
    @Column(nullable = false)
    private boolean successful;

    // ================== ACTION IDENTIFICATION (FOR SEALED CLASSES) ==================

    /**
     * Action name (e.g., "LOGIN", "USER_CREATED")
     * Used to reconstruct the proper sealed type via ActionLogType.fromString()
     */
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    /**
     * Action category/domain (e.g., "AUTH", "ADMIN", "ACCOUNT")
     * Used with actionType to reconstruct the proper sealed type
     */
    @Column(name = "action_category", nullable = false, length = 20)
    private String actionCategory;

    // ================== CONTEXTUAL INFORMATION ==================

    /**
     * IP address where the action originated
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Geographic location (city, country, etc.)
     */
    @Column(length = 255)
    private String location;

    /**
     * User agent string
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Session identifier
     */
    @Column(name = "session_id", length = 255)
    private String sessionId;

    // ================== BUSINESS CONTEXT ==================

    /**
     * Reason for failure (if unsuccessful)
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * Additional action details
     */
    @Column(name = "action_details", columnDefinition = "TEXT")
    private String actionDetails;

    /**
     * Risk level (0=very low, 1=low, 2=medium, 3=high, 4=critical)
     */
    @Column(name = "risk_level")
    private Integer riskLevel;

    /**
     * Duration in seconds (for timed actions)
     */
    @Column(name = "duration_seconds")
    private Long durationSeconds;

    /**
     * Additional metadata as JSON
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    // ================== SECURITY & MONITORING ==================

    /**
     * Whether this action triggered an alert
     */
    @Column(name = "triggered_alert")
    private Boolean triggeredAlert;

    /**
     * Error details (for debugging)
     */
    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    // ================== COMPLIANCE & AUDIT ==================

    /**
     * Compliance category for audit purposes
     */
    @Column(name = "compliance_category", length = 50)
    private String complianceCategory;

    /**
     * External audit trail reference
     */
    @Column(name = "audit_reference", length = 100)
    private String auditReference;

    // ================== BUILDER PATTERN EXTENSIONS ==================

    /**
     * Builder with pre-filled timestamp
     */
    public static ActivityLogBuilder builderWithTimestamp() {
        return ActivityLog.builder().timestamp(Instant.now());
    }
}
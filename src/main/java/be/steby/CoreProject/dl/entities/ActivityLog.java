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
     * Used to reconstruct the proper sealed type
     */
    @Column(name = "action_type", length = 50, nullable = false)
    private String actionType;

    /**
     * Action category/domain (e.g., "AUTH", "ADMIN")
     * Used for fast domain-based queries and sealed type reconstruction
     */
    @Column(name = "action_category", length = 20, nullable = false)
    private String actionCategory;

    // ================== CONTEXTUAL INFORMATION ==================

    /**
     * IP address where action originated
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * Geographic location (city, country)
     */
    @Column(name = "location", length = 100)
    private String location;

    /**
     * Session identifier to group related actions
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * User agent string
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // ================== FAILURE & ERROR HANDLING ==================

    /**
     * Reason for failure (if not successful)
     */
    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    /**
     * Additional error details or stack trace
     */
    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    // ================== METADATA & CONTEXT ==================

    /**
     * Additional action-specific metadata in JSON format
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * Human-readable action details
     */
    @Column(name = "action_details", length = 255)
    private String actionDetails;

    // ================== SECURITY & RISK ASSESSMENT ==================

    /**
     * Risk level of the action:
     * 0 = Normal, 1 = Low risk, 2 = Medium risk, 3 = High risk, 4 = Critical
     */
    @Column(name = "risk_level")
    private Integer riskLevel;

    /**
     * Whether this action triggered a security alert
     */
    @Column(name = "triggered_alert")
    private Boolean triggeredAlert;

    /**
     * Security flags (bitmask for various security conditions)
     */
    @Column(name = "security_flags")
    private Integer securityFlags;

    // ================== PERFORMANCE & DURATION ==================

    /**
     * Duration of the action in seconds (for long-running operations)
     */
    @Column(name = "duration_seconds")
    private Long durationSeconds;

    /**
     * Processing time in milliseconds
     */
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    // ================== COMPLIANCE & AUDIT ==================

    /**
     * Whether this action requires compliance logging
     */
    @Column(name = "requires_compliance")
    private Boolean requiresCompliance;

    /**
     * Compliance category (GDPR, HIPAA, SOX, etc.)
     */
    @Column(name = "compliance_category", length = 50)
    private String complianceCategory;

    /**
     * External audit trail reference
     */
    @Column(name = "audit_reference", length = 100)
    private String auditReference;


    // ================== MINIMAL HELPER METHODS (DB FOCUSED ONLY) ==================

    /**
     * Quick check if action was successful
     */
    public boolean wasSuccessful() {
        return successful;
    }

    /**
     * Quick check if action failed
     */
    public boolean wasFailed() {
        return !successful;
    }

    /**
     * Quick check if action is high risk
     */
    public boolean isHighRisk() {
        return riskLevel != null && riskLevel >= 3;
    }

    /**
     * Quick check if action triggered alert
     */
    public boolean hasTriggeredAlert() {
        return Boolean.TRUE.equals(triggeredAlert);
    }

    /**
     * Quick check if action has error details
     */
    public boolean hasErrorDetails() {
        return errorDetails != null && !errorDetails.trim().isEmpty();
    }

    /**
     * Quick check if action has metadata
     */
    public boolean hasMetadata() {
        return metadata != null && !metadata.trim().isEmpty();
    }

    // ================== BUILDER PATTERN EXTENSIONS ==================

    /**
     * Builder with pre-filled timestamp
     */
    public static ActivityLogBuilder builderWithTimestamp() {
        return ActivityLog.builder().timestamp(Instant.now());
    }

    /**
     * Builder for successful action
     */
    public static ActivityLogBuilder builderForSuccess() {
        return builderWithTimestamp().successful(true).riskLevel(0);
    }

    /**
     * Builder for failed action
     */
    public static ActivityLogBuilder builderForFailure() {
        return builderWithTimestamp().successful(false).riskLevel(2);
    }

    /**
     * Builder for high-risk action
     */
    public static ActivityLogBuilder builderForHighRisk() {
        return builderWithTimestamp().riskLevel(3).triggeredAlert(true);
    }
}
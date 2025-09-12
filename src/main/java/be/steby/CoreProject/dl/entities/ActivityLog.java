package be.steby.CoreProject.dl.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "activity_log", indexes = {
        @Index(name = "idx_log_user_timestamp", columnList = "user_id, timestamp"),
        @Index(name = "idx_log_action_type", columnList = "action_type"),
        @Index(name = "idx_log_action_category", columnList = "action_category")
})
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ActivityLog extends BaseEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(name = "action_type", length = 50, nullable = false)
    private String actionType;

    @Column(name = "action_category", length = 20, nullable = false)
    private String actionCategory;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private boolean successful;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "action_details", length = 500)
    private String actionDetails;

    /**
     * Builder with timestamp automatically set to now
     */
    public static ActivityLogBuilder builderWithTimestamp() {
        return ActivityLog.builder().timestamp(Instant.now());
    }
}
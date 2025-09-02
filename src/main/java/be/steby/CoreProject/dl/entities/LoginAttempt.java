package be.steby.CoreProject.dl.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entity for tracking failed login attempts to implement brute force protection.
 * Stores attempts by username, IP address, or combination of both.
 */
@Entity
@Table(name = "login_attempt",
        indexes = {
                @Index(name = "idx_login_attempt_username", columnList = "username"),
                @Index(name = "idx_login_attempt_ip", columnList = "ip_address"),
                @Index(name = "idx_login_attempt_combo", columnList = "username, ip_address")
        })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The username involved in the failed attempt (nullable for IP-only tracking)
     */
    @Column(name = "username", length = 50)
    private String username;

    /**
     * The IP address involved in the failed attempt (nullable for username-only tracking)
     */
    @Column(name = "ip_address", length = 45) // IPv6 compatible
    private String ipAddress;

    /**
     * Type of tracking: "username", "ip", or "combined"
     */
    @Column(name = "attempt_type", nullable = false, length = 20)
    private String attemptType;

    /**
     * Current number of failed attempts
     */
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    /**
     * Timestamp of the first failed attempt in current sequence
     */
    @Column(name = "first_attempt_time")
    private Instant firstAttemptTime;

    /**
     * Timestamp of the most recent failed attempt
     */
    @Column(name = "last_attempt_time")
    private Instant lastAttemptTime;

    /**
     * Timestamp when blocking expires (null if not blocked)
     */
    @Column(name = "blocked_until")
    private Instant blockedUntil;

    /**
     * Whether this entry is currently in blocked state
     */
    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked = false;

    public LoginAttempt(String username, String ipAddress, String attemptType) {
        this.username = username;
        this.ipAddress = ipAddress;
        this.attemptType = attemptType;
        this.attemptCount = 0;
        this.isBlocked = false;
    }
}
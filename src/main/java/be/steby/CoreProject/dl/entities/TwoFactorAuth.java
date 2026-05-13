package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.TwoFactorType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity representing a two-factor authentication configuration for a user.
 * 
 * A user can have multiple 2FA methods enabled simultaneously:
 * - One primary method (TOTP, Email, SMS, or WebAuthn)
 * - Optional backup methods
 * - Backup codes for recovery
 * 
 * Security considerations:
 * - The 'secret' field MUST be encrypted at rest using SecureTokenService
 * - Backup codes MUST be hashed before storage (like passwords)
 * - Failed verification attempts should be rate-limited
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@Entity
@Table(
    name = "two_factor_auth",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_type", 
            columnNames = {"user_id", "type"}
        )
    },
    indexes = {
        @Index(name = "idx_tfa_user_enabled", columnList = "user_id, enabled"),
        @Index(name = "idx_type", columnList = "type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorAuth extends BaseEntity<Long> {
    
    /**
     * User who owns this 2FA configuration
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Type of 2FA method
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TwoFactorType type;
    
    /**
     * Encrypted secret for TOTP method.
     * NULL for other methods (SMS, EMAIL, BACKUP_CODES, WEBAUTHN).
     * 
     * SECURITY: This value MUST be encrypted using SecureTokenService before storage.
     * Format: "sec_[encrypted_base32_secret]"
     */
    @Column(length = 500)
    private String secret;
    
    /**
     * Phone number for SMS method (encrypted).
     * NULL for other methods.
     */
    @Column(length = 200)
    private String phoneNumber;
    
    /**
     * Email address for EMAIL method (encrypted if different from user's primary email).
     * NULL for other methods.
     */
    @Column(length = 200)
    private String emailAddress;
    
    /**
     * Hashed backup codes (JSON array of hashed codes).
     * Format: ["hash1", "hash2", ..., "hash10"]
     * Each code is bcrypt-hashed like a password.
     * NULL until backup codes are generated.
     */
    @Column(length = 2000)
    private String backupCodesHashed;
    
    /**
     * Number of unused backup codes remaining
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer remainingBackupCodes = 0;
    
    /**
     * Whether this 2FA method is currently active
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = false;
    
    /**
     * Whether this is the primary 2FA method for the user
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;
    
    /**
     * Timestamp when this method was first enabled
     */
    @Column
    private Instant enabledAt;

    /**
     * Timestamp when this method was last disabled
     */
    @Column
    private Instant disabledAt;
    
    /**
     * Timestamp when this method was last verified successfully
     */
    @Column
    private Instant lastVerifiedAt;
    
    /**
     * Number of failed verification attempts (for rate limiting)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer failedAttempts = 0;
    
    /**
     * Timestamp when the user is locked out due to too many failed attempts
     */
    @Column
    private Instant lockedUntil;
    
    /**
     * User-friendly label for this 2FA method (e.g., "Work Phone", "Personal Authenticator")
     */
    @Column(length = 100)
    private String label;
    
    // ========================================
    // Business Logic Helper Methods
    // ========================================
    
    /**
     * Check if this 2FA method is currently locked due to failed attempts
     */
    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }
    
    /**
     * Record a successful verification
     */
    public void recordSuccessfulVerification() {
        this.lastVerifiedAt = Instant.now();
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }
    
    /**
     * Record a failed verification attempt
     */
    public void recordFailedAttempt() {
        this.failedAttempts++;
    }

}
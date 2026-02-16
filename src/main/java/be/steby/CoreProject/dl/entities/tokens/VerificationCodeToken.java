package be.steby.CoreProject.dl.entities.tokens;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <p>Represents a token entity used for SMS-based password reset operations in the application.</p>
 *
 * <p>This class extends the {@link BaseToken} class, inheriting its core functionality,
 * such as token management and linkage to a user entity. It adds SMS-specific functionality
 * for storing and validating verification codes sent via SMS.</p>
 *
 * <h4>Database Mapping:</h4>
 * <ul>
 *   <li>This entity will inherit fields and relationships defined in {@link BaseToken}.</li>
 *   <li>Mapped to a database table (by default, named `sms_password_reset_token`).</li>
 *   <li>Contains additional field for hashed verification code storage.</li>
 * </ul>
 *
 * <h4>Security Features:</h4>
 * <ul>
 *   <li>Verification codes are stored as hashed values for security</li>
 *   <li>Integrates with rate limiting mechanisms</li>
 *   <li>Supports automatic cleanup of expired tokens</li>
 *   <li>Can be extended for 2FA SMS authentication</li>
 * </ul>
 *
 * <h4>Usage:</h4>
 * <p>This entity is used during the SMS password reset process, where:</p>
 * <ol>
 *   <li>A 6-digit verification code is generated and sent via SMS</li>
 *   <li>The hashed code is stored in this token entity</li>
 *   <li>User provides the code for validation</li>
 *   <li>Upon successful validation, password reset is permitted</li>
 * </ol>
 *
 * @see BaseToken
 * @see be.steby.CoreProject.dl.entities.tokens.enums.TokenType#SMS_PASSWORD_RESET
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VerificationCodeToken extends BaseToken {

    /**
     * The hashed verification code sent to the user via SMS.
     * 
     * <p>This field stores the securely hashed version of the 6-digit verification code
     * that was sent to the user's phone number. The original code is never stored in 
     * plain text for security purposes.</p>
     * 
     * <p><strong>Security Note:</strong> This field should always contain a hashed value
     * using a secure hashing algorithm (e.g., BCrypt) and should never store the 
     * original verification code in plain text.</p>
     * 
     * <p><strong>Validation Process:</strong> When validating a user-provided code,
     * the provided code should be hashed using the same algorithm and compared
     * against this stored hash.</p>
     */
    @Column(name = "verification_code_hash", length = 60)
    private String verificationCodeHash;

    /**
     * Constructor for creating a new SMS password reset token with verification code.
     * 
     * @param verificationCodeHash the hashed verification code to store
     */
    public VerificationCodeToken(String verificationCodeHash) {
        this.verificationCodeHash = verificationCodeHash;
    }
}
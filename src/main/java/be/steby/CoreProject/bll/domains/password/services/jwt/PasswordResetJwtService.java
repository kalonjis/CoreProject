package be.steby.CoreProject.bll.domains.password.services.jwt;

import be.steby.CoreProject.bll.common.jwt.BaseJwtService;
import be.steby.CoreProject.bll.common.jwt.JwtTokenConfig;
import be.steby.CoreProject.bll.domains.password.exceptions.InvalidPasswordResetTokenException;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * JWT service specialized for password reset token management.
 *
 * <p>This service handles the generation and validation of JWT tokens used during
 * the password reset process. It manages multiple types of tokens used in different
 * phases of the password reset flow to provide a secure, multi-step verification process.</p>
 *
 * <p>The password reset flow typically works as follows:</p>
 * <ol>
 *   <li>User requests password reset via email</li>
 *   <li>System generates SMS reference token and sends to user's registered phone</li>
 *   <li>User provides SMS reference token and receives verification code</li>
 *   <li>System generates SMS verification token containing hashed code</li>
 *   <li>User provides verification code for validation</li>
 *   <li>System generates permission token allowing actual password reset</li>
 *   <li>User can reset password using permission token</li>
 * </ol>
 *
 * <p>Token types managed by this service:</p>
 * <ul>
 *   <li><strong>SMS Reference Token</strong>: References database token for SMS delivery</li>
 *   <li><strong>SMS Verification Token</strong>: Contains hashed verification code for validation</li>
 *   <li><strong>Permission Token</strong>: Grants authorization to perform password reset</li>
 * </ul>
 *
 * <p>Security features:</p>
 * <ul>
 *   <li>Multi-step verification process with different token types</li>
 *   <li>Configurable expiration times for different security levels</li>
 *   <li>Email-based user identification throughout the flow</li>
 *   <li>Verification codes are hashed before storage in tokens</li>
 *   <li>Purpose-based validation prevents token misuse</li>
 * </ul>
 *
 * @author Steby Team
 * @since 2.0.0
 * @see BaseJwtService
 * @see be.steby.CoreProject.bll.domains.password.services.PasswordService
 */
@Service
public class PasswordResetJwtService extends BaseJwtService {

    /**
     * Token purpose for SMS reference tokens that link to database tokens.
     */
    private static final String SMS_REFERENCE_PURPOSE = "PASSWORD_RESET_SMS_REFERENCE";

    /**
     * Token purpose for SMS verification tokens containing hashed verification codes.
     */
    private static final String SMS_VERIFICATION_PURPOSE = "PASSWORD_RESET_SMS";

    /**
     * Token purpose for permission tokens that grant password reset authorization.
     */
    private static final String PERMISSION_PURPOSE = "PASSWORD_RESET_PERMISSION";

    /**
     * Expiration time for SMS verification tokens in milliseconds (10 minutes).
     * Shorter expiration for security-sensitive verification codes.
     */
    private static final long SMS_VERIFICATION_EXPIRATION = 10 * 60 * 1000L;

    /**
     * Expiration time for permission tokens in milliseconds (15 minutes).
     * Longer expiration to allow user time to complete password reset.
     */
    private static final long PERMISSION_EXPIRATION = 15 * 60 * 1000L;

    /**
     * The expiration time for SMS reference tokens in milliseconds.
     * Configured via application property security.jwt.password-reset.sms-reference-expiration.
     */
    private final long smsReferenceExpiration;

    /**
     * Constructs a new PasswordResetJwtService with the specified configuration.
     *
     * @param secretKey the JWT secret key from application properties
     * @param smsReferenceExpiration the expiration time for SMS reference tokens in milliseconds
     */
    public PasswordResetJwtService(
            @Value("${security.jwt.secret}") String secretKey,
            @Value("${security.jwt.password-reset.sms-reference-expiration:600000}") long smsReferenceExpiration) {
        super(secretKey);
        this.smsReferenceExpiration = smsReferenceExpiration;
    }

    /**
     * Generates an SMS reference token that references a database password reset token.
     *
     * <p>This token is sent to the user's registered phone number and contains a reference
     * to a database token. It's used in the first step of SMS-based password reset where
     * the user needs to provide this token to request a verification code.</p>
     *
     * <p>Token claims include:</p>
     * <ul>
     *   <li>tokenRef: Reference to the database password reset token</li>
     *   <li>purpose: Set to "PASSWORD_RESET_SMS_REFERENCE"</li>
     * </ul>
     *
     * @param email the user's email address (used as subject)
     * @param tokenReference the reference to the database password reset token
     * @return JWT SMS reference token
     * @throws IllegalArgumentException if email or tokenReference is null/empty
     */
    public String generateSmsReferenceToken(String email, String tokenReference) {
        JwtTokenConfig config = JwtTokenConfig.builder()
                .subject(email)
                .expirationTimeMs(smsReferenceExpiration)
                .purpose(SMS_REFERENCE_PURPOSE)
                .claim("tokenRef", tokenReference)
                .build();

        return generateToken(config);
    }

    /**
     * Generates an SMS verification token containing user email and hashed verification code.
     *
     * <p>This token is created when a verification code is sent to the user's phone via SMS.
     * It contains the hashed verification code and user email for validation during the
     * verify-password-reset-sms step.</p>
     *
     * <p>Token claims include:</p>
     * <ul>
     *   <li>email: User's email address for identification</li>
     *   <li>verificationCodeHash: Hashed verification code for validation</li>
     *   <li>purpose: Set to "PASSWORD_RESET_SMS"</li>
     * </ul>
     *
     * <p>This token has a shorter expiration (10 minutes) for enhanced security
     * of the verification code process.</p>
     *
     * @param email the user's email address
     * @param hashedVerificationCode the hashed 6-digit verification code sent via SMS
     * @return JWT SMS verification token
     * @throws IllegalArgumentException if email or hashedVerificationCode is null/empty
     */
    public String generateSmsVerificationToken(String email, String hashedVerificationCode) {
        JwtTokenConfig config = JwtTokenConfig.builder()
                .subject(email)
                .expirationTimeMs(SMS_VERIFICATION_EXPIRATION)
                .purpose(SMS_VERIFICATION_PURPOSE)
                .claim("email", email)
                .claim("verificationCodeHash", hashedVerificationCode)
                .build();

        return generateToken(config);
    }

    /**
     * Generates a permission token granting authorization to reset password.
     *
     * <p>This token is issued after successful SMS code verification and grants
     * the user permission to access the password reset page and actually change
     * their password. It has a longer expiration than verification tokens to give
     * users adequate time to complete the password reset process.</p>
     *
     * <p>Token claims include:</p>
     * <ul>
     *   <li>email: User's email address for identification</li>
     *   <li>grantedAt: Timestamp when permission was granted</li>
     *   <li>purpose: Set to "PASSWORD_RESET_PERMISSION"</li>
     * </ul>
     *
     * @param email the user's email address
     * @return JWT permission token granting password reset authorization
     * @throws IllegalArgumentException if email is null or empty
     */
    public String generatePermissionToken(String email) {
        JwtTokenConfig config = JwtTokenConfig.builder()
                .subject(email)
                .expirationTimeMs(PERMISSION_EXPIRATION)
                .purpose(PERMISSION_PURPOSE)
                .claim("email", email)
                .claim("grantedAt", System.currentTimeMillis())
                .build();

        return generateToken(config);
    }

    /**
     * Validates an SMS reference token and returns its claims.
     *
     * <p>Validates the token signature, expiration, and confirms the purpose is
     * "PASSWORD_RESET_SMS_REFERENCE". Ensures the required tokenRef claim exists
     * and is not empty.</p>
     *
     * @param token the JWT SMS reference token to validate
     * @return the claims contained in the token if validation succeeds
     * @throws InvalidPasswordResetTokenException if token is invalid, expired, wrong purpose, or missing required claims
     */
    public Claims validateSmsReferenceToken(String token) {
        try {
            Claims claims = validateTokenWithPurpose(token, SMS_REFERENCE_PURPOSE);

            String tokenRef = claims.get("tokenRef", String.class);
            if (tokenRef == null || tokenRef.isBlank()) {
                throw new InvalidPasswordResetTokenException("Missing required tokenRef claim in password reset SMS reference token");
            }

            return claims;
        } catch (InvalidPasswordResetTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidPasswordResetTokenException("Invalid or expired password reset SMS reference token: " + e.getMessage(), e);
        }
    }

    /**
     * Validates an SMS verification token and returns its claims.
     *
     * <p>Validates the token signature, expiration, and confirms the purpose is
     * "PASSWORD_RESET_SMS". Ensures the required email and verificationCodeHash
     * claims exist and are not empty.</p>
     *
     * @param token the JWT SMS verification token to validate
     * @return the claims contained in the token if validation succeeds
     * @throws InvalidPasswordResetTokenException if token is invalid, expired, wrong purpose, or missing required claims
     */
    public Claims validateSmsVerificationToken(String token) {
        try {
            Claims claims = validateTokenWithPurpose(token, SMS_VERIFICATION_PURPOSE);

            String email = claims.get("email", String.class);
            String verificationCodeHash = claims.get("verificationCodeHash", String.class);

            if (email == null || email.isBlank()) {
                throw new InvalidPasswordResetTokenException("Missing required email claim in password reset SMS token");
            }

            if (verificationCodeHash == null || verificationCodeHash.isBlank()) {
                throw new InvalidPasswordResetTokenException("Missing required verificationCodeHash claim in password reset SMS token");
            }

            return claims;
        } catch (InvalidPasswordResetTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidPasswordResetTokenException("Invalid or expired password reset SMS token: " + e.getMessage(), e);
        }
    }

    /**
     * Validates a permission token and returns its claims.
     *
     * <p>Validates the token signature, expiration, and confirms the purpose is
     * "PASSWORD_RESET_PERMISSION". Ensures the required email and grantedAt
     * claims exist and are valid.</p>
     *
     * @param token the JWT permission token to validate
     * @return the claims contained in the token if validation succeeds
     * @throws InvalidPasswordResetTokenException if token is invalid, expired, wrong purpose, or missing required claims
     */
    public Claims validatePermissionToken(String token) {
        try {
            Claims claims = validateTokenWithPurpose(token, PERMISSION_PURPOSE);

            String email = claims.get("email", String.class);
            Long grantedAt = claims.get("grantedAt", Long.class);

            if (email == null || email.isBlank()) {
                throw new InvalidPasswordResetTokenException("Missing required email claim in password reset permission token");
            }

            if (grantedAt == null) {
                throw new InvalidPasswordResetTokenException("Missing required grantedAt claim in password reset permission token");
            }

            return claims;
        } catch (InvalidPasswordResetTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidPasswordResetTokenException("Invalid or expired password reset permission token: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if a token is an SMS reference token without throwing exceptions.
     *
     * @param token the JWT token to check
     * @return true if token is a valid SMS reference token, false otherwise
     */
    public boolean isSmsReferenceToken(String token) {
        return hasTokenPurpose(token, SMS_REFERENCE_PURPOSE);
    }

    /**
     * Checks if a token is an SMS verification token without throwing exceptions.
     *
     * @param token the JWT token to check
     * @return true if token is a valid SMS verification token, false otherwise
     */
    public boolean isSmsVerificationToken(String token) {
        return hasTokenPurpose(token, SMS_VERIFICATION_PURPOSE);
    }

    /**
     * Checks if a token is a permission token without throwing exceptions.
     *
     * @param token the JWT token to check
     * @return true if token is a valid permission token, false otherwise
     */
    public boolean isPermissionToken(String token) {
        return hasTokenPurpose(token, PERMISSION_PURPOSE);
    }

    /**
     * Extracts the token reference from a validated SMS reference token.
     *
     * @param token the JWT SMS reference token
     * @return the token reference for database lookup
     * @throws InvalidPasswordResetTokenException if token is invalid or missing tokenRef
     */
    public String extractTokenReference(String token) {
        Claims claims = validateSmsReferenceToken(token);
        return claims.get("tokenRef", String.class);
    }

    /**
     * Extracts the email from a validated password reset token.
     *
     * @param token the JWT password reset token (any type)
     * @return the user's email address
     * @throws InvalidPasswordResetTokenException if token is invalid or missing email
     */
    public String extractEmail(String token) {
        Claims claims = validateToken(token);
        return claims.get("email", String.class);
    }

    /**
     * Extracts the verification code hash from a validated SMS verification token.
     *
     * @param token the JWT SMS verification token
     * @return the hashed verification code for validation
     * @throws InvalidPasswordResetTokenException if token is invalid or missing verification code hash
     */
    public String extractVerificationCodeHash(String token) {
        Claims claims = validateSmsVerificationToken(token);
        return claims.get("verificationCodeHash", String.class);
    }

    /**
     * Extracts the granted timestamp from a validated permission token.
     *
     * @param token the JWT permission token
     * @return the timestamp when permission was granted
     * @throws InvalidPasswordResetTokenException if token is invalid or missing grantedAt
     */
    public Long extractGrantedAt(String token) {
        Claims claims = validatePermissionToken(token);
        return claims.get("grantedAt", Long.class);
    }
}
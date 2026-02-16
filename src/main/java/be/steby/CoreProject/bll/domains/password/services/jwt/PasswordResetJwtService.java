package be.steby.CoreProject.bll.domains.password.services.jwt;

import be.steby.CoreProject.bll.domains.password.exceptions.InvalidPasswordResetTokenException;
import be.steby.CoreProject.il.jwt.JwtCoreService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Domain service for password reset JWT operations.
 *
 * <p>This service handles the generation and validation of all JWT tokens
 * used in the password reset flow. It manages three distinct token types,
 * each serving a specific purpose in the password reset process.
 *
 * <p><b>Token Types:</b>
 * <ul>
 *   <li><b>Code Reference Token:</b> Links cookie session to database VerificationCode token</li>
 *   <li><b>Code Verification Token:</b> Contains email and hashed verification code</li>
 *   <li><b>Permission Token:</b> Grants access to password reset page after verification</li>
 * </ul>
 *
 * <p><b>Password Reset Flow (SMS/Email Code):</b>
 * <ol>
 *   <li>User requests password reset</li>
 *   <li>System generates Code Reference Token (stored in cookie)</li>
 *   <li>Verification code sent via SMS/Email</li>
 *   <li>User submits code, system validates</li>
 *   <li>Permission Token issued (allows password change)</li>
 *   <li>User submits new password with Permission Token</li>
 * </ol>
 *
 * @author Steby Team
 * @since 2.1.0
 * @see JwtCoreService
 */
@Service
@Slf4j
public class PasswordResetJwtService {

    private static final String PURPOSE_CODE_REFERENCE = "PASSWORD_RESET_CODE_REFERENCE";
    private static final String PURPOSE_CODE_VERIFICATION = "PASSWORD_RESET_CODE";
    private static final String PURPOSE_PERMISSION = "PASSWORD_RESET_PERMISSION";

    private final JwtCoreService jwtCoreService;

    @Value("${domains.password.jwt.verification-expiration}")
    private long verificationExpiration;

    @Value("${domains.password.jwt.permission-expiration}")
    private long permissionExpiration;

    public PasswordResetJwtService(JwtCoreService jwtCoreService) {
        this.jwtCoreService = jwtCoreService;
    }

    // =========================================================================
    // Code Reference Token
    // =========================================================================

    /**
     * Generates an Code reference token linking to a database token.
     *
     * <p>This lightweight token is stored in a cookie and references the
     * actual password reset token stored in the database. This pattern
     * allows stateless session management while keeping sensitive data
     * in the database.
     *
     * @param tokenReference UUID reference to the database VerificationCode token
     * @return signed JWT reference token
     */
    public String generateCodeReferenceToken(String tokenReference) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenRef", tokenReference);
        claims.put("purpose", PURPOSE_CODE_REFERENCE);

        String token = jwtCoreService.sign(claims, "password-reset-code", verificationExpiration);

        log.debug("Password reset Code reference token generated");
        return token;
    }

    /**
     * Validates an Code reference token and returns the claims.
     *
     * @param token the reference token to validate
     * @return validated claims containing token reference
     * @throws InvalidPasswordResetTokenException if token is invalid
     */
    public Claims validateCodeReferenceToken(String token) {
        try {
            Claims claims = jwtCoreService.parse(token);

            String purpose = claims.get("purpose", String.class);
            if (!PURPOSE_CODE_REFERENCE.equals(purpose)) {
                throw new InvalidPasswordResetTokenException("Invalid token purpose for password reset Code reference");
            }

            String tokenRef = claims.get("tokenRef", String.class);
            if (tokenRef == null) {
                throw new InvalidPasswordResetTokenException("Missing tokenRef claim in password reset Code reference token");
            }

            log.debug("Password reset Code reference token validated");
            return claims;

        } catch (InvalidPasswordResetTokenException e) {
            throw e;
        } catch (JwtException e) {
            log.warn("Password reset Code reference token validation failed: {}", e.getMessage());
            throw new InvalidPasswordResetTokenException("Invalid or expired password reset Code reference token: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the token reference from an Code reference token.
     *
     * @param token the Code reference token
     * @return the database token reference UUID
     * @throws InvalidPasswordResetTokenException if token is invalid
     */
    public String extractTokenReference(String token) {
        Claims claims = validateCodeReferenceToken(token);
        return claims.get("tokenRef", String.class);
    }

    // =========================================================================
    // Code Verification Token
    // =========================================================================

    /**
     * Generates an Code verification token for password reset.
     *
     * <p>Contains the user's email and hashed verification code for
     * validating the code submitted by the user.
     *
     * @param email                  the user's email address
     * @param hashedVerificationCode BCrypt hash of the verification code
     * @return signed JWT verification token
     */
    public String generateSmsVerificationToken(String email, String hashedVerificationCode) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("verificationCodeHash", hashedVerificationCode);
        claims.put("purpose", PURPOSE_CODE_VERIFICATION);

        String token = jwtCoreService.sign(claims, email, verificationExpiration);

        log.debug("Password reset Code verification token generated for email: {}", maskEmail(email));
        return token;
    }

    /**
     * Validates an Code verification token and returns the claims.
     *
     * @param token the verification token to validate
     * @return validated claims containing email and hashed code
     * @throws InvalidPasswordResetTokenException if token is invalid
     */
    public Claims validateSmsVerificationToken(String token) {
        try {
            Claims claims = jwtCoreService.parse(token);

            String purpose = claims.get("purpose", String.class);
            if (!PURPOSE_CODE_VERIFICATION.equals(purpose)) {
                throw new InvalidPasswordResetTokenException("Invalid token purpose for password reset Code verification");
            }

            String email = claims.get("email", String.class);
            String verificationCodeHash = claims.get("verificationCodeHash", String.class);

            if (email == null || verificationCodeHash == null) {
                throw new InvalidPasswordResetTokenException("Missing required claims in password reset Code token");
            }

            log.debug("Password reset Code verification token validated for email: {}", maskEmail(email));
            return claims;

        } catch (InvalidPasswordResetTokenException e) {
            throw e;
        } catch (JwtException e) {
            log.warn("Password reset Code verification token validation failed: {}", e.getMessage());
            throw new InvalidPasswordResetTokenException("Invalid or expired password reset Code token: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // Permission Token
    // =========================================================================

    /**
     * Generates a permission token granting access to password reset.
     *
     * <p>This token is issued after successful verification code validation
     * and grants the user permission to access the password reset page.
     * It has a longer expiration than verification tokens.
     *
     * @param email the user's email address
     * @return signed JWT permission token
     */
    public String generatePermissionToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("grantedAt", System.currentTimeMillis());
        claims.put("purpose", PURPOSE_PERMISSION);

        String token = jwtCoreService.sign(claims, email, permissionExpiration);

        log.debug("Password reset permission token generated for email: {}", maskEmail(email));
        return token;
    }

    /**
     * Validates a permission token and returns the claims.
     *
     * @param token the permission token to validate
     * @return validated claims containing email and grant timestamp
     * @throws InvalidPasswordResetTokenException if token is invalid
     */
    public Claims validatePermissionToken(String token) {
        try {
            Claims claims = jwtCoreService.parse(token);

            String purpose = claims.get("purpose", String.class);
            if (!PURPOSE_PERMISSION.equals(purpose)) {
                throw new InvalidPasswordResetTokenException("Invalid token purpose for password reset permission");
            }

            String email = claims.get("email", String.class);
            Long grantedAt = claims.get("grantedAt", Long.class);

            if (email == null || grantedAt == null) {
                throw new InvalidPasswordResetTokenException("Missing required claims in password reset permission token");
            }

            log.debug("Password reset permission token validated for email: {}", maskEmail(email));
            return claims;

        } catch (InvalidPasswordResetTokenException e) {
            throw e;
        } catch (JwtException e) {
            log.warn("Password reset permission token validation failed: {}", e.getMessage());
            throw new InvalidPasswordResetTokenException("Invalid or expired password reset permission token: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the email from a permission token.
     *
     * @param token the permission token
     * @return the user's email address
     * @throws InvalidPasswordResetTokenException if token is invalid
     */
    public String extractEmailFromPermissionToken(String token) {
        Claims claims = validatePermissionToken(token);
        return claims.get("email", String.class);
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * Masks email for secure logging.
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***.***";
        }
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        String maskedLocal = local.length() > 2
                ? local.charAt(0) + "***" + local.charAt(local.length() - 1)
                : "***";
        return maskedLocal + "@" + domain;
    }
}
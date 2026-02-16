package be.steby.CoreProject.bll.domains.auth.services.twofactor.jwt;

import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.InvalidTwoFactorTokenException;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.il.jwt.JwtCoreService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Domain service for two-factor authentication JWT operations.
 *
 * <p>This service handles the generation and validation of all JWT tokens
 * used in the 2FA authentication flow. It manages three distinct token types,
 * each serving a specific purpose in the authentication process.
 *
 * <p><b>Token Types:</b>
 * <ul>
 *   <li><b>Session Token:</b> Lightweight token for 2FA method selection phase</li>
 *   <li><b>Verification Token:</b> Full token containing verification code hash</li>
 *   <li><b>Activation Token:</b> Token for TOTP activation flow</li>
 * </ul>
 *
 * <p><b>Token Purposes:</b>
 * <pre>
 * METHOD_SELECTION  → Session token (choose 2FA method)
 * 2FA_VERIFICATION  → Verification token (validate code)
 * ACTIVATION        → Activation token (setup TOTP)
 * </pre>
 *
 * <p><b>Security Notes:</b>
 * <ul>
 *   <li>All tokens have short expiration times (configurable)</li>
 *   <li>Verification codes are stored as hashes, never plain text</li>
 *   <li>Token purpose is validated to prevent misuse</li>
 * </ul>
 *
 * @author Steby Team
 * @since 2.1.0
 * @see JwtCoreService
 */
@Service
@Slf4j
public class TwoFactorJwtService {

    private static final String PURPOSE_METHOD_SELECTION = "METHOD_SELECTION";
    private static final String PURPOSE_2FA_VERIFICATION = "2FA_VERIFICATION";
    private static final String PURPOSE_ACTIVATION = "ACTIVATION";

    private final JwtCoreService jwtCoreService;

    @Value("${domains.twofactor.jwt.expiration}")
    private long tokenExpiration;

    public TwoFactorJwtService(JwtCoreService jwtCoreService) {
        this.jwtCoreService = jwtCoreService;
    }

    // =========================================================================
    // Session Token (Method Selection Phase)
    // =========================================================================

    /**
     * Generates a lightweight session token for 2FA method selection phase.
     *
     * <p>This token is issued after successful credentials verification when
     * the user has multiple 2FA methods enabled and needs to choose one.
     *
     * <p><b>Claims:</b>
     * <ul>
     *   <li>publicId: User's public identifier</li>
     *   <li>purpose: METHOD_SELECTION</li>
     * </ul>
     *
     * @param user the user requiring 2FA method selection
     * @return signed JWT session token
     */
    public String generateSessionToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("publicId", user.getPublicId());
        claims.put("purpose", PURPOSE_METHOD_SELECTION);

        String token = jwtCoreService.sign(claims, user.getPublicId(), tokenExpiration);

        log.debug("2FA session token generated for user: {}", user.getUsername());
        return token;
    }

    /**
     * Validates a 2FA session token and returns the claims.
     *
     * @param token the session token to validate
     * @return validated claims
     * @throws InvalidTwoFactorTokenException if token is invalid or wrong purpose
     */
    public Claims validateSessionToken(String token) {
        Claims claims = parseAndValidate(token);

        String purpose = claims.get("purpose", String.class);
        if (!PURPOSE_METHOD_SELECTION.equals(purpose)) {
            throw new InvalidTwoFactorTokenException("Invalid token purpose for method selection");
        }

        String publicId = claims.get("publicId", String.class);
        if (publicId == null || publicId.isBlank()) {
            throw new InvalidTwoFactorTokenException("Missing publicId in session token");
        }

        log.debug("2FA session token validated for publicId: {}", publicId);
        return claims;
    }

    // =========================================================================
    // Verification Token (Code Verification Phase)
    // =========================================================================

    /**
     * Generates a verification token for 2FA code validation phase.
     *
     * <p>This token is issued after the user selects a 2FA method and contains
     * the hashed verification code for later comparison.
     *
     * <p><b>Claims:</b>
     * <ul>
     *   <li>publicId: User's public identifier</li>
     *   <li>username: User's username</li>
     *   <li>email: User's email address</li>
     *   <li>twoFactorType: Selected 2FA method type</li>
     *   <li>verificationCodeHash: Hashed verification code</li>
     *   <li>purpose: 2FA_VERIFICATION</li>
     * </ul>
     *
     * @param user                 the user undergoing 2FA verification
     * @param verificationCodeHash hashed verification code (null for TOTP/backup codes)
     * @param twoFactorType        the selected 2FA method
     * @return signed JWT verification token
     */
    public String generateVerificationToken(User user, String verificationCodeHash, TwoFactorType twoFactorType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("publicId", user.getPublicId());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("twoFactorType", twoFactorType.name());
        claims.put("verificationCodeHash", verificationCodeHash);
        claims.put("purpose", PURPOSE_2FA_VERIFICATION);

        String token = jwtCoreService.sign(claims, user.getUsername(), tokenExpiration);

        log.debug("2FA verification token generated for user: {}, method: {}",
                user.getUsername(), twoFactorType);
        return token;
    }

    /**
     * Validates a 2FA verification token and returns the claims.
     *
     * @param token the verification token to validate
     * @return validated claims
     * @throws InvalidTwoFactorTokenException if token is invalid or wrong purpose
     */
    public Claims validateVerificationToken(String token) {
        Claims claims = parseAndValidate(token);

        String purpose = claims.get("purpose", String.class);
        if (!PURPOSE_2FA_VERIFICATION.equals(purpose)) {
            throw new InvalidTwoFactorTokenException("Invalid token purpose for 2FA verification");
        }

        // Validate required claims
        if (claims.get("publicId", String.class) == null) {
            throw new InvalidTwoFactorTokenException("Missing publicId in verification token");
        }
        if (claims.get("twoFactorType", String.class) == null) {
            throw new InvalidTwoFactorTokenException("Missing twoFactorType in verification token");
        }

        log.debug("2FA verification token validated for user: {}", claims.getSubject());
        return claims;
    }

    /**
     * Checks if a token is a valid 2FA verification token.
     *
     * @param token the token to check
     * @return true if token is valid and has 2FA_VERIFICATION purpose
     */
    public boolean isVerificationToken(String token) {
        try {
            Claims claims = jwtCoreService.parse(token);
            return PURPOSE_2FA_VERIFICATION.equals(claims.get("purpose", String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    // =========================================================================
    // Activation Token (TOTP Setup Phase)
    // =========================================================================

    /**
     * Generates an activation token for TOTP 2FA setup flow.
     *
     * <p>This token is issued when a user initiates TOTP activation and contains
     * the encrypted secret for later validation.
     *
     * <p><b>Claims:</b>
     * <ul>
     *   <li>publicId: User's public identifier</li>
     *   <li>purpose: ACTIVATION</li>
     *   <li>verificationCode: Encrypted TOTP secret</li>
     * </ul>
     *
     * @param user             the user activating TOTP
     * @param verificationCode the encrypted TOTP secret
     * @return signed JWT activation token
     */
    public String generateActivationToken(User user, String verificationCode) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("publicId", user.getPublicId());
        claims.put("purpose", PURPOSE_ACTIVATION);
        claims.put("verificationCode", verificationCode);

        String token = jwtCoreService.sign(claims, user.getPublicId(), tokenExpiration);

        log.debug("2FA activation token generated for user: {}", user.getUsername());
        return token;
    }

    /**
     * Validates a 2FA activation token and returns the claims.
     *
     * @param token the activation token to validate
     * @return validated claims
     * @throws InvalidTwoFactorTokenException if token is invalid or wrong purpose
     */
    public Claims validateActivationToken(String token) {
        Claims claims = parseAndValidate(token);

        String purpose = claims.get("purpose", String.class);
        if (!PURPOSE_ACTIVATION.equals(purpose)) {
            log.warn("Invalid token purpose for activation: {}", purpose);
            throw new InvalidTwoFactorTokenException("Invalid token purpose for activation");
        }

        String publicId = claims.get("publicId", String.class);
        if (publicId == null || publicId.isBlank()) {
            log.warn("Missing or invalid publicId in activation token");
            throw new InvalidTwoFactorTokenException("Missing publicId in activation token");
        }

        log.debug("2FA activation token validated for publicId: {}", publicId);
        return claims;
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Extracts the public ID from any valid 2FA token.
     *
     * @param token any 2FA token (session, verification, or activation)
     * @return the user's public ID
     * @throws InvalidTwoFactorTokenException if token is invalid
     */
    public String extractPublicId(String token) {
        Claims claims = parseAndValidate(token);
        String publicId = claims.get("publicId", String.class);

        if (publicId == null || publicId.isBlank()) {
            throw new InvalidTwoFactorTokenException("Missing publicId in token");
        }

        return publicId;
    }

    /**
     * Extracts the two-factor type from a verification token.
     *
     * @param token the verification token
     * @return the TwoFactorType from the token
     * @throws InvalidTwoFactorTokenException if token is invalid or not a verification token
     */
    public TwoFactorType extractTwoFactorType(String token) {
        Claims claims = validateVerificationToken(token);
        String typeStr = claims.get("twoFactorType", String.class);

        try {
            return TwoFactorType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            throw new InvalidTwoFactorTokenException("Invalid twoFactorType in token: " + typeStr);
        }
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * Parses and validates a token, converting JWT exceptions to domain exceptions.
     */
    private Claims parseAndValidate(String token) {
        try {
            return jwtCoreService.parse(token);
        } catch (JwtException e) {
            log.warn("2FA token validation failed: {}", e.getMessage());
            throw new InvalidTwoFactorTokenException("Invalid or expired 2FA token", e);
        }
    }
}
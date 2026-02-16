package be.steby.CoreProject.bll.domains.profile.services.phone;

import be.steby.CoreProject.bll.common.exceptions.phone.InvalidPhoneVerificationTokenException;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.jwt.JwtCoreService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Domain service for phone verification JWT operations.
 *
 * <p>This service handles the generation and validation of JWT tokens
 * used in the phone number verification flow. It encapsulates all
 * JWT logic related to phone verification within the profile domain.
 *
 * <p><b>Verification Flow:</b>
 * <ol>
 *   <li>User requests phone verification</li>
 *   <li>System generates verification code and creates JWT token with hashed code</li>
 *   <li>SMS is sent with plain code</li>
 *   <li>User submits code, system validates against hashed code in token</li>
 *   <li>Phone number is marked as verified</li>
 * </ol>
 *
 * <p><b>Token Claims:</b>
 * <ul>
 *   <li>userPublicId: User's public identifier</li>
 *   <li>phoneNumber: The phone number being verified</li>
 *   <li>verificationCodeHash: BCrypt hash of the verification code</li>
 *   <li>purpose: PHONE_VERIFICATION</li>
 * </ul>
 *
 * <p><b>Security Notes:</b>
 * <ul>
 *   <li>Verification code is stored as BCrypt hash, never plain text</li>
 *   <li>Token expiration is configurable (default 10 minutes)</li>
 *   <li>User's public ID is validated to prevent token theft</li>
 * </ul>
 *
 * @author Steby Team
 * @since 2.1.0
 * @see JwtCoreService
 */
@Service
@Slf4j
public class PhoneJwtService {

    private static final String PURPOSE_PHONE_VERIFICATION = "PHONE_VERIFICATION";

    private final JwtCoreService jwtCoreService;

    @Value("${domains.profile.jwt.phone-verification-expiration}")
    private long tokenExpiration;

    public PhoneJwtService(JwtCoreService jwtCoreService) {
        this.jwtCoreService = jwtCoreService;
    }

    /**
     * Generates a phone verification token.
     *
     * <p>Creates a JWT token containing the phone number and hashed verification
     * code for later validation. The token is sent to the client while the plain
     * code is sent via SMS.
     *
     * @param user                 the user requesting phone verification
     * @param phoneNumber          the formatted phone number being verified
     * @param verificationCodeHash BCrypt hash of the verification code
     * @return signed JWT verification token
     */
    public String generateVerificationToken(User user, String phoneNumber, String verificationCodeHash) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userPublicId", user.getPublicId());
        claims.put("phoneNumber", phoneNumber);
        claims.put("verificationCodeHash", verificationCodeHash);
        claims.put("purpose", PURPOSE_PHONE_VERIFICATION);

        String token = jwtCoreService.sign(claims, user.getUsername(), tokenExpiration);

        log.debug("Phone verification token generated for user: {}, phone: {}",
                user.getUsername(), maskPhoneNumber(phoneNumber));
        return token;
    }

    /**
     * Validates a phone verification token and returns the claims.
     *
     * <p>Performs comprehensive validation:
     * <ul>
     *   <li>Signature and expiration check</li>
     *   <li>Purpose validation (must be PHONE_VERIFICATION)</li>
     *   <li>Required claims presence check</li>
     * </ul>
     *
     * @param token the verification token to validate
     * @return validated claims containing phone number and hashed code
     * @throws InvalidPhoneVerificationTokenException if token is invalid
     */
    public Claims validateVerificationToken(String token) {
        try {
            Claims claims = jwtCoreService.parse(token);

            // Validate purpose
            String purpose = claims.get("purpose", String.class);
            if (!PURPOSE_PHONE_VERIFICATION.equals(purpose)) {
                throw new InvalidPhoneVerificationTokenException("Invalid token purpose for phone verification");
            }

            // Validate required claims
            String userPublicId = claims.get("userPublicId", String.class);
            String phoneNumber = claims.get("phoneNumber", String.class);
            String verificationCodeHash = claims.get("verificationCodeHash", String.class);

            if (userPublicId == null || phoneNumber == null || verificationCodeHash == null) {
                throw new InvalidPhoneVerificationTokenException("Missing required claims in phone verification token");
            }

            log.debug("Phone verification token validated for user publicId: {}", userPublicId);
            return claims;

        } catch (InvalidPhoneVerificationTokenException e) {
            throw e;
        } catch (JwtException e) {
            log.warn("Phone verification token validation failed: {}", e.getMessage());
            throw new InvalidPhoneVerificationTokenException("Invalid or expired phone verification token: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the user's public ID from a verification token.
     *
     * @param token the verification token
     * @return the user's public ID
     * @throws InvalidPhoneVerificationTokenException if token is invalid
     */
    public String extractUserPublicId(String token) {
        Claims claims = validateVerificationToken(token);
        return claims.get("userPublicId", String.class);
    }

    /**
     * Extracts the phone number from a verification token.
     *
     * @param token the verification token
     * @return the phone number being verified
     * @throws InvalidPhoneVerificationTokenException if token is invalid
     */
    public String extractPhoneNumber(String token) {
        Claims claims = validateVerificationToken(token);
        return claims.get("phoneNumber", String.class);
    }

    /**
     * Extracts the verification code hash from a verification token.
     *
     * @param token the verification token
     * @return the BCrypt hash of the verification code
     * @throws InvalidPhoneVerificationTokenException if token is invalid
     */
    public String extractVerificationCodeHash(String token) {
        Claims claims = validateVerificationToken(token);
        return claims.get("verificationCodeHash", String.class);
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * Masks phone number for secure logging.
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
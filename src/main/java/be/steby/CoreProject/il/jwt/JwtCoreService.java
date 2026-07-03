package be.steby.CoreProject.il.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * Core JWT infrastructure service providing low-level token operations.
 *
 * <p>This service is the single source of truth for JWT cryptographic operations
 * in the application. It handles only the technical aspects of JWT management:
 * signing tokens and parsing/validating them.
 *
 * <p><b>Design Principles:</b>
 * <ul>
 *   <li><b>Infrastructure Layer Only:</b> Contains no business logic whatsoever</li>
 *   <li><b>Single Responsibility:</b> Only handles cryptographic operations</li>
 *   <li><b>Domain Agnostic:</b> Does not know about Users, Devices, 2FA, etc.</li>
 * </ul>
 *
 * <p><b>Usage Pattern:</b>
 * <pre>{@code
 * // In a domain-specific JWT service (e.g., AuthJwtService)
 * Map<String, Object> claims = Map.of(
 *     "username", user.getUsername(),
 *     "deviceId", device.getId()
 * );
 * String token = jwtCoreService.sign(claims, "user@example.com", 3600000L);
 *
 * // Later, to validate and extract claims
 * Claims parsed = jwtCoreService.parse(token);
 * String username = parsed.get("username", String.class);
 * }</pre>
 *
 * <p><b>Security Notes:</b>
 * <ul>
 *   <li>The secret key must be at least 256 bits for HS256 algorithm</li>
 *   <li>Secret key is injected from configuration and never exposed</li>
 *   <li>All JWT exceptions are logged but re-thrown for domain handling</li>
 * </ul>
 *
 * @author Steby Team
 * @since 2.1.0
 * @see io.jsonwebtoken.Jwts
 */
@Service
@Slf4j
public class JwtCoreService {

    private final SecretKey secretKey;

    /**
     * Constructs the JWT core service with the application's secret key.
     *
     * @param secret the JWT secret key from configuration (minimum 256 bits)
     * @throws IllegalArgumentException if the secret key is too short
     */
    public JwtCoreService(@Value("${security.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        log.info("JwtCoreService initialized with HS256 signing algorithm");
    }

    /**
     * Signs a JWT token with the provided claims and expiration.
     *
     * <p>Creates a new JWT token containing:
     * <ul>
     *   <li>All provided custom claims</li>
     *   <li>Subject (sub) claim set to the provided subject</li>
     *   <li>Issued at (iat) claim set to current time</li>
     *   <li>Expiration (exp) claim calculated from current time + expirationMs</li>
     * </ul>
     *
     * @param claims       custom claims to include in the token payload
     * @param subject      the subject (sub) claim, typically user identifier
     * @param expirationMs token validity duration in milliseconds
     * @return the signed JWT token string
     */
    public String sign(Map<String, Object> claims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        String token = Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();

        log.debug("JWT token signed for subject: {}, expires at: {}", subject, expiration);
        return token;
    }

    /**
     * Parses and validates a JWT token, returning its claims.
     *
     * <p>This method performs the following validations:
     * <ul>
     *   <li>Signature verification using the configured secret key</li>
     *   <li>Expiration check (rejects expired tokens)</li>
     *   <li>Structural validation (proper JWT format)</li>
     * </ul>
     *
     * <p><b>Note:</b> This method only performs technical validation.
     * Business-specific validation (e.g., checking token purpose, user existence)
     * must be handled by the calling domain service.
     *
     * @param token the JWT token string to parse
     * @return the validated claims from the token
     * @throws JwtException if the token is invalid, expired, or malformed
     */
    public Claims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.debug("JWT token parsed successfully for subject: {}", claims.getSubject());
            return claims;

        } catch (JwtException e) {
            log.warn("JWT parsing failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Checks if a token is valid without throwing exceptions.
     *
     * <p>Useful for conditional logic where you need to check validity
     * without exception handling overhead.
     *
     * @param token the JWT token string to validate
     * @return true if the token is valid and not expired, false otherwise
     */
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
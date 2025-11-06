package be.steby.CoreProject.bll.common.jwt;

import be.steby.CoreProject.bll.common.exceptions.InvalidTokenPurposeException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * Base JWT service providing common JWT token operations.
 *
 * <p>This abstract service provides shared functionality for JWT token generation
 * and validation that can be extended by domain-specific JWT services. It handles
 * the core JWT operations including token creation, validation, and purpose-based
 * token verification.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Centralized JWT secret key management</li>
 *   <li>Generic token generation with custom claims and expiration</li>
 *   <li>Token validation with purpose verification</li>
 *   <li>Reusable token utilities for all JWT types</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * @Service
 * public class MyDomainJwtService extends BaseJwtService {
 *     public String generateMyToken(User user) {
 *         JwtTokenConfig config = JwtTokenConfig.builder()
 *             .subject(user.getUsername())
 *             .expirationTimeMs(3600000L)
 *             .purpose("MY_PURPOSE")
 *             .claim("userId", user.getId())
 *             .build();
 *         return generateToken(config);
 *     }
 * }
 * }</pre>
 *
 * @author Steby Team
 * @since 2.0.0
 * @see JwtTokenConfig
 */
@Service
@Getter
public abstract class BaseJwtService {

    /**
     * The secret key used for JWT token signing and verification.
     * Generated from the application property security.jwt.secret.
     */
    protected final SecretKey key;

    /**
     * Constructs a new BaseJwtService with the specified secret key.
     *
     * @param secretKey the JWT secret key from application properties
     * @throws IllegalArgumentException if secretKey is null or too short
     */
    public BaseJwtService(@Value("${security.jwt.secret}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Generates a JWT token with the specified claims, subject, and expiration time.
     *
     * <p>This method creates a JWT token with the provided claims, sets the subject
     * and expiration time, and signs it with the configured secret key using HS256.</p>
     *
     * @param claims the claims to include in the token payload
     * @param subject the subject (typically username or user identifier)
     * @param expirationTimeMs the expiration time in milliseconds from now
     * @return the generated JWT token as a string
     * @throws IllegalArgumentException if any parameter is null or expiration is negative
     */
    protected String generateToken(Map<String, Object> claims, String subject, long expirationTimeMs) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationTimeMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates a JWT token using a configuration object.
     *
     * <p>This is a convenience method that extracts the necessary parameters
     * from a JwtTokenConfig object and delegates to the main generateToken method.</p>
     *
     * @param config the JWT token configuration containing claims, subject, and expiration
     * @return the generated JWT token as a string
     * @throws IllegalArgumentException if config is null or contains invalid parameters
     * @see #generateToken(Map, String, long)
     */
    protected String generateToken(JwtTokenConfig config) {
        return generateToken(config.getClaims(), config.getSubject(), config.getExpirationTimeMs());
    }

    /**
     * Validates a JWT token and returns its claims.
     *
     * <p>This method parses and validates the JWT token using the configured secret key.
     * It verifies the token signature, expiration, and structure.</p>
     *
     * @param token the JWT token to validate
     * @return the claims contained in the token
     * @throws io.jsonwebtoken.JwtException if the token is invalid, expired, or malformed
     * @throws IllegalArgumentException if token is null or empty
     */
    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validates a JWT token and verifies it has the expected purpose.
     *
     * <p>This method first validates the token structure and signature, then
     * checks that the 'purpose' claim matches the expected value. This provides
     * an additional layer of security by ensuring tokens are used for their
     * intended purpose.</p>
     *
     * @param token the JWT token to validate
     * @param expectedPurpose the expected value of the 'purpose' claim
     * @return the claims contained in the token
     * @throws InvalidTokenPurposeException if the token purpose doesn't match
     * @throws io.jsonwebtoken.JwtException if the token is invalid, expired, or malformed
     * @throws IllegalArgumentException if token or expectedPurpose is null/empty
     */
    protected Claims validateTokenWithPurpose(String token, String expectedPurpose) {
        Claims claims = validateToken(token);

        String purpose = claims.get("purpose", String.class);
        if (!expectedPurpose.equals(purpose)) {
            throw new InvalidTokenPurposeException(
                String.format("Invalid token purpose. Expected: %s, Found: %s", expectedPurpose, purpose)
            );
        }

        return claims;
    }

    /**
     * Checks if a token has a specific purpose without throwing exceptions.
     *
     * <p>This method is useful for token type detection. It safely checks
     * if a token contains the expected purpose claim without throwing exceptions
     * for invalid or expired tokens.</p>
     *
     * @param token the JWT token to check
     * @param expectedPurpose the purpose to check for
     * @return true if the token has the specified purpose, false otherwise
     */
    protected boolean hasTokenPurpose(String token, String expectedPurpose) {
        try {
            Claims claims = validateToken(token);
            return expectedPurpose.equals(claims.get("purpose", String.class));
        } catch (Exception e) {
            return false;
        }
    }
}
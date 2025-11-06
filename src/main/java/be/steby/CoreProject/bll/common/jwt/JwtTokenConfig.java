package be.steby.CoreProject.bll.common.jwt;

import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for JWT token generation using the Builder pattern.
 *
 * <p>This class encapsulates all the parameters needed to generate a JWT token,
 * including claims, subject, expiration time, and purpose. It uses Lombok's
 * {@code @Builder} annotation to provide a fluent API for constructing token
 * configurations.</p>
 *
 * <p>The builder pattern makes it easy to handle variable parameters for different
 * types of JWT tokens while maintaining type safety and readability.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * JwtTokenConfig config = JwtTokenConfig.builder()
 *     .subject("john.doe")
 *     .expirationTimeMs(3600000L)
 *     .purpose("ACCESS_TOKEN")
 *     .claim("userId", "123")
 *     .claim("role", "USER")
 *     .build();
 * }</pre>
 *
 * @author Steby Team
 * @since 2.0.0
 * @see BaseJwtService#generateToken(JwtTokenConfig)
 */
@Getter
@Builder
public class JwtTokenConfig {

    /**
     * The claims to include in the JWT token payload.
     * Contains all custom data that will be embedded in the token.
     */
    private final Map<String, Object> claims;

    /**
     * The subject of the JWT token (typically username or user identifier).
     * This is a standard JWT claim that identifies the principal.
     */
    private final String subject;

    /**
     * The expiration time for the token in milliseconds from creation.
     * Determines how long the token will remain valid.
     */
    private final long expirationTimeMs;

    /**
     * The purpose of the token (stored as a claim).
     * Used for token type identification and validation.
     */
    private final String purpose;

    /**
     * Custom builder class to provide enhanced claim management functionality.
     *
     * <p>This builder extends the Lombok-generated builder to provide convenient
     * methods for adding individual claims and setting the purpose.</p>
     */
    public static class JwtTokenConfigBuilder {

        /**
         * The claims map, initialized as empty HashMap.
         */
        private Map<String, Object> claims = new HashMap<>();

        /**
         * Adds a single claim to the token configuration.
         *
         * @param key the claim key
         * @param value the claim value
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if key is null or empty
         */
        public JwtTokenConfigBuilder claim(String key, Object value) {
            this.claims.put(key, value);
            return this;
        }

        /**
         * Adds multiple claims to the token configuration.
         *
         * <p>This method merges the provided claims with any existing claims
         * in the builder. If a claim key already exists, it will be overwritten.</p>
         *
         * @param claims the claims to add
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if claims map is null
         */
        public JwtTokenConfigBuilder claims(Map<String, Object> claims) {
            this.claims.putAll(claims);
            return this;
        }

        /**
         * Sets the purpose of the token.
         *
         * <p>This method sets both the purpose field and adds it as a claim
         * to ensure it's included in the token payload for validation purposes.</p>
         *
         * @param purpose the token purpose identifier
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if purpose is null or empty
         */
        public JwtTokenConfigBuilder purpose(String purpose) {
            this.claims.put("purpose", purpose);
            this.purpose = purpose;
            return this;
        }
    }
}
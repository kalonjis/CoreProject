package be.steby.CoreProject.il.Jwt;

import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidTwoFactorTokenException;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Getter
public class JwtUtil {

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final String accessTokenCookieName; // renommé de cookieName
    private final String refreshTokenCookieName;
    private final long twoFactorTokenExpiration;

    public JwtUtil(
            @Value("${security.jwt.secret}") String secretKey,
            @Value("${security.jwt.access-token.expiration}") long accessTokenExpiration,
            @Value("${security.jwt.access-token.name}") String accessTokenCookieName,
            @Value("${security.jwt.refresh-token.name}") String refreshTokenCookieName,
            @Value("${security.jwt.2fa-token.expiration}") long twoFactorTokenExpiration)
    {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.accessTokenCookieName = accessTokenCookieName;
        this.refreshTokenCookieName = refreshTokenCookieName;
        this.twoFactorTokenExpiration = twoFactorTokenExpiration;
    }

    public String generateAccessToken(User user, Device device) {
        return generateToken(user, device, accessTokenExpiration);
    }



    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    /**
     * Generate a lightweight 2FA session token for method selection phase.
     * Contains only user info without verification code - used before user chooses method.
     *
     * @param user the user requiring 2FA
     * @return JWT token for method selection phase
     */
    public String generate2FASessionToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("publicId", user.getPublicId());
        claims.put("purpose", "METHOD_SELECTION");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getPublicId()) // publicId comme subject aussi
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + twoFactorTokenExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }


    /**
     * Validate 2FA session token (method selection phase).
     *
     * @param token the JWT token to validate
     * @return Claims if token is valid
     * @throws InvalidTwoFactorTokenException if token is invalid or wrong purpose
     */
    public Claims validate2FASessionToken(String token) {
        Claims claims = validateToken(token);

        String purpose = claims.get("purpose", String.class);
        if (!"METHOD_SELECTION".equals(purpose)) {
            throw new InvalidTwoFactorTokenException("Invalid token purpose for method selection");
        }

        // Verify that publicId claim exists
        String publicId = claims.get("publicId", String.class);
        if (publicId == null || publicId.isBlank()) {
            throw new InvalidTwoFactorTokenException("Missing publicId in session token");
        }

        return claims;
    }


    /**
     * Generates a 2FA JWT token containing user info and hashed verification code.
     * Used during the 2FA authentication flow between initiate-login and verify-2fa.
     *
     * @param user User entity
     * @param verificationCodeHash Hashed verification code (for validation)
     * @param twoFactorType Type of 2FA being used
     * @return JWT token string
     */
    public String generate2FAToken(User user, String verificationCodeHash, TwoFactorType twoFactorType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("twoFactorType", twoFactorType.name());
        claims.put("verificationCodeHash", verificationCodeHash);
        claims.put("purpose", "2FA_VERIFICATION");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + twoFactorTokenExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates and extracts claims from 2FA JWT token.
     *
     * @param token 2FA JWT token
     * @return Claims containing user info and verification data
     * @throws RuntimeException if token is invalid or expired
     */
    public Claims validate2FAToken(String token) {
        Claims claims = validateToken(token);  // uses existing method

        // Additional validation for 2FA tokens
        String purpose = claims.get("purpose", String.class);
        if (!"2FA_VERIFICATION".equals(purpose)) {
            throw new RuntimeException("Invalid token purpose");
        }

        return claims;
    }

    /**
     * Checks if a JWT token is a 2FA token.
     *
     * @param token JWT token to check
     * @return true if token is for 2FA verification
     */
    public boolean is2FAToken(String token) {
        try {
            Claims claims = validateToken(token);
            return "2FA_VERIFICATION".equals(claims.get("purpose", String.class));
        } catch (Exception e) {
            return false;
        }
    }


    private String generateToken(User user, Device device, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", user.getUsername());
        claims.put("mustChangePassword", user.isMustChangePassword());
        claims.put("roles", user.getAuthorities());

        // Ajouter les informations du Device
        claims.put("deviceId", device.getId());
        claims.put("deviceFingerprint", device.getFingerprint());
//        claims.put("deviceTrustLevel", device.getDeviceTrustLevel().name());
//        claims.put("deviceConfirmed", device.isConfirmed());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

}
package be.steby.CoreProject.il.Jwt;

import be.steby.CoreProject.bll.common.exceptions.phone.InvalidPhoneVerificationTokenException;
import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidTwoFactorTokenException;
import be.steby.CoreProject.bll.domains.password.exceptions.InvalidPasswordResetTokenException;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Getter
@Slf4j
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
     * Generates a JWT token for two-factor authentication activation process.
     *
     * This token contains:
     * - User's public ID for identification
     * - Purpose set to "ACTIVATION" for validation
     * - Verification code for later validation
     * - Expiration time based on configured duration
     *
     * @param user the user for whom the activation token is generated
     * @param verificationCode the verification code to embed in the token
     * @return String containing the signed JWT activation token
     */
    public String generate2FAActivationToken(User user, String verificationCode) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("publicId", user.getPublicId());
        claims.put("purpose", "ACTIVATION");
        claims.put("verificationCode", verificationCode);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getPublicId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + twoFactorTokenExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates a two-factor authentication activation token and extracts claims.
     *
     * This method performs comprehensive validation:
     * 1. Validates the JWT signature and expiration
     * 2. Checks that the token purpose is "ACTIVATION"
     * 3. Ensures the publicId claim is present and valid
     *
     * @param token the JWT activation token to validate
     * @return Claims object containing the validated token claims
     * @throws InvalidTwoFactorTokenException if token is invalid or has wrong purpose
     */
    public Claims validate2FAActivationToken(String token) {
        Claims claims = validateToken(token);

        // Validate token purpose
        String purpose = claims.get("purpose", String.class);
        if (!"ACTIVATION".equals(purpose)) {
            log.warn("Invalid token purpose for activation: {}", purpose);
            throw new InvalidTwoFactorTokenException("Invalid token purpose for activation");
        }

        // Verify that publicId claim exists and is valid
        String publicId = claims.get("publicId", String.class);
        if (publicId == null || publicId.isBlank()) {
            log.warn("Missing or invalid publicId in activation token");
            throw new InvalidTwoFactorTokenException("Missing publicId in activation token");
        }

        return claims;
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
        claims.put("publicId", user.getPublicId());
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


    /**
     * Generates a SMS verification JWT token containing user info and hashed verification code.
     * Used during SMS number verification flow.
     *
     * @param user User entity requesting verification
     * @param phoneNumber Formatted SMS number being verified
     * @param verificationCodeHash Hashed verification code (for validation)
     * @return JWT token string
     */
    public String generatePhoneVerificationToken(User user, String phoneNumber, String verificationCodeHash) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userPublicId", user.getPublicId());
        claims.put("phoneNumber", phoneNumber);
        claims.put("verificationCodeHash", verificationCodeHash);
        claims.put("purpose", "PHONE_VERIFICATION");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + twoFactorTokenExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }


    /**
     * Validates and extracts claims from SMS verification JWT token.
     *
     * @param token the JWT token to validate
     * @return Claims if token is valid
     * @throws InvalidPhoneVerificationTokenException if token is invalid or wrong purpose
     */
    public Claims validatePhoneVerificationToken(String token) {
        try {
            Claims claims = validateToken(token);

            String purpose = claims.get("purpose", String.class);
            if (!"PHONE_VERIFICATION".equals(purpose)) {
                throw new InvalidPhoneVerificationTokenException("Invalid token purpose for SMS verification");
            }

            // Verify that required claims exist
            String userPublicId = claims.get("userPublicId", String.class);
            String phoneNumber = claims.get("phoneNumber", String.class);
            String verificationCodeHash = claims.get("verificationCodeHash", String.class);

            if (userPublicId == null || phoneNumber == null || verificationCodeHash == null) {
                throw new InvalidPhoneVerificationTokenException("Missing required claims in SMS verification token");
            }

            return claims;

        } catch (InvalidPhoneVerificationTokenException e) {
            // Re-throw our custom exception
            throw e;
        } catch (Exception e) {
            // Convert any other JWT exception to our custom exception
            throw new InvalidPhoneVerificationTokenException("Invalid or expired SMS verification token: " + e.getMessage(), e);
        }
    }


    /**
     * Generates a JWT token for password reset SMS reference.
     *
     * <p>Creates a lightweight reference token that links the cookie session
     * to the database SMS token. Used for stateless session management.</p>
     *
     * <p>Pattern: Similar to generatePhoneVerificationToken but for password reset context.</p>
     *
     * @param tokenReference UUID reference to the database SMS token
     * @return JWT token string for password reset SMS reference
     */
    public String generatePasswordResetSmsReferenceToken(String tokenReference) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenRef", tokenReference);
        claims.put("purpose", "PASSWORD_RESET_SMS_REFERENCE");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject("password-reset-sms")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + twoFactorTokenExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates and extracts claims from password reset SMS reference JWT token.
     *
     * <p>Pattern: Similar to validatePhoneVerificationToken but for password reset context.</p>
     *
     * @param token the JWT token to validate
     * @return Claims if token is valid
     * @throws InvalidPasswordResetTokenException if token is invalid or wrong purpose
     */
    public Claims validatePasswordResetSmsReferenceToken(String token) {
        try {
            Claims claims = validateToken(token);

            String purpose = claims.get("purpose", String.class);
            if (!"PASSWORD_RESET_SMS_REFERENCE".equals(purpose)) {
                throw new InvalidPasswordResetTokenException("Invalid token purpose for password reset SMS reference");
            }

            // Verify that required claims exist
            String tokenRef = claims.get("tokenRef", String.class);

            if (tokenRef == null) {
                throw new InvalidPasswordResetTokenException("Missing required tokenRef claim in password reset SMS reference token");
            }

            return claims;

        } catch (InvalidPasswordResetTokenException e) {
            // Re-throw our custom exception
            throw e;
        } catch (Exception e) {
            // Convert any other JWT exception to our custom exception
            throw new InvalidPasswordResetTokenException("Invalid or expired password reset SMS reference token: " + e.getMessage(), e);
        }
    }


    /**
     * Generates a JWT token for password reset SMS verification.
     *
     * <p>Similar to phone verification token but specific to password reset flow.
     * Contains the user's email, hashed verification code, and expiration.
     *
     * @param email the user's email address (identifier)
     * @param hashedVerificationCode the hashed 6-digit verification code
     * @return JWT token for password reset SMS verification
     */
    public String generatePasswordResetSmsToken(String email, String hashedVerificationCode) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("purpose", "PASSWORD_RESET_SMS");
        claims.put("email", email);
        claims.put("verificationCodeHash", hashedVerificationCode);

        // Shorter expiration for password reset (10 minutes vs 15 for phone verification)
        long expiration = 10 * 60 * 1000; // 10 minutes in milliseconds

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates a password reset SMS verification token.
     *
     * <p>Extracts and validates the claims from the JWT token. Ensures the token
     * is specifically for password reset SMS verification and contains all required data.
     *
     * @param token the JWT token to validate
     * @return Claims if token is valid
     * @throws InvalidPasswordResetTokenException if token is invalid or wrong purpose
     */
    public Claims validatePasswordResetSmsToken(String token) {
        try {
            Claims claims = validateToken(token);

            String purpose = claims.get("purpose", String.class);
            if (!"PASSWORD_RESET_SMS".equals(purpose)) {
                throw new InvalidPasswordResetTokenException("Invalid token purpose for password reset SMS verification");
            }

            // Verify that required claims exist
            String email = claims.get("email", String.class);
            String verificationCodeHash = claims.get("verificationCodeHash", String.class);

            if (email == null || verificationCodeHash == null) {
                throw new InvalidPasswordResetTokenException("Missing required claims in password reset SMS token");
            }

            return claims;

        } catch (InvalidPasswordResetTokenException e) {
            // Re-throw our custom exception
            throw e;
        } catch (Exception e) {
            // Convert any other JWT exception to our custom exception
            throw new InvalidPasswordResetTokenException("Invalid or expired password reset SMS token: " + e.getMessage(), e);
        }
    }


    /**
     * Generates a JWT token granting permission to reset password.
     *
     * <p>This token is issued after successful SMS code verification and grants
     * the user permission to access the password reset page. It has a longer
     * expiration than the SMS verification token.
     *
     * @param email the user's email address (identifier)
     * @return JWT token granting password reset permission
     */
    public String generatePasswordResetPermissionToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("purpose", "PASSWORD_RESET_PERMISSION");
        claims.put("email", email);
        claims.put("grantedAt", System.currentTimeMillis());

        // Longer expiration for password reset permission (15 minutes)
        long expiration = 15 * 60 * 1000; // 15 minutes in milliseconds

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates a password reset permission token.
     *
     * <p>Ensures the token grants valid permission to reset password and
     * extracts the user's email for authorization purposes.
     *
     * @param token the JWT permission token to validate
     * @return Claims if token is valid
     * @throws InvalidPasswordResetTokenException if token is invalid or wrong purpose
     */
    public Claims validatePasswordResetPermissionToken(String token) {
        try {
            Claims claims = validateToken(token);

            String purpose = claims.get("purpose", String.class);
            if (!"PASSWORD_RESET_PERMISSION".equals(purpose)) {
                throw new InvalidPasswordResetTokenException("Invalid token purpose for password reset permission");
            }

            // Verify that required claims exist
            String email = claims.get("email", String.class);
            Long grantedAt = claims.get("grantedAt", Long.class);

            if (email == null || grantedAt == null) {
                throw new InvalidPasswordResetTokenException("Missing required claims in password reset permission token");
            }

            return claims;

        } catch (InvalidPasswordResetTokenException e) {
            // Re-throw our custom exception
            throw e;
        } catch (Exception e) {
            // Convert any other JWT exception to our custom exception
            throw new InvalidPasswordResetTokenException("Invalid or expired password reset permission token: " + e.getMessage(), e);
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
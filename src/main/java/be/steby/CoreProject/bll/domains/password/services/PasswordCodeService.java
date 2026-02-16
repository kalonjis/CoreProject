package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.common.services.passwordgenerator.TemporaryPasswordGeneratorService;
import be.steby.CoreProject.bll.domains.password.exceptions.InvalidPasswordResetTokenException;
import be.steby.CoreProject.bll.domains.password.services.jwt.PasswordResetJwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for managing verification codes for password reset operations.
 * 
 * <p>This service handles both SMS and email verification codes, providing
 * a unified approach for code-based password reset (as alternative to email links).
 * 
 * <p>Responsibilities:
 * <ul>
 *   <li>Generate verification codes (delegates to TemporaryPasswordGeneratorService)</li>
 *   <li>Store codes for later validation (SMS: JWT, Email: JWT/DB)</li>
 *   <li>Validate provided codes against stored codes</li>
 * </ul>
 * 
 * <p>SMS codes are stored in JWT tokens (similar to phone verification) with 10-minute expiration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordCodeService {

    private final TemporaryPasswordGeneratorService temporaryPasswordGeneratorService;
    private final PasswordResetJwtService passwordResetJwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Generates a secure verification code for password reset.
     * 
     * <p>Delegates to TemporaryPasswordGeneratorService to avoid code duplication.
     * The code length and type can be customized based on the delivery channel.
     * 
     * @param codeLength the length of the code to generate (typically 6 for SMS, could be 8 for email)
     * @return a secure numeric verification code
     */
    public String generateVerificationCode(int codeLength) {
        log.debug("Generating {}-digit verification code", codeLength);
        return temporaryPasswordGeneratorService.generateNumericCode(codeLength);
    }

    /**
     * Generates a 6-digit code (default for SMS).
     * 
     * @return a 6-digit verification code
     */
    public String generateSmsCode() {
        return generateVerificationCode(6);
    }

    /**
     * Generates an 8-digit code (potential future use for email).
     * 
     * @return an 8-digit verification code
     */
    public String generateEmailCode() {
        return generateVerificationCode(8);
    }

    /**
     * Stores a verification code for SMS delivery channel using JWT.
     * 
     * <p>Creates a JWT token containing the user's email and hashed verification code.
     * The token expires in 10 minutes for security.
     * 
     * @param email the user's email (identifier)
     * @param code the verification code to store
     * @return JWT token for later validation
     */
    public String storeSmsCode(String email, String code) {
        log.debug("Storing SMS verification code for email: {}", email);
        
        // Hash the code before storing in JWT
        String hashedCode = passwordEncoder.encode(code);
        
        // Generate JWT token with hashed code
        String jwtToken = passwordResetJwtService.generateSmsVerificationToken(email, hashedCode);
        
        log.debug("SMS verification code stored in JWT for email: {}", email);
        return jwtToken;
    }

    /**
     * Validates a provided SMS code against stored code in JWT token.
     *
     * <p>Extracts the hashed code from the JWT token and compares it with
     * the provided code using constant-time comparison for security.
     *
     * @param email the user's email (identifier)
     * @param providedCode the code provided by user
     * @param jwtToken the JWT token from storeSmsCode()
     * @return true if code is valid and not expired
     * @throws InvalidPasswordResetTokenException if token is invalid or expired
     */
    public boolean validateSmsCode(String email, String providedCode, String jwtToken) {
        log.debug("Validating SMS code for email: {}", email);

        try {
            // Validate and extract claims from JWT token
            Claims claims = passwordResetJwtService.validateSmsVerificationToken(jwtToken);

            // Delegate to the new method that avoids double JWT parsing
            return validateSmsCodeWithClaims(email, providedCode, claims);

        } catch (InvalidPasswordResetTokenException e) {
            log.debug("SMS code validation failed for email {} - token invalid: {}",
                    email, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during SMS code validation for email {} - error: {}",
                    email, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates a provided SMS code using pre-extracted JWT claims.
     *
     * <p>This method is useful when JWT claims have already been extracted
     * to avoid double JWT token validation. Uses constant-time comparison
     * for security.
     *
     * @param email the user's email (identifier)
     * @param providedCode the code provided by user
     * @param claims pre-extracted JWT claims containing verificationCodeHash
     * @return true if code is valid
     */
    public boolean validateSmsCodeWithClaims(String email, String providedCode, Claims claims) {
        log.debug("Validating SMS code for email: {} with pre-extracted claims", email);

        try {
            // Extract stored data from claims
            String tokenEmail = claims.get("email", String.class);
            String hashedCode = claims.get("verificationCodeHash", String.class);

            // DEBUG LOGS - À SUPPRIMER EN PROD
            log.debug("DEBUG - Token email: {}", tokenEmail);
            log.debug("DEBUG - Provided code: '{}'", providedCode);
            log.debug("DEBUG - Provided code length: {}", providedCode != null ? providedCode.length() : "null");
            log.debug("DEBUG - Hashed code from JWT: {}", hashedCode != null ? hashedCode.substring(0, Math.min(20, hashedCode.length())) + "..." : "null");

            // Verify email matches (security check)
            if (!email.equals(tokenEmail)) {
                log.warn("Email mismatch in password reset SMS validation: expected {}, got {}",
                        email, tokenEmail);
                return false;
            }

            // Test de validation AVANT le match pour voir si le passwordEncoder fonctionne
            log.debug("DEBUG - About to call passwordEncoder.matches()");

            // Constant-time comparison to prevent timing attacks
            boolean isValid = passwordEncoder.matches(providedCode, hashedCode);

            log.debug("DEBUG - passwordEncoder.matches() returned: {}", isValid);
            log.debug("SMS code validation result for email {}: {}", email, isValid);
            return isValid;

        } catch (Exception e) {
            log.error("Unexpected error during SMS code validation for email {} - error: {}",
                    email, e.getMessage(), e);
            return false;
        }
    }
}
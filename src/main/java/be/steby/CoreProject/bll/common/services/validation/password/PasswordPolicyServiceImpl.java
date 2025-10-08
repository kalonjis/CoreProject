package be.steby.CoreProject.bll.common.services.validation.password;

import be.steby.CoreProject.bll.domains.password.models.PasswordValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Implementation of password policy validation and secure password generation.
 *
 * Configuration is loaded from application.properties:
 * - security.password.min-length (default: 8)
 * - security.password.max-length (default: 55)
 * - security.password.require-uppercase (default: true)
 * - security.password.require-lowercase (default: true)
 * - security.password.require-digit (default: true)
 * - security.password.require-special-char (default: true)
 * - security.password.special-chars (configurable set)
 *
 * All password generation uses SecureRandom for cryptographic security.
 * Generated passwords are shuffled using Fisher-Yates algorithm to avoid
 * predictable patterns (e.g., "Abc1234!..." at the start).
 */
@Service
@Slf4j
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

    // ===============================
    // CONFIGURATION PROPERTIES
    // ===============================

    @Value("${security.password.min-length:8}")
    private int minLength;

    @Value("${security.password.max-length:55}")
    private int maxLength;

    @Value("${security.password.require-uppercase:true}")
    private boolean requireUppercase;

    @Value("${security.password.require-lowercase:true}")
    private boolean requireLowercase;

    @Value("${security.password.require-digit:true}")
    private boolean requireDigit;

    @Value("${security.password.require-special-char:true}")
    private boolean requireSpecialChar;

    @Value("${security.password.special-chars:!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?}")
    private String specialChars;

    // ===============================
    // CHARACTER POOLS
    // ===============================

    // Full character pools (for standard passwords)
    private static final String UPPERCASE_FULL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_FULL = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS_FULL = "0123456789";

    // Human-friendly character pools (without ambiguous characters)
    private static final String UPPERCASE_CLEAR = "ACDEFGHJKLMNPQRSTUVWXYZ";  // No I, O, B
    private static final String LOWERCASE_CLEAR = "acdefghjkmnpqrstuvwxyz";   // No i, l, o
    private static final String DIGITS_CLEAR = "23456789";                     // No 0, 1
    private static final String SPECIAL_CLEAR = "!@#$%*+=?";                   // Clear symbols only

    // ===============================
    // VALIDATION PATTERNS
    // ===============================

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");

    // ===============================
    // PASSWORD VALIDATION
    // ===============================

    @Override
    public PasswordValidationResult validatePassword(String password) {
        if (password == null) {
            return new PasswordValidationResult(false, List.of("Password cannot be null"));
        }

        List<String> validationErrors = new ArrayList<>();

        // Check length requirements
        if (password.length() < minLength) {
            validationErrors.add(String.format(
                    "Password must be at least %d characters long", minLength));
        }

        if (password.length() > maxLength) {
            validationErrors.add(String.format(
                    "Password cannot exceed %d characters", maxLength));
        }

        // Check character requirements
        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).matches()) {
            validationErrors.add("Password must contain at least one uppercase letter");
        }

        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).matches()) {
            validationErrors.add("Password must contain at least one lowercase letter");
        }

        if (requireDigit && !DIGIT_PATTERN.matcher(password).matches()) {
            validationErrors.add("Password must contain at least one digit");
        }

        if (requireSpecialChar && !containsSpecialChar(password)) {
            validationErrors.add("Password must contain at least one special character");
        }

        boolean isValid = validationErrors.isEmpty();

        if (isValid) {
            log.debug("Password validation successful");
        } else {
            log.debug("Password validation failed: {}", String.join(", ", validationErrors));
        }

        return new PasswordValidationResult(isValid, validationErrors);
    }

    /**
     * Checks if a password contains at least one special character from the configured set.
     *
     * @param password The password to check
     * @return true if password contains a special character, false otherwise
     */
    private boolean containsSpecialChar(String password) {
        Pattern specialCharPattern = Pattern.compile(".*[" + Pattern.quote(specialChars) + "].*");
        return specialCharPattern.matcher(password).matches();
    }

    // ===============================
    // VALIDATION HELPERS
    // ===============================

    /**
     * Validates that the desired length is within acceptable bounds.
     *
     * @param desiredLength The length to validate
     * @throws IllegalArgumentException if length is invalid
     */
    private void validateDesiredLength(int desiredLength) {
        if (desiredLength < minLength) {
            throw new IllegalArgumentException(
                    String.format("Desired length (%d) cannot be less than minimum password length (%d)",
                            desiredLength, minLength));
        }

        if (desiredLength > maxLength) {
            throw new IllegalArgumentException(
                    String.format("Desired length (%d) cannot exceed maximum password length (%d)",
                            desiredLength, maxLength));
        }
    }
}
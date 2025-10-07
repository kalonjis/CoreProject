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
    // PASSWORD GENERATION - STANDARD
    // ===============================

    @Override
    public String generateSecurePassword() {
        return generateSecurePassword(minLength);
    }

    @Override
    public String generateSecurePassword(int desiredLength) {
        validateDesiredLength(desiredLength);

        log.debug("Generating secure password with length: {}", desiredLength);

        String password = generatePasswordInternal(
                desiredLength,
                UPPERCASE_FULL,
                LOWERCASE_FULL,
                DIGITS_FULL,
                specialChars
        );

        log.debug("Secure password generated successfully (length: {})", password.length());
        return password;
    }

    // ===============================
    // PASSWORD GENERATION - HUMAN-FRIENDLY
    // ===============================

    @Override
    public String generateHumanFriendlyPassword(int desiredLength) {
        validateDesiredLength(desiredLength);

        log.debug("Generating human-friendly password with length: {} (no ambiguous characters)",
                desiredLength);

        String password = generatePasswordInternal(
                desiredLength,
                UPPERCASE_CLEAR,
                LOWERCASE_CLEAR,
                DIGITS_CLEAR,
                SPECIAL_CLEAR
        );

        log.debug("Human-friendly password generated successfully (length: {})", password.length());
        return password;
    }

    // ===============================
    // PASSWORD GENERATION - FORMATTED
    // ===============================

    @Override
    public String generateFormattedTemporaryPassword(int desiredLength) {
        return generateFormattedTemporaryPassword(desiredLength, 4);
    }

    @Override
    public String generateFormattedTemporaryPassword(int desiredLength, int blockSize) {
        validateDesiredLength(desiredLength);

        if (blockSize < 2 || blockSize > 8) {
            throw new IllegalArgumentException(
                    "Block size must be between 2 and 8 (provided: " + blockSize + ")");
        }

        log.debug("Generating formatted temporary password - length: {}, blockSize: {}",
                desiredLength, blockSize);

        // Generate human-friendly password first
        String password = generateHumanFriendlyPassword(desiredLength);

        // Format into blocks with hyphens
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < password.length(); i++) {
            if (i > 0 && i % blockSize == 0) {
                formatted.append("-");
            }
            formatted.append(password.charAt(i));
        }

        String result = formatted.toString();
        log.debug("Formatted password generated successfully - structure: {} blocks",
                (desiredLength + blockSize - 1) / blockSize);

        return result;
    }

    // ===============================
    // INTERNAL PASSWORD GENERATION
    // ===============================

    /**
     * Internal method for password generation with custom character pools.
     *
     * Algorithm:
     * 1. Ensure at least one character from each required category
     * 2. Fill remaining length with random characters from all pools
     * 3. Shuffle using Fisher-Yates algorithm to avoid predictable patterns
     *
     * @param desiredLength Length of the password to generate
     * @param uppercase Uppercase characters to use
     * @param lowercase Lowercase characters to use
     * @param digits Digits to use
     * @param special Special characters to use
     * @return Generated password
     */
    private String generatePasswordInternal(
            int desiredLength,
            String uppercase,
            String lowercase,
            String digits,
            String special) {

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        // Step 1: Ensure at least one character from each required category
        if (requireUppercase) {
            password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        }
        if (requireLowercase) {
            password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        }
        if (requireDigit) {
            password.append(digits.charAt(random.nextInt(digits.length())));
        }
        if (requireSpecialChar) {
            password.append(special.charAt(random.nextInt(special.length())));
        }

        // Step 2: Fill the rest with random characters from all pools
        String allChars = uppercase + lowercase + digits + special;
        int remainingLength = desiredLength - password.length();

        for (int i = 0; i < remainingLength; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Step 3: Shuffle to avoid predictable patterns (e.g., "Abc1..." at start)
        return shuffleString(password.toString(), random);
    }

    /**
     * Shuffles the characters in a string randomly using Fisher-Yates algorithm.
     * This prevents predictable patterns like having all uppercase letters at the start.
     *
     * Fisher-Yates algorithm:
     * - Start from the end
     * - For each position i, swap with random position j (0 <= j <= i)
     * - Continue until reaching the start
     *
     * Time complexity: O(n)
     *
     * @param input The string to shuffle
     * @param random The SecureRandom instance to use
     * @return The shuffled string
     */
    private String shuffleString(String input, SecureRandom random) {
        char[] characters = input.toCharArray();

        // Fisher-Yates shuffle
        for (int i = characters.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);

            // Swap characters[i] and characters[j]
            char temp = characters[i];
            characters[i] = characters[j];
            characters[j] = temp;
        }

        return new String(characters);
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
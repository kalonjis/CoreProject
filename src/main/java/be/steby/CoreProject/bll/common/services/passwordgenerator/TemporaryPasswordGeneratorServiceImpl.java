package be.steby.CoreProject.bll.common.services.passwordgenerator;

import be.steby.CoreProject.bll.common.services.validation.password.PasswordPolicyService;
import be.steby.CoreProject.bll.domains.password.models.PasswordValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of TemporaryPasswordGeneratorService.
 *
 * Generates cryptographically secure temporary passwords using SecureRandom.
 * All generated passwords are validated against PasswordPolicyService rules.
 *
 * Character sets:
 * - FULL: Complete character sets (maximum security)
 * - CLEAR: No ambiguous characters (human-friendly)
 *
 * @author Steby Core Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemporaryPasswordGeneratorServiceImpl implements TemporaryPasswordGeneratorService {

    private final PasswordPolicyService passwordPolicyService;
    private final SecureRandom secureRandom = new SecureRandom();

    // Configuration from application.properties
    @Value("${security.password.min-length}")
    private int minLength;

    @Value("${security.password.max-length}")
    private int maxLength;

    @Value("${security.password.special-chars}")
    private String specialChars;

    // ===============================
    // CHARACTER SETS - FULL (MAX SECURITY)
    // ===============================
    private static final String UPPERCASE_FULL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_FULL = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS_FULL = "0123456789";

    // ===============================
    // CHARACTER SETS - CLEAR (HUMAN-FRIENDLY)
    // ===============================
    // Removed ambiguous characters: O/0, I/1/l, S/5, Z/2, B/8
    private static final String UPPERCASE_CLEAR = "ACDEFGHJKMNPQRTUVWXY";
    private static final String LOWERCASE_CLEAR = "acdefghjkmnpqrtuvwxy";
    private static final String DIGITS_CLEAR = "34679";
    private static final String SPECIAL_CLEAR = "!@#$%^&*-_=+";

    // ===============================
    // STANDARD GENERATION
    // ===============================

    @Override
    public String generateStandard() {
        return generateStandard(minLength);
    }

    @Override
    public String generateStandard(int length) {
        validateLength(length);

        log.debug("Generating standard secure password (length: {})", length);

        String password = generatePasswordInternal(
                length,
                UPPERCASE_FULL,
                LOWERCASE_FULL,
                DIGITS_FULL,
                specialChars
        );

        validateGeneratedPassword(password);

        log.debug("Standard password generated successfully");
        return password;
    }

    // ===============================
    // HUMAN-FRIENDLY GENERATION
    // ===============================

    @Override
    public String generateHumanFriendly(int length) {
        validateLength(length);

        log.debug("Generating human-friendly password (length: {}, no ambiguous chars)", length);

        String password = generatePasswordInternal(
                length,
                UPPERCASE_CLEAR,
                LOWERCASE_CLEAR,
                DIGITS_CLEAR,
                SPECIAL_CLEAR
        );

        validateGeneratedPassword(password);

        log.debug("Human-friendly password generated successfully");
        return password;
    }

    // ===============================
    // FORMATTED GENERATION
    // ===============================

    @Override
    public String generateFormatted(int length) {
        return generateFormatted(length, 4);
    }

    @Override
    public String generateFormatted(int length, int blockSize) {
        validateLength(length);

        if (blockSize < 2 || blockSize > 8) {
            throw new IllegalArgumentException(
                    "Block size must be between 2 and 8, got: " + blockSize);
        }

        log.debug("Generating formatted password (length: {}, blockSize: {})", length, blockSize);

        // Generate base password
        String basePassword = generatePasswordInternal(
                length,
                UPPERCASE_FULL,
                LOWERCASE_FULL,
                DIGITS_FULL,
                specialChars
        );

        // Format in blocks
        String formattedPassword = formatIntoBlocks(basePassword, blockSize);

        validateGeneratedPassword(basePassword); // Validate without separators

        log.debug("Formatted password generated successfully: {} blocks",
                (length + blockSize - 1) / blockSize);

        return formattedPassword;
    }

    // ===============================
    // SMART GENERATION (DELIVERY METHOD)
    // ===============================

    @Override
    public String generateForDeliveryMethod(String deliveryMethod) {
        return generateForDeliveryMethod(deliveryMethod, minLength);
    }

    @Override
    public String generateForDeliveryMethod(String deliveryMethod, int length) {
        if (deliveryMethod == null) {
            log.debug("No delivery method specified, using standard generation");
            return generateStandard(length);
        }

        log.debug("Generating password for delivery method: {}", deliveryMethod);

        return switch (deliveryMethod.toUpperCase()) {
            case "SMS", "PHONE" -> {
                log.debug("Using human-friendly generation (SMS/PHONE)");
                yield generateHumanFriendly(length);
            }
            case "EMAIL" -> {
                log.debug("Using formatted generation (EMAIL)");
                yield generateFormatted(length);
            }
            default -> {
                log.debug("Unknown delivery method, using standard generation");
                yield generateStandard(length);
            }
        };
    }

    // ===============================
    // INTERNAL GENERATION LOGIC
    // ===============================

    /**
     * Core password generation algorithm.
     *
     * Strategy:
     * 1. Ensure at least ONE character from each required type
     * 2. Fill remaining positions with random characters from all sets
     * 3. Shuffle to avoid predictable patterns
     *
     * @return A cryptographically secure password
     */
    private String generatePasswordInternal(
            int length,
            String uppercase,
            String lowercase,
            String digits,
            String special) {

        StringBuilder password = new StringBuilder(length);
        List<Character> passwordChars = new ArrayList<>(length);

        // 1. Ensure at least one character from each required type
        passwordChars.add(uppercase.charAt(secureRandom.nextInt(uppercase.length())));
        passwordChars.add(lowercase.charAt(secureRandom.nextInt(lowercase.length())));
        passwordChars.add(digits.charAt(secureRandom.nextInt(digits.length())));
        passwordChars.add(special.charAt(secureRandom.nextInt(special.length())));

        // 2. Fill remaining positions with random characters
        String allChars = uppercase + lowercase + digits + special;
        for (int i = 4; i < length; i++) {
            passwordChars.add(allChars.charAt(secureRandom.nextInt(allChars.length())));
        }

        // 3. Shuffle to avoid predictable patterns (type1-type2-type3-random-random...)
        Collections.shuffle(passwordChars, secureRandom);

        // 4. Build final password
        passwordChars.forEach(password::append);

        return password.toString();
    }

    /**
     * Formats a password into blocks separated by dashes.
     * Example: "Xy7$aB9!Qw3#" -> "Xy7$-aB9!-Qw3#"
     */
    private String formatIntoBlocks(String password, int blockSize) {
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < password.length(); i++) {
            if (i > 0 && i % blockSize == 0) {
                formatted.append('-');
            }
            formatted.append(password.charAt(i));
        }

        return formatted.toString();
    }

    // ===============================
    // VALIDATION
    // ===============================

    /**
     * Validates that the desired length is within configured bounds.
     */
    private void validateLength(int length) {
        if (length < minLength || length > maxLength) {
            throw new IllegalArgumentException(String.format(
                    "Password length must be between %d and %d characters, got: %d",
                    minLength, maxLength, length));
        }
    }

    /**
     * Validates that the generated password meets policy requirements.
     * This ensures consistency between generation and validation rules.
     */
    private void validateGeneratedPassword(String password) {
        PasswordValidationResult result = passwordPolicyService.validatePassword(password);

        if (!result.isValid()) {
            // This should NEVER happen if generation logic is correct
            log.error("CRITICAL: Generated password failed validation! Errors: {}",
                    result.errors());
            throw new IllegalStateException(
                    "Generated password doesn't meet policy requirements: "
                            + String.join(", ", result.errors()));
        }
    }
}
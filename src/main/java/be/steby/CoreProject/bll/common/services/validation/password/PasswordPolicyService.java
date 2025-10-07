package be.steby.CoreProject.bll.common.services.validation.password;

import be.steby.CoreProject.bll.domains.password.models.PasswordValidationResult;

/**
 * Service for password policy validation and secure password generation.
 *
 * Provides multiple password generation strategies:
 * - Standard: Respects all policy requirements
 * - Custom length: For temporary passwords needing extra security
 * - Human-friendly: Excludes ambiguous characters (ideal for SMS/phone)
 * - Formatted: Grouped in blocks for easier reading/typing
 *
 * All generated passwords are cryptographically secure using SecureRandom
 * and meet the configured policy requirements from application.properties.
 */
public interface PasswordPolicyService {

    // ===============================
    // PASSWORD VALIDATION
    // ===============================

    /**
     * Validates a password against all configured policy rules.
     *
     * Checks:
     * - Minimum and maximum length
     * - Uppercase letter requirement
     * - Lowercase letter requirement
     * - Digit requirement
     * - Special character requirement
     *
     * @param password The password to validate
     * @return PasswordValidationResult with validation status and error messages
     */
    PasswordValidationResult validatePassword(String password);

    // ===============================
    // PASSWORD GENERATION - STANDARD
    // ===============================

    /**
     * Generates a secure password using the minimum length from configuration.
     * Uses all character types required by the policy.
     *
     * Use case: Standard password generation for user accounts.
     *
     * @return A randomly generated secure password (default: minLength characters)
     */
    String generateSecurePassword();

    /**
     * Generates a secure password with a specific length.
     * Useful for temporary passwords that need extra security.
     *
     * All character types (uppercase, lowercase, digits, special) are included
     * according to the configured policy requirements.
     *
     * Use case: Admin-generated temporary passwords requiring higher entropy.
     *
     * @param desiredLength The desired length (must be >= minLength and <= maxLength)
     * @return A randomly generated secure password of the specified length
     * @throws IllegalArgumentException if desiredLength is outside valid range
     */
    String generateSecurePassword(int desiredLength);

    // ===============================
    // PASSWORD GENERATION - HUMAN-FRIENDLY
    // ===============================

    /**
     * Generates a human-friendly secure password without ambiguous characters.
     * Ideal for temporary passwords sent via SMS, phone, or physical medium.
     *
     * Excludes ambiguous characters to prevent confusion:
     * - 0 (zero) vs O (letter O)
     * - 1 (one) vs l (lowercase L) vs I (uppercase i)
     * - 8 (eight) vs B (letter B)
     *
     * Character pools used:
     * - Uppercase: ACDEFGHJKLMNPQRSTUVWXYZ (no I, O, B)
     * - Lowercase: acdefghjkmnpqrstuvwxyz (no i, l, o)
     * - Digits: 23456789 (no 0, 1)
     * - Special: !@#$%*+=? (clear symbols only)
     *
     * Use case: SMS delivery, phone dictation, printed temporary passwords.
     *
     * @param desiredLength The desired length (must be >= minLength and <= maxLength)
     * @return A randomly generated human-friendly password without ambiguous characters
     * @throws IllegalArgumentException if desiredLength is outside valid range
     */
    String generateHumanFriendlyPassword(int desiredLength);

    // ===============================
    // PASSWORD GENERATION - FORMATTED
    // ===============================

    /**
     * Generates a formatted temporary password in blocks for easier reading.
     * Blocks are separated by hyphens for improved readability.
     * Uses human-friendly characters (no ambiguous ones).
     *
     * Example output with 16 characters and block size 4:
     * "Kj8m-N2pQ-r5Wx-9dFt"
     *
     * Use case: Email delivery, printed passwords, visual display.
     *
     * @param desiredLength The desired length of the password (excluding hyphens)
     * @param blockSize Size of each block (between 2 and 8, default: 4)
     * @return A randomly generated formatted password with hyphens between blocks
     * @throws IllegalArgumentException if desiredLength is outside valid range or blockSize invalid
     */
    String generateFormattedTemporaryPassword(int desiredLength, int blockSize);

    /**
     * Generates a formatted temporary password with default block size of 4.
     * Uses human-friendly characters (no ambiguous ones).
     *
     * Example output with 16 characters:
     * "Kj8m-N2pQ-r5Wx-9dFt"
     *
     * Use case: Email delivery, printed passwords, visual display.
     *
     * @param desiredLength The desired length of the password (excluding hyphens)
     * @return A randomly generated formatted password with hyphens every 4 characters
     * @throws IllegalArgumentException if desiredLength is outside valid range
     */
    String generateFormattedTemporaryPassword(int desiredLength);
}
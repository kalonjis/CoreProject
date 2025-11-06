package be.steby.CoreProject.bll.common.services.passwordgenerator;

import be.steby.CoreProject.bll.common.exceptions.InvalidArgumentException;

/**
 * Service for generating temporary passwords with multiple strategies.
 *
 * This service is responsible ONLY for password generation,
 * while PasswordPolicyService handles validation rules.
 *
 * Provides multiple generation strategies:
 * - Standard: Uses full character set (maximum security)
 * - Human-friendly: Excludes ambiguous characters (ideal for SMS/phone)
 * - Formatted: Grouped in blocks for easier reading/typing
 *
 * All generated passwords are cryptographically secure using SecureRandom
 * and are validated against PasswordPolicyService rules.
 *
 * @author Steby Core Team
 * @version 1.0
 * @since 2025-01
 */
public interface TemporaryPasswordGeneratorService {

    // ===============================
    // STANDARD GENERATION
    // ===============================

    /**
     * Generates a secure password using the minimum length from configuration.
     * Uses all character types (uppercase, lowercase, digits, special).
     *
     * Use case: Standard temporary password generation.
     *
     * @return A randomly generated secure password
     */
    String generateStandard();

    /**
     * Generates a secure password with a specific length.
     * Uses all character types (uppercase, lowercase, digits, special).
     *
     * Use case: Temporary passwords requiring higher entropy.
     *
     * @param length The desired length (must be >= minLength and <= maxLength)
     * @return A randomly generated secure password of the specified length
     * @throws InvalidArgumentException if length is outside valid range
     */
    String generateStandard(int length);

    // ===============================
    // HUMAN-FRIENDLY GENERATION
    // ===============================

    /**
     * Generates a human-friendly secure password without ambiguous characters.
     * Excludes: O/0, I/1/l, S/5, Z/2, B/8, etc.
     *
     * Use case: Temporary passwords sent via SMS, phone, or physical medium
     * where character confusion must be avoided.
     *
     * @param length The desired length
     * @return A human-friendly password without ambiguous characters
     * @throws InvalidArgumentException if length is outside valid range
     */
    String generateHumanFriendly(int length);

    // ===============================
    // FORMATTED GENERATION
    // ===============================

    /**
     * Generates a formatted temporary password grouped in blocks.
     * Example (4-char blocks): "Xy7$-aB9!-Qw3#-Zx8@"
     *
     * Use case: Passwords that users need to type manually,
     * making it easier to read and enter correctly.
     *
     * @param length The desired total length (separator not included)
     * @return A formatted password with dash separators
     * @throws InvalidArgumentException if length is outside valid range
     */
    String generateFormatted(int length);

    /**
     * Generates a formatted temporary password with custom block size.
     * Example (3-char blocks): "Xy7-aB9-Qw3-Zx8"
     *
     * @param length The desired total length (separator not included)
     * @param blockSize Size of each block (must be between 2 and 8)
     * @return A formatted password with custom block size
     * @throws InvalidArgumentException if parameters are invalid
     */
    String generateFormatted(int length, int blockSize);

    // ===============================
    // SMART GENERATION (DELIVERY METHOD)
    // ===============================

    /**
     * Generates a temporary password optimized for the delivery method.
     *
     * Strategy selection:
     * - SMS/PHONE: Human-friendly (no ambiguous characters)
     * - EMAIL: Formatted (easier to copy/paste in blocks)
     * - INTERNAL_DISPLAY: Standard (maximum security)
     *
     * @param deliveryMethod How the password will be delivered
     * @return An appropriately generated password
     */
    String generateForDeliveryMethod(String deliveryMethod);

    /**
     * Generates a temporary password optimized for the delivery method
     * with custom length.
     *
     * @param deliveryMethod How the password will be delivered
     * @param length The desired length
     * @return An appropriately generated password
     */
    String generateForDeliveryMethod(String deliveryMethod, int length);


    /**
     * Generates a secure numeric verification code.
     *
     * <p>Creates a numeric-only code of specified length using SecureRandom.
     * Ideal for SMS verification codes, email verification codes, or any
     * scenario requiring purely numeric codes for easy input.
     *
     * <p>Examples:
     * <ul>
     *   <li>generateNumericCode(6) → "123456" (SMS codes)</li>
     *   <li>generateNumericCode(8) → "12345678" (email codes)</li>
     *   <li>generateNumericCode(4) → "1234" (PIN codes)</li>
     * </ul>
     *
     * @param length the desired length of the numeric code (minimum 4, maximum 12)
     * @return a secure numeric code of the specified length
     * @throws InvalidArgumentException if length is outside valid range
     */
    String generateNumericCode(int length);
}
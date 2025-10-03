package be.steby.CoreProject.bll.domains.user.services;

import be.steby.CoreProject.dal.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.UUID;

/**
 * Service responsible for generating unique usernames.
 * Provides two generation strategies:
 * 1. From email (for self-signup) - Discord-style with random suffix
 * 2. From full name (for admin creation) - Professional lastname+firstname format
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UsernameGeneratorServiceImpl implements UsernameGeneratorService  {

    private final UserRepository userRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    // Character set for random suffix (no ambiguous chars like 0/O, 1/I/l)
    private static final String CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int SUFFIX_LENGTH = 4;
    private static final int MAX_ATTEMPTS = 10;

    // ================== EMAIL-BASED GENERATION (SELF-SIGNUP) ==================

    /**
     * Generates a unique username from email address.
     * Format: emailPrefix_XXXX (Discord-style)
     * Example: john.doe@example.com → johndoe_A3K9
     *
     * @param email User's email address
     * @return Unique username
     */
    @Override
    public String generateFromEmail(String email) {
        if (email == null || email.isBlank()) {
            return generateRandom();
        }

        // Extract email prefix (before @)
        String emailPrefix = email.substring(0, email.indexOf('@'))
                .replaceAll("[^a-zA-Z0-9]", "") // Remove special chars
                .toLowerCase();

        // Ensure minimum length
        if (emailPrefix.length() < 3) {
            emailPrefix = "user" + emailPrefix;
        }

        // Truncate if too long (leave room for separator and suffix)
        int maxPrefixLength = 50 - 1 - SUFFIX_LENGTH; // username max = 50
        if (emailPrefix.length() > maxPrefixLength) {
            emailPrefix = emailPrefix.substring(0, maxPrefixLength);
        }

        return generateUniqueUsernameWithSuffix(emailPrefix);
    }

    // ================== NAME-BASED GENERATION (ADMIN CREATE) ==================

    /**
     * Generates username from full name in professional format.
     * Strategy: lastname + firstname initials, incrementally add more letters if taken.
     *
     * Examples:
     * - Dupont, Jean → dupontj
     * - If taken → dupontje
     * - If taken → dupontjea
     * - If taken → dupontjean
     * - If all taken → dupontjean2
     *
     * @param firstname User's first name
     * @param lastname User's last name
     * @return Unique professional username
     */
    @Override
    public String generateFromFullName(String firstname, String lastname) {
        if (lastname == null || lastname.isBlank() || firstname == null || firstname.isBlank()) {
            log.warn("Invalid name provided for username generation, using random");
            return generateRandom();
        }

        // Normalize and sanitize names (remove accents, special chars)
        String cleanLastname = sanitizeName(lastname);
        String cleanFirstname = sanitizeName(firstname);

        if (cleanLastname.isEmpty() || cleanFirstname.isEmpty()) {
            log.warn("Names became empty after sanitization, using random");
            return generateRandom();
        }

        // Try with increasing firstname length
        for (int i = 1; i <= cleanFirstname.length(); i++) {
            String firstnamePart = cleanFirstname.substring(0, i);
            String candidate = cleanLastname + firstnamePart;

            // Check if available
            if (!userRepository.existsByUsernameIgnoreCase(candidate)) {
                log.debug("Generated username from name: {} (attempt {})", candidate, i);
                return candidate;
            }
        }

        // All firstname letters used, add numeric suffix
        String baseUsername = cleanLastname + cleanFirstname;
        return generateUniqueUsernameWithNumber(baseUsername);
    }

    // ================== HELPER METHODS ==================

    /**
     * Sanitizes a name for username generation.
     * - Removes accents (é → e, ñ → n)
     * - Keeps only letters and numbers
     * - Converts to lowercase
     */
    private String sanitizeName(String name) {
        // Remove accents
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{M}", "");

        // Keep only alphanumeric, convert to lowercase
        return withoutAccents.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    /**
     * Generates unique username by appending numeric suffix.
     * Format: basename2, basename3, etc.
     */
    private String generateUniqueUsernameWithNumber(String baseUsername) {
        // Truncate if too long (leave room for potential large numbers)
        int maxLength = 50 - 3; // Reserve 3 chars for numbers up to 999
        if (baseUsername.length() > maxLength) {
            baseUsername = baseUsername.substring(0, maxLength);
        }

        for (int num = 2; num <= 999; num++) {
            String candidate = baseUsername + num;
            if (!userRepository.existsByUsernameIgnoreCase(candidate)) {
                log.debug("Generated username with number suffix: {}", candidate);
                return candidate;
            }
        }

        // Extreme fallback: UUID
        String fallback = baseUsername + "_" + UUID.randomUUID().toString().substring(0, 6);
        log.warn("Max numeric attempts reached. Using UUID fallback: {}", fallback);
        return fallback;
    }

    /**
     * Generates unique username by appending random suffix (Discord-style).
     * Format: prefix_XXXX
     */
    private String generateUniqueUsernameWithSuffix(String baseUsername) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String suffix = generateRandomSuffix();
            String username = baseUsername + "_" + suffix;

            if (!userRepository.existsByUsernameIgnoreCase(username)) {
                log.debug("Generated unique username with suffix: {} (attempt {})", username, attempt + 1);
                return username;
            }

            log.debug("Username collision: {} - retrying", username);
        }

        // Fallback: use UUID if all attempts failed
        String fallback = baseUsername + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.warn("Max attempts reached for username generation. Using UUID fallback: {}", fallback);
        return fallback;
    }

    /**
     * Generates random alphanumeric suffix.
     * Uses secure random and non-ambiguous characters.
     */
    private String generateRandomSuffix() {
        StringBuilder suffix = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            suffix.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
        }
        return suffix.toString();
    }

    /**
     * Generates a completely random username.
     * Format: user_XXXXXXXX
     * Used as fallback when other methods fail.
     */
    public String generateRandom() {
        String randomId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return generateUniqueUsernameWithSuffix("user_" + randomId);
    }

    /**
     * Validates if a username is auto-generated (contains underscore + suffix pattern).
     * Useful for identifying Discord-style generated usernames.
     */
    public boolean isAutoGenerated(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        int lastUnderscore = username.lastIndexOf('_');
        if (lastUnderscore == -1 || lastUnderscore == username.length() - 1) {
            return false;
        }

        String suffix = username.substring(lastUnderscore + 1);
        return suffix.length() == SUFFIX_LENGTH && suffix.matches("[A-Z0-9]+");
    }
}
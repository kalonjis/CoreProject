package be.steby.CoreProject.bll.domains.user.services;

/**
 * Service for generating usernames based on different contexts.
 * Provides multiple generation strategies depending on the user creation mode.
 */
public interface UsernameGeneratorService {

    /**
     * Generates username from email address.
     * Used for self-signup where only email is provided.
     *
     * Strategy: Extract local part, sanitize, add random suffix if needed.
     * Example: john.doe@example.com → johndoe123
     *
     * @param email The user's email address
     * @return A unique generated username
     */
    String generateFromEmail(String email);

    /**
     * Generates username from full name.
     * Used for admin creation where firstname and lastname are provided.
     *
     * Strategy: lastname + firstname initial, incrementally add more letters if taken.
     * Examples:
     * - Dupont, Jean → dupontj
     * - If taken → dupontje
     * - If taken → dupontjea
     * - etc.
     *
     * @param firstname The user's first name
     * @param lastname The user's last name
     * @return A unique generated username
     */
    String generateFromFullName(String firstname, String lastname);
}
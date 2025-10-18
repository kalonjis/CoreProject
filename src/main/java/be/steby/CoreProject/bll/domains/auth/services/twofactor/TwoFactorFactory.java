package be.steby.CoreProject.bll.domains.auth.services.twofactor;

import be.steby.CoreProject.bll.domains.auth.models.CodeGenerationResult;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;

import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.TwoFactorNotEnabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.TwoFactorServiceNotFoundException;

import java.util.Optional;

/**
 * Factory service interface for managing two-factor authentication operations.
 *
 * This factory serves as the central coordinator for all 2FA operations,
 * abstracting away the complexity of determining which specific 2FA service
 * to use based on the user's primary authentication method.
 *
 * Key responsibilities:
 * - Determine which 2FA method is primary for a given user
 * - Route operations to the appropriate specialized service
 * - Provide a unified interface for code generation and verification
 * - Handle user 2FA status checks
 *
 * Design benefits:
 * - Single entry point for all 2FA operations
 * - Easy to add new 2FA methods without changing calling code
 * - Encapsulates the mapping logic between TwoFactorType and services
 *
 * @author Steby Team
 * @since 2.0.0
 */
public interface TwoFactorFactory {

    /**
     * Generate a verification code using the user's primary 2FA method.
     *
     * This method determines which 2FA service to use based on the user's
     * primary authentication method and delegates code generation to that service.
     * The generated code should be sent to the user via the appropriate channel
     * (email, SMS, etc.) by the calling code.
     *
     * @param user the user for whom to generate a verification code
     * @return a verification code appropriate for the user's primary 2FA method
     * @throws TwoFactorNotEnabledException if user has no enabled 2FA methods
     * @throws TwoFactorServiceNotFoundException if the primary 2FA service is not available
     */
    String generateCode(User user);

    /**
     * Generate a verification code and return both plain text and hashed versions.
     *
     * This method generates a verification code once and returns both the plain text
     * version (for sending to user) and the hashed version (for secure JWT storage).
     * This is more efficient than calling generateCode() and generateAndHashCode() separately.
     *
     * @param user the user for whom to generate a verification code
     * @return CodeGenerationResult containing both plain and hashed versions
     * @throws TwoFactorNotEnabledException if user has no enabled 2FA methods
     * @throws TwoFactorServiceNotFoundException if the primary 2FA service is not available
     */
    CodeGenerationResult generateCodeWithHash(User user);


    /**
     * Verify a provided code against a stored hash using the user's primary 2FA method.
     *
     * Routes the verification to the appropriate 2FA service and performs
     * secure hash comparison using constant-time algorithms to prevent timing attacks.
     *
     * @param user the user attempting verification
     * @param providedCode the code entered by the user
     * @param hashedCode the hashed code retrieved from JWT token
     * @return true if the codes match and verification is successful, false otherwise
     * @throws TwoFactorNotEnabledException if user has no enabled 2FA methods
     * @throws TwoFactorServiceNotFoundException if the primary 2FA service is not available
     */
    boolean verifyCodeAgainstHash(User user, String providedCode, String hashedCode);

    /**
     * Check if the user has any enabled two-factor authentication method.
     *
     * This is a lightweight check that doesn't load full 2FA configurations,
     * useful for quick authentication flow decisions.
     *
     * @param user the user to check
     * @return true if user has at least one enabled 2FA method, false otherwise
     */
    boolean hasTwoFactorEnabled(User user);

    /**
     * Get the user's primary two-factor authentication type.
     *
     * Returns the type of 2FA method that is currently set as primary
     * for this user. Used for UI display and routing decisions.
     *
     * @param user the user whose primary 2FA type to retrieve
     * @return Optional containing the primary TwoFactorType, or empty if no 2FA enabled
     */
    Optional<TwoFactorType> getPrimaryTwoFactorType(User user);
}
package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TwoFactorAuth entity operations.
 * 
 * Provides standard CRUD operations plus domain-specific queries
 * for 2FA management. Follows KISS principle - only essential queries.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@Repository
public interface TwoFactorAuthRepository extends JpaRepository<TwoFactorAuth, Long> {
    
    // ========================================
    // Basic Queries - Find by User
    // ========================================
    
    /**
     * Find all 2FA methods configured for a user.
     * Useful for listing all methods in user settings.
     */
    List<TwoFactorAuth> findAllByUser(User user);

    /**
     * Find all enabled two-factor authentication methods for a user.
     *
     * Returns all 2FA configurations that are currently enabled for the specified user,
     * ordered by primary status (primary first) and then by type for consistent ordering.
     * Used to display available 2FA methods during authentication flow.
     *
     * @param user the user whose enabled 2FA methods to retrieve
     * @return List of enabled TwoFactorAuth configurations, empty if none enabled
     */
    @Query("SELECT tfa FROM TwoFactorAuth tfa WHERE tfa.user = :user AND tfa.enabled = true ORDER BY tfa.isPrimary DESC, tfa.type ASC")
    List<TwoFactorAuth> findByUserAndEnabledTrue(@Param("user") User user);
    
    /**
     * Find a specific 2FA method type for a user.
     * Returns empty if method not configured or disabled.
     */
    Optional<TwoFactorAuth> findByUserAndType(User user, TwoFactorType type);
    
    /**
     * Find an enabled 2FA method of specific type for a user.
     * Most commonly used query during 2FA verification.
     */
    Optional<TwoFactorAuth> findByUserAndTypeAndEnabledTrue(User user, TwoFactorType type);
    
    // ========================================
    // Primary Method Query
    // ========================================
    
    /**
     * Find the primary 2FA method for a user.
     * Only one method can be primary at a time.
     */
    Optional<TwoFactorAuth> findByUserAndIsPrimaryTrue(User user);
    
    // ========================================
    // Existence Checks
    // ========================================
    
    /**
     * Check if user has any enabled 2FA method.
     * Fast boolean check without loading entities.
     */
    boolean existsByUserAndEnabledTrue(User user);

    /**
     * Check if a specific two-factor authentication method is enabled for a user.
     *
     * Efficient boolean check without loading full entity data.
     * Used for quick validation during authentication flow.
     *
     * @param user the user to check
     * @param type the 2FA method type to verify
     * @return true if the specific method is enabled, false otherwise
     */
    boolean existsByUserAndTypeAndEnabledTrue(@Param("user") User user, @Param("type") TwoFactorType type);
    
    // ========================================
    // Bulk Operations
    // ========================================
    
    /**
     * Delete all 2FA methods for a user.
     * Used when user account is deleted or user wants to disable all 2FA.
     */
    void deleteAllByUser(User user);
    
    /**
     * Count enabled 2FA methods for a user.
     * Useful for displaying "X methods enabled" in UI.
     */
    @Query("SELECT COUNT(t) FROM TwoFactorAuth t WHERE t.user = :user AND t.enabled = true")
    long countEnabledMethodsByUser(@Param("user") User user);
}
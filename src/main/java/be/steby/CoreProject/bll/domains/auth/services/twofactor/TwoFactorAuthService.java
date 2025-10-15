package be.steby.CoreProject.bll.domains.auth.services.twofactor;

import be.steby.CoreProject.dl.entities.User;

/**
 * Service interface for Two-Factor Authentication operations.
 * 
 * Handles setup, verification, and management of 2FA methods.
 * Focuses on EMAIL type for now, other types will be added later.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public interface TwoFactorAuthService {
    
    /**
     * Enable EMAIL-based 2FA for a user and set it as primary method.
     * 
     * This is the simplest 2FA method to implement first:
     * - No external dependencies (TOTP libraries, SMS providers)
     * - Uses existing email infrastructure
     * - Good for users without smartphones
     * 
     * Process:
     * 1. Create TwoFactorAuth entity with type=EMAIL
     * 2. Set as enabled and primary
     * 3. Send confirmation email to user
     * 
     * @param user User to enable 2FA for
     * @throws IllegalStateException if EMAIL 2FA already enabled for this user
     */
    void enableEmailTwoFactor();
}

package be.steby.CoreProject.bll.domains.auth.services.twofactor.emailtwofactor;

import be.steby.CoreProject.dl.entities.User;

/**
 * Service interface for EMAIL-based two-factor authentication.
 * Handles all EMAIL 2FA operations including code generation and verification.
 */
public interface EmailTwoFactorService {
    
    /**
     * Enable EMAIL 2FA for user
     */
    void enableEmailTwoFactor(User user);
    
    /**
     * Generate 6-digit email verification code
     */
    String generateVerificationCode();
    
    /**
     * Hash verification code for JWT storage
     */
    String hashVerificationCode(String code);
    
    /**
     * Verify email verification code against hashed version
     */
    boolean verifyCode(String providedCode, String hashedCode);
}



package be.steby.CoreProject.bll.domains.auth.services.twofactor.totptwofactor;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.domains.auth.models.responses.TOTPSetupResponse;

/**
 * Service interface for TOTP-based two-factor authentication.
 * Handles all TOTP 2FA operations including secret generation and verification.
 */
public interface TOTPTwoFactorService {
    
    /**
     * Setup TOTP 2FA for user - generates secret and setup data
     */
    TOTPSetupResponse setupTOTPTwoFactor(User user);
    
    /**
     * Verify TOTP code during setup to enable 2FA
     */
    void verifyTOTPSetup(User user, String code);
    
    /**
     * Verify TOTP code during authentication
     */
    boolean verifyCode(String code, User user);
}


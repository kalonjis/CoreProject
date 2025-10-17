package be.steby.CoreProject.bll.domains.auth.models;

import be.steby.CoreProject.dl.enums.TwoFactorType;

/**
 * Result of login initiation containing either complete login or 2FA requirement
 */
public record LoginInitiationResult(
    boolean requiresTwoFactor,
    TwoFactorType twoFactorType,
    String twoFactorToken,  // JWT token for 2FA session
    LoginTokens loginTokens, // Only present if no 2FA required
    String maskedEmail      // For display purposes
) {
    /**
     * Constructor for successful login without 2FA
     */
    public static LoginInitiationResult completeLogin(LoginTokens tokens) {
        return new LoginInitiationResult(false, null, null, tokens, null);
    }
    
    /**
     * Constructor for login requiring 2FA
     */
    public static LoginInitiationResult requiresTwoFactor(
            TwoFactorType type, 
            String twoFactorToken, 
            String maskedEmail) {
        return new LoginInitiationResult(true, type, twoFactorToken, null, maskedEmail);
    }
}
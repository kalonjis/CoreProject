package be.steby.CoreProject.bll.domains.auth.models;

import be.steby.CoreProject.dl.enums.TwoFactorType;

/**
 * Result of login initiation containing either complete login or 2FA requirement
 */
public record LoginInitiationResult(
        boolean requiresTwoFactor,
        String sessionToken,          // ← Renommé depuis twoFactorToken
        LoginTokens loginTokens
) {

    // Factory method pour 2FA required
    public static LoginInitiationResult requiresTwoFactor(String sessionToken) {
        return new LoginInitiationResult(true, sessionToken, null);
    }

    // Factory method pour login direct (pas de 2FA)
    public static LoginInitiationResult loginComplete(LoginTokens tokens) {
        return new LoginInitiationResult(false, null,  tokens);
    }
}
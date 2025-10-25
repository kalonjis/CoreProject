package be.steby.CoreProject.pl.domains.auth.models.responses;

import be.steby.CoreProject.bll.domains.auth.models.webauthn.WebAuthnCredentialCreationOptions;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import lombok.Builder;

/**
 * Response model for WebAuthn 2FA setup operations.
 * 
 * Contains all information needed for a user to register a WebAuthn credential
 * in their browser using navigator.credentials.create().
 * 
 * Follows the same pattern as TOTPSetupResponse.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@Builder
public record WebAuthnSetupResponse(
    boolean success,
    String message,
    TwoFactorType type,
    String challenge,                           // Base64URL challenge for frontend
    WebAuthnCredentialCreationOptions credentialCreationOptions, // Complete WebAuthn options
    String instructions                         // Setup instructions for user
) {
    
    /**
     * Factory method for successful WebAuthn setup initiation.
     */
    public static WebAuthnSetupResponse success(String challenge, WebAuthnCredentialCreationOptions credentialOptions) {
        return WebAuthnSetupResponse.builder()
            .success(true)
            .message("WebAuthn two-factor authentication setup initiated")
            .type(TwoFactorType.WEBAUTHN)
            .challenge(challenge)
            .credentialCreationOptions(credentialOptions)
            .instructions("Use your browser to create a WebAuthn credential (fingerprint, Face ID, security key, etc.)")
            .build();
    }
}
package be.steby.CoreProject.bll.domains.auth.models.webauthn;

import java.util.Map;

/**
 * Data model for WebAuthn registration start.
 * 
 * Contains all the information needed by the frontend
 * to start the WebAuthn credential registration process.
 * 
 * Follows the same pattern as other auth models.
 */
public record WebAuthnRegistrationStart(
        String registrationToken,    // Temporary JWT token for completion
        String challenge,           // Base64URL encoded challenge
        String userId,              // User identifier for WebAuthn
        Map<String, Object> publicKeyCredentialCreationOptions // Full WebAuthn options
) {}
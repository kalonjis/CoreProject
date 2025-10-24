package be.steby.CoreProject.bll.domains.auth.models.webauthn;

import java.util.List;
import java.util.Map;

/**
 * Data model for WebAuthn authentication start.
 * 
 * Contains challenge and options for WebAuthn authentication.
 * May be used in the future for more detailed authentication flows.
 */
public record WebAuthnAuthenticationStart(
        String authenticationToken, // Temporary JWT token
        String challenge,          // Base64URL encoded challenge
        List<String> allowCredentials, // List of credential IDs
        Map<String, Object> publicKeyCredentialRequestOptions // Full WebAuthn options
) {}
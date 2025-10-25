package be.steby.CoreProject.bll.domains.auth.models.webauthn;


import java.util.List;

/**
 * Typed WebAuthn PublicKeyCredentialCreationOptions.
 * 
 * Replaces the generic Map<String, Object> with strongly typed fields
 * for better type safety and IDE support.
 * 
 * These options are passed directly to navigator.credentials.create()
 * in the browser for WebAuthn credential creation.
 */
public record WebAuthnCredentialCreationOptions(
        String challenge,                           // Base64URL challenge (same as above)
        RelyingParty rp,                           // Relying Party info
        UserInfo user,                             // User info for the credential
        List<PublicKeyCredentialParameters> pubKeyCredParams, // Supported algorithms
        AuthenticatorSelection authenticatorSelection, // Authenticator preferences
        Long timeout,                              // Timeout in milliseconds
        String attestation                         // Attestation preference ("none", "indirect", "direct")
) {}
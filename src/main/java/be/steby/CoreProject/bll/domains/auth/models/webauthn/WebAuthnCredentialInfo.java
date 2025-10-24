package be.steby.CoreProject.bll.domains.auth.models.webauthn;

/**
 * Data model for WebAuthn credential information display.
 * 
 * Used for showing user's registered credentials in the UI
 * for management purposes (view, rename, delete).
 * 
 * Follows the same pattern as other auth models.
 */
public record WebAuthnCredentialInfo(
    String credentialId,        // Credential identifier (truncated for display)
    String name,               // User-assigned name (optional)
    String authenticatorType,  // "PLATFORM" or "CROSS_PLATFORM"
    java.time.LocalDateTime createdAt,     // Registration date
    java.time.LocalDateTime lastUsedAt,    // Last authentication date
    boolean isActive           // Whether credential is active
) {}
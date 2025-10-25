package be.steby.CoreProject.bll.domains.auth.models.webauthn;

/**
 * Supported cryptographic algorithms.
 */
public record PublicKeyCredentialParameters(
    String type,    // Always "public-key"
    Integer alg     // Algorithm identifier (e.g., -7 for ES256, -257 for RS256)
) {}
package be.steby.CoreProject.bll.domains.auth.models.webauthn;

/**
 * Relying Party information.
 */
public record RelyingParty(
    String name,    // Human-readable name (e.g., "Steby CoreProject")
    String id       // Domain (e.g., "localhost", "example.com")
) {}
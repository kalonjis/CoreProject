package be.steby.CoreProject.bll.domains.auth.models.webauthn;

/**
 * User information for credential creation.
 */
public record UserInfo(
    String id,          // User identifier (Base64URL encoded)
    String name,        // User's username or email
    String displayName  // User's display name
) {}
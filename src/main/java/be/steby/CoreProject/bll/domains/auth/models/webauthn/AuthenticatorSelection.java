package be.steby.CoreProject.bll.domains.auth.models.webauthn;

/**
 * Authenticator selection criteria.
 */
public record AuthenticatorSelection(
    String authenticatorAttachment,  // "platform", "cross-platform", or null
    String userVerification,         // "required", "preferred", "discouraged"
    Boolean requireResidentKey       // true/false for resident key requirement
) {}
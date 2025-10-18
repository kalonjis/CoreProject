package be.steby.CoreProject.bll.domains.auth.models;

/**
 * Result object containing both plain text and hashed versions of a generated 2FA code.
 * 
 * This record is used by TwoFactorFactory.generateCodeWithHash() to return both
 * versions of the code efficiently, avoiding the need to generate the code twice.
 * 
 * @param plainCode the plain text verification code to send to the user
 * @param hashedCode the securely hashed version for storage in JWT tokens
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public record CodeGenerationResult(
        String plainCode,
        String hashedCode
) {
}
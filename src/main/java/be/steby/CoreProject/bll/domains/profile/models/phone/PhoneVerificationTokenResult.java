package be.steby.CoreProject.bll.domains.profile.models.phone;

/**
 * Result of phone verification token generation.
 * Contains the JWT token and masked phone number after successful SMS sending.
 * 
 * This record is returned by the BLL service after:
 * 1. Validating the phone number format
 * 2. Generating and sending the verification code via SMS
 * 3. Creating a JWT token with the hashed verification code
 * 
 * The token should be stored in a secure cookie by the presentation layer.
 * The masked phone number is used for user display purposes.
 * 
 * @param token JWT token containing hashed verification code and metadata
 */
public record PhoneVerificationTokenResult(
    String token
) { }
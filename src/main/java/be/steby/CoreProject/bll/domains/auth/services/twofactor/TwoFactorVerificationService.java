package be.steby.CoreProject.bll.domains.auth.services.twofactor;

/**
 * Service for managing two-factor authentication verification codes.
 * Handles code generation, hashing, and verification.
 */
public interface TwoFactorVerificationService {
    
    /**
     * Generate a random 6-digit verification code
     * 
     * @return String containing 6 digits
     */
    String generateVerificationCode();
    
    /**
     * Hash a verification code for secure storage
     * 
     * @param code Plain text verification code
     * @return Hashed code
     */
    String hashVerificationCode(String code);
    
    /**
     * Verify a provided code against a hashed code
     * 
     * @param providedCode Code provided by user
     * @param hashedCode Hashed code from storage/JWT
     * @return true if codes match
     */
    boolean verifyCode(String providedCode, String hashedCode);
}
package be.steby.CoreProject.bll.domains.auth.services.twofactor.smstwofactor;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.SmsTwoFactorAlreadyEnabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.SmsTwoFactorNotEnabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.phone.InvalidPhoneNumberException;
/**
 * Service interface for SMS-based Two-Factor Authentication operations.
 * 
 * Handles the setup, management, and verification of SMS-based 2FA.
 * This method provides secure verification via SMS codes sent to the user's
 * registered phone number.
 * 
 * SMS 2FA flow:
 * 1. User enables SMS 2FA -> configuration saved to database
 * 2. During login -> verification code sent to user's phone number
 * 3. User enters code -> code verified and login completed
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public interface SmsTwoFactorService {
    
    /**
     * Enable SMS-based 2FA for the authenticated user.
     * 
     * Creates a TwoFactorAuth configuration with type SMS and sets it as
     * the primary authentication method for the user. If another 2FA method
     * was previously primary, it will be demoted.
     * 
     * The phone number used for 2FA is the user's registered phone number.
     * 
     * @throws SmsTwoFactorAlreadyEnabledException if SMS 2FA is already enabled for the user
     * @throws InvalidPhoneNumberException if user has no valid phone number configured
     */
    void enable();
    
    /**
     * Disable SMS-based 2FA for the authenticated user.
     * 
     * Removes the SMS 2FA configuration from the database. If this was the
     * primary 2FA method and other methods exist, none will be automatically
     * promoted to primary - the user will need to explicitly set a new primary.
     * 
     * @throws SmsTwoFactorNotEnabledException if SMS 2FA is not currently enabled
     */
    void disable();
    
    /**
     * Generate a verification code for SMS 2FA.
     * 
     * Creates a secure 6-digit numeric code that will be sent to the user's
     * phone number during the 2FA verification process. The code has a limited
     * lifetime and should be used promptly.
     * 
     * @param user the user for whom to generate the code
     * @return a 6-digit verification code as a String
     * @throws SmsTwoFactorNotEnabledException if SMS 2FA is not enabled for the user
     * @throws InvalidPhoneNumberException if user has no valid phone number configured
     */
    String generateCode(User user);
    
    /**
     * Verify a provided code against the expected verification code.
     * 
     * Performs secure comparison of the user-provided code with the expected
     * code (typically generated and sent via SMS). Uses constant-time
     * comparison to prevent timing attacks.
     * 
     * @param user the user attempting verification
     * @param providedCode the code entered by the user
     * @param expectedCode the code that was generated and sent to the user
     * @return true if the codes match, false otherwise
     * @throws SmsTwoFactorNotEnabledException if SMS 2FA is not enabled for the user
     */
    boolean verifyCode(User user, String providedCode, String expectedCode);
}
package be.steby.CoreProject.bll.domains.auth.services.twofactor.emailtwofactor;

import be.steby.CoreProject.bll.domains.auth.models.EmailTwoFactorActivationBllRequest;
import be.steby.CoreProject.bll.domains.auth.models.TwoFactorActivationResult;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.EmailTwoFactorAlreadyEnabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.EmailTwoFactorNotEnabledException;
import be.steby.CoreProject.dl.entities.User;

/**
 * Service interface for Email-based Two-Factor Authentication operations.
 * 
 * Handles the setup, management, and verification of email-based 2FA.
 * This is the simplest 2FA method as it leverages existing email infrastructure
 * without requiring external dependencies or specialized hardware.
 * 
 * Email 2FA flow:
 * 1. User enables email 2FA -> configuration saved to database
 * 2. During login -> verification code sent to user's email
 * 3. User enters code -> code verified and login completed
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public interface EmailTwoFactorService {

    TwoFactorActivationResult initiateActivation();

    void verifyAndActivateEmailTwoFactor(EmailTwoFactorActivationBllRequest request);
    
    /**
     * Enable email-based 2FA for the authenticated user.
     * 
     * Creates a TwoFactorAuth configuration with type EMAIL and sets it as
     * the primary authentication method for the user. If another 2FA method
     * was previously primary, it will be demoted.
     * 
     * The email address used for 2FA is the user's primary email address.
     * 
     * @throws EmailTwoFactorAlreadyEnabledException if email 2FA is already enabled for the user
     */
    void enable();
    
    /**
     * Disable email-based 2FA for the authenticated user.
     * 
     * Removes the email 2FA configuration from the database. If this was the
     * primary 2FA method and other methods exist, none will be automatically
     * promoted to primary - the user will need to explicitly set a new primary.
     * 
     * @throws EmailTwoFactorNotEnabledException if email 2FA is not currently enabled
     */
    void disable();
    
    /**
     * Generate a verification code for email 2FA.
     * 
     * Creates a secure 6-digit numeric code that will be sent to the user's
     * email address during the 2FA verification process. The code has a limited
     * lifetime and should be used promptly.
     * 
     * @param user the user for whom to generate the code
     * @return a 6-digit verification code as a String
     * @throws EmailTwoFactorNotEnabledException if email 2FA is not enabled for the user
     */
    String generateVerificationCode(User user);
    
    /**
     * Verify a provided code against the expected verification code.
     * 
     * Performs secure comparison of the user-provided code with the expected
     * code (typically generated and sent via email). Uses constant-time
     * comparison to prevent timing attacks.
     * 
     * @param user the user attempting verification
     * @param providedCode the code entered by the user
     * @param expectedCode the code that was generated and sent to the user
     * @return true if the codes match, false otherwise
     * @throws EmailTwoFactorNotEnabledException if email 2FA is not enabled for the user
     */
    boolean verifyCode(User user, String providedCode, String expectedCode);
}
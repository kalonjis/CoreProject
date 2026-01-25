package be.steby.CoreProject.bll.domains.auth.services.twofactor.smstwofactor;

import be.steby.CoreProject.bll.domains.auth.models.SmsTwoFactorActivationBllRequest;
import be.steby.CoreProject.bll.domains.auth.models.TwoFactorActivationResult;
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


    // ===========================================================================
    // SETUP FLOW (Two-step activation)
    // ===========================================================================

    /**
     * Initiate SMS 2FA activation process.
     *
     * Step 1 of 2: Generates a verification code and sends it via SMS.
     * The activation token is returned for cookie storage.
     *
     * Process:
     * 1. Validates that SMS 2FA is not already enabled
     * 2. Validates that user has a verified phone number
     * 3. Checks rate limiting for activation attempts
     * 4. Generates a secure 6-digit verification code
     * 5. Creates JWT activation token with hashed code
     * 6. Publishes event to send SMS with plain code
     *
     * @return TwoFactorActivationResult containing the activation token
     * @throws SmsTwoFactorAlreadyEnabledException if SMS 2FA is already enabled
     * @throws InvalidPhoneNumberException if user has no verified phone number
     * @throws MaxAttemptsReachedException if too many activation attempts
     */
    TwoFactorActivationResult initiateActivation();

    /**
     * Verify code and activate SMS 2FA.
     *
     * Step 2 of 2: Validates the provided code against the hashed value
     * in the activation token. On success, SMS 2FA is enabled.
     *
     * Process:
     * 1. Validates the activation token (JWT)
     * 2. Extracts user and hashed code from token
     * 3. Checks rate limiting for verification attempts
     * 4. Verifies the provided code against hashed code
     * 5. Creates and saves SMS 2FA configuration
     * 6. Sets SMS 2FA as primary method
     *
     * @param request the activation request containing code and token
     * @throws InvalidVerificationCodeException if code is invalid
     * @throws MaxAttemptsReachedException if too many verification attempts
     */
    void verifyAndActivateSmsTwoFactor(SmsTwoFactorActivationBllRequest request);
}
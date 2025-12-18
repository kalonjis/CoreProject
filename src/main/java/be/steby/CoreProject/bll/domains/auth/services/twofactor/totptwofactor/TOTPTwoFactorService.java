package be.steby.CoreProject.bll.domains.auth.services.twofactor.totptwofactor;

import be.steby.CoreProject.bll.domains.auth.models.TotpActivationInitiateResult;
import be.steby.CoreProject.bll.domains.auth.models.TotpTwoFactorActivationBllRequest;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.TOTPTwoFactorAlreadyEnabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.TOTPTwoFactorNotEnabledException;

/**
 * Service interface for TOTP-based Two-Factor Authentication operations.
 * 
 * Handles the setup, management, and verification of TOTP (Time-based One-Time Password) 2FA.
 * TOTP is the most secure 2FA method and is compatible with popular authenticator apps
 * like Google Authenticator, Authy, Microsoft Authenticator, etc.
 * 
 * TOTP 2FA flow:
 * 1. User enables TOTP 2FA -> secret key generated and QR code provided
 * 2. User scans QR code with authenticator app 
 * 3. During login -> user enters current TOTP code from app
 * 4. Code verified against RFC 6238 algorithm and login completed
 * 
 * Benefits of TOTP:
 * - Most secure (phishing-resistant)
 * - Works offline (no SMS/email required)
 * - Standardized RFC 6238 implementation
 * - Compatible with all major authenticator apps
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public interface TOTPTwoFactorService {
    
    /**
     * Enable TOTP-based 2FA for the authenticated user.
     * 
     * Creates a TwoFactorAuth configuration with type TOTP and generates a new
     * secret key. Sets it as the primary authentication method for the user. 
     * If another 2FA method was previously primary, it will be demoted.
     * 
     * The secret key is generated using secure random bytes and encoded in Base32
     * format compatible with RFC 6238 specification.
     * 
     * @return SetupInfo containing the secret key and QR code URI for app setup
     * @throws TOTPTwoFactorAlreadyEnabledException if TOTP 2FA is already enabled for the user
     */
    TOTPSetupInfo enable();


    /**
     * Initiate TOTP 2FA activation process (step 1 of 2-step flow).
     *
     * Generates a new secret key and QR code URI for the user to configure
     * their authenticator app. The secret is stored in a JWT activation token
     * (set as HttpOnly cookie by controller) for verification in step 2.
     *
     * Unlike the direct enable() method, this does NOT persist the TOTP config
     * until the user verifies they can generate valid codes in step 2.
     *
     * @return TotpActivationInitiateResult containing QR code, secret, and activation token
     * @throws TOTPTwoFactorAlreadyEnabledException if TOTP 2FA is already enabled
     * @throws MaxAttemptsReachedException if rate limit exceeded
     */
    TotpActivationInitiateResult initiateActivation();

    /**
     * Verify TOTP code and complete activation (step 2 of 2-step flow).
     *
     * Validates the provided TOTP code against the secret stored in the
     * activation token. On success, persists the TOTP configuration and
     * enables TOTP 2FA for the user.
     *
     * @param request contains the 6-digit TOTP code and activation token
     * @throws TOTPTwoFactorAlreadyEnabledException if TOTP 2FA is already enabled
     * @throws InvalidVerificationCodeException if the TOTP code is invalid
     * @throws MaxAttemptsReachedException if rate limit exceeded
     */
    void verifyAndActivateTotpTwoFactor(TotpTwoFactorActivationBllRequest request);

    /**
     * Disable TOTP-based 2FA for the authenticated user.
     * 
     * Removes the TOTP 2FA configuration from the database. If this was the
     * primary 2FA method and other methods exist, none will be automatically
     * promoted to primary - the user will need to explicitly set a new primary.
     * 
     * @throws TOTPTwoFactorNotEnabledException if TOTP 2FA is not currently enabled
     */
    void disable();
    
    /**
     * Generate the current TOTP code for a user (for verification purposes).
     * 
     * This method is typically used internally for code verification.
     * The actual code generation for users is done by their authenticator app.
     * 
     * @param user the user for whom to generate the current TOTP code
     * @return the current 6-digit TOTP code
     * @throws TOTPTwoFactorNotEnabledException if TOTP 2FA is not enabled for the user
     */
    String generateCode(User user);
    
    /**
     * Verify a provided TOTP code against the current valid codes.
     * 
     * Validates the user-provided code against the current time window and
     * adjacent time windows (to account for slight time differences between
     * user device and server). Uses RFC 6238 algorithm with:
     * - 30-second time steps
     * - 6-digit codes
     * - HMAC-SHA1 algorithm
     * - Time window tolerance (±1 period)
     * 
     * @param user the user attempting verification
     * @param providedCode the code entered by the user from their authenticator app
     * @return true if the code is valid for current time window, false otherwise
     * @throws TOTPTwoFactorNotEnabledException if TOTP 2FA is not enabled for the user
     */
    boolean verifyCode(User user, String providedCode);
    
    /**
     * Data class containing TOTP setup information for user onboarding.
     */
    record TOTPSetupInfo(
        String secretKey,      // Base32-encoded secret key
        String qrCodeUri,      // URI for QR code generation
        String manualEntryKey  // Formatted key for manual entry
    ) {}
}
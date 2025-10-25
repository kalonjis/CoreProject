package be.steby.CoreProject.bll.domains.auth.services.twofactor.webauthn;

import be.steby.CoreProject.bll.domains.auth.models.webauthn.WebAuthnSetupInfo;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.WebAuthnNotEnabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.WebAuthnAlreadyEnabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.WebAuthnAuthenticationException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.WebAuthnRegistrationException;
import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidTokenException;

/**
 * WebAuthn Two-Factor Authentication Service Interface
 *
 * Follows the same pattern as EmailTwoFactorService, TOTPTwoFactorService, etc.
 * NO common interface - each 2FA service has its own specific interface.
 *
 * Integration with TwoFactorFactory:
 * - Factory routes WEBAUTHN type to this service
 * - Uses JWT temporary tokens for challenge storage
 * - Auto-links with existing User entities
 * - Manages TwoFactorAuth table entries
 *
 * WebAuthn 2FA Flow:
 * 1. User enables WebAuthn 2FA -> enable() creates TwoFactorAuth + returns setup data
 * 2. User completes credential registration -> completeRegistration() validates credential
 * 3. During login -> generateCode() creates challenge, verifyCode() validates assertion
 */
public interface WebAuthnTwoFactorService {

    /**
     * Enable WebAuthn 2FA for the authenticated user.
     *
     * Creates a TwoFactorAuth configuration with type WEBAUTHN and sets it as
     * the primary authentication method. Similar to TOTPTwoFactorService.enable().
     *
     * Returns setup information needed for credential registration in the browser.
     * The user must then complete registration with completeRegistration().
     *
     * Flow:
     * 1. Check if WebAuthn already enabled
     * 2. Create TwoFactorAuth entry (enabled=true)
     * 3. Generate registration challenge
     * 4. Create temporary JWT with challenge
     * 5. Return setup data for frontend
     *
     * @return WebAuthnSetupInfo containing challenge and registration options
     * @throws WebAuthnAlreadyEnabledException if already enabled
     */
    WebAuthnSetupInfo initiateActivation();

    /**
     * Complete WebAuthn credential registration.
     *
     * This method receives the WebAuthn credential created by the browser
     * and validates it to complete the registration process.
     *
     * Called after enable() when user has created credential in browser.
     *
     * @param registrationToken Temporary JWT token from enable()
     * @param webAuthnCredential The credential returned by navigator.credentials.create()
     * @return true if registration successful
     * @throws InvalidTokenException if registrationToken invalid
     * @throws WebAuthnRegistrationException if credential validation fails
     */
    boolean completeRegistration(String webAuthnCredential);

    /**
     * Disable WebAuthn 2FA for the authenticated user.
     *
     * Removes the WebAuthn 2FA configuration and all associated credentials.
     * Follows same pattern as other 2FA services.
     *
     * @throws WebAuthnNotEnabledException if WebAuthn not enabled
     */
    void disable();

    /**
     * Generate WebAuthn authentication challenge.
     *
     * Called by TwoFactorFactory when user chooses WebAuthn during 2FA verification.
     * Equivalent to generateCode() in other 2FA services.
     *
     * @param user The user attempting authentication
     * @return WebAuthn challenge data (replaces "code" for WebAuthn)
     * @throws WebAuthnNotEnabledException if user has no WebAuthn credentials
     */
    String generateCode(User user);

    /**
     * Verify WebAuthn assertion.
     *
     * Called by TwoFactorFactory.verifyTwoFactorCode() for WebAuthn verification.
     * Equivalent to verifyCode() in other 2FA services.
     *
     * @param user The user being authenticated
     * @param providedAssertion WebAuthn assertion from client (replaces "providedCode")
     * @param expectedChallenge Challenge data from generateCode() (replaces "expectedCode")
     * @return true if authentication successful
     * @throws WebAuthnAuthenticationException if assertion validation fails
     */
    boolean verifyCode(User user, String providedAssertion, String expectedChallenge);
}
package be.steby.CoreProject.bll.domains.auth.services.twofactor.webauthn;

import be.steby.CoreProject.bll.domains.auth.models.webauthn.WebAuthnCredentialInfo;
import be.steby.CoreProject.bll.domains.auth.models.webauthn.WebAuthnRegistrationStart;
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
 * Integration with your TwoFactorFactory:
 * - Factory routes WEBAUTHN type to this service
 * - Uses JWT temporary tokens for challenge storage
 * - Auto-links with existing User entities
 * - Manages TwoFactorAuth table entries
 */
public interface WebAuthnTwoFactorService {

    /**
     * Enable WebAuthn 2FA for the authenticated user.
     *
     * Unlike other 2FA methods, this returns registration challenge data
     * instead of immediately enabling. Registration is completed in two phases.
     *
     * Flow:
     * 1. Check if WebAuthn already enabled
     * 2. Generate registration challenge
     * 3. Create temporary JWT with challenge
     * 4. Return registration options for frontend
     *
     * @return WebAuthnRegistrationStart containing challenge and options
     * @throws WebAuthnAlreadyEnabledException if already enabled
     */
    WebAuthnRegistrationStart enable();

    /**
     * Complete WebAuthn registration with the credential from frontend.
     *
     * This method receives the WebAuthn credential created by the browser
     * and completes the registration process.
     *
     * @param registrationToken Temporary JWT token from enable()
     * @param webAuthnCredential The credential returned by navigator.credentials.create()
     * @return true if registration successful
     * @throws InvalidTokenException if registrationToken invalid
     * @throws WebAuthnRegistrationException if credential validation fails
     */
    boolean completeRegistration(String registrationToken, String webAuthnCredential);

    /**
     * Disable WebAuthn 2FA for the authenticated user.
     *
     * Removes all WebAuthn credentials and disables WebAuthn 2FA.
     * Follows same pattern as other 2FA services.
     *
     * @throws WebAuthnNotEnabledException if WebAuthn not enabled
     */
    void disable();

    /**
     * Generate WebAuthn authentication challenge.
     *
     * Called by TwoFactorFactory when user chooses WebAuthn during 2FA verification.
     * Similar to how other services generate codes.
     *
     * @param user The user attempting authentication
     * @return WebAuthn challenge data (replaces "code" for WebAuthn)
     * @throws WebAuthnNotEnabledException if user has no WebAuthn credentials
     */
    String generateChallenge(User user);

    /**
     * Verify WebAuthn assertion.
     *
     * Called by TwoFactorFactory.verifyTwoFactorCode() for WebAuthn verification.
     *
     * @param user The user being authenticated
     * @param providedAssertion WebAuthn assertion from client (replaces "code")
     * @param challengeToken JWT token containing the challenge (replaces "hashedCode")
     * @return true if authentication successful
     * @throws InvalidTokenException if challengeToken invalid
     * @throws WebAuthnAuthenticationException if assertion validation fails
     */
    boolean verifyAssertion(User user, String providedAssertion, String challengeToken);



    /**
     * Get user's WebAuthn credentials for management.
     *
     * Returns list of registered credentials with metadata
     * for display in user settings.
     *
     * @return List of WebAuthnCredentialInfo for UI display
     */
    java.util.List<WebAuthnCredentialInfo> getUserCredentials();


}
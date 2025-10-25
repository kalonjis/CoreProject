package be.steby.CoreProject.bll.domains.auth.services.twofactor.webauthn;

import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.WebAuthnAlreadyEnabledException;
import be.steby.CoreProject.bll.domains.auth.models.webauthn.*;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dal.repositories.TwoFactorAuthRepository;
import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

/**
 * Implementation of WebAuthnTwoFactorService.
 * 
 * Handles WebAuthn-based 2FA using browser credentials and biometric authentication.
 * Follows the same pattern as TOTPTwoFactorServiceImpl and SmsTwoFactorServiceImpl.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebAuthnTwoFactorServiceImpl implements WebAuthnTwoFactorService {

    private final TwoFactorAuthRepository twoFactorAuthRepository;
    private final UserService userService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public WebAuthnSetupInfo initiateActivation() {
        User user = userService.getAuthenticatedUser();
        log.info("Initiating WebAuthn 2FA activation for user: {}", user.getUsername());

        // 1. Check if WebAuthn 2FA is already enabled
        if (twoFactorAuthRepository.existsByUserAndTypeAndEnabledTrue(user, TwoFactorType.WEBAUTHN)) {
            log.warn("WebAuthn 2FA already enabled for user: {}", user.getUsername());
            throw new WebAuthnAlreadyEnabledException("WebAuthn two-factor authentication is already enabled");
        }

        // 2. Check if a disabled WebAuthn 2FA configuration already exists
        var existingWebAuthnTwoFactor = twoFactorAuthRepository
                .findByUserAndType(user, TwoFactorType.WEBAUTHN);

        TwoFactorAuth webAuthnAuth;
        if (existingWebAuthnTwoFactor.isPresent()) {
            // Reactivate existing configuration
            webAuthnAuth = existingWebAuthnTwoFactor.get();
            webAuthnAuth.setEnabled(true);
            webAuthnAuth.setDisabledAt(null);
            log.debug("Reactivated existing WebAuthn 2FA configuration for user: {}", user.getUsername());
        } else {
            // Create new WebAuthn 2FA configuration
            webAuthnAuth = TwoFactorAuth.builder()
                    .user(user)
                    .type(TwoFactorType.WEBAUTHN)
                    .enabled(true)
                    .isPrimary(true) // Set as primary by default
                    .enabledAt(Instant.now())
                    .build();
            log.debug("Created new WebAuthn 2FA configuration for user: {}", user.getUsername());
        }

        // 3. Demote any existing primary 2FA method
        demoteExistingPrimaryTwoFactor(user);

        // 4. Set this as primary
        webAuthnAuth.setIsPrimary(true);
        twoFactorAuthRepository.save(webAuthnAuth);

        // 5. Generate WebAuthn challenge and credential creation options
        String challenge = generateChallenge();
        WebAuthnCredentialCreationOptions credentialOptions = buildCredentialCreationOptions(user, challenge);

        log.info("WebAuthn 2FA activation initiated successfully for user: {}", user.getUsername());

        return new WebAuthnSetupInfo(challenge, credentialOptions);
    }

    @Override
    public boolean completeRegistration(String webAuthnCredential) {
        // TODO: Implement in next step
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void disable() {
        // TODO: Implement in next step
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String generateCode(User user) {
        // TODO: Implement in next step
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean verifyCode(User user, String providedAssertion, String expectedChallenge) {
        // TODO: Implement in next step
        throw new UnsupportedOperationException("Not implemented yet");
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * Generate a cryptographically secure challenge for WebAuthn.
     * 
     * @return Base64URL encoded challenge
     */
    private String generateChallenge() {
        byte[] challengeBytes = new byte[32]; // 32 bytes = 256 bits
        secureRandom.nextBytes(challengeBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);
    }

    /**
     * Build WebAuthn credential creation options for browser.
     * 
     * @param user The user for whom to create credentials
     * @param challenge The challenge to include
     * @return Complete credential creation options
     */
    private WebAuthnCredentialCreationOptions buildCredentialCreationOptions(User user, String challenge) {
        // Relying Party info
        RelyingParty rp = new RelyingParty(
            "Steby CoreProject",  // Human-readable name
            "localhost"           // TODO: Make this configurable
        );

        // User info for the credential
        String userIdBytes = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(user.getId().toString().getBytes());
        UserInfo userInfo = new UserInfo(
            userIdBytes,
            user.getEmail(),
            user.getFirstname() + " " + user.getLastname()
        );

        // Supported cryptographic algorithms
        List<PublicKeyCredentialParameters> pubKeyCredParams = List.of(
                new PublicKeyCredentialParameters("public-key", -7),   // ES256
                new PublicKeyCredentialParameters("public-key", -257)  // RS256
        );

        // Authenticator selection preferences
        AuthenticatorSelection authenticatorSelection = new AuthenticatorSelection(
            "platform",    // Prefer platform authenticators (built-in)
            "required",    // Require user verification
            false          // Don't require resident key
        );

        return new WebAuthnCredentialCreationOptions(
            challenge,
            rp,
            userInfo,
            pubKeyCredParams,
            authenticatorSelection,
            60000L,        // 60 seconds timeout
            "none"         // No attestation required
        );
    }

    /**
     * Demote existing primary 2FA method to secondary.
     * Follows same pattern as TOTPTwoFactorServiceImpl.
     */
    private void demoteExistingPrimaryTwoFactor(User user) {
        twoFactorAuthRepository.findByUserAndIsPrimaryTrue(user)
                .ifPresent(existingPrimary -> {
                    log.debug("Demoting existing primary 2FA method: {} for user: {}", 
                             existingPrimary.getType(), user.getUsername());
                    existingPrimary.setIsPrimary(false);
                    twoFactorAuthRepository.save(existingPrimary);
                });
    }
}
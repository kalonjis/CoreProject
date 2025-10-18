package be.steby.CoreProject.bll.domains.auth.services.twofactor.emailtwofactor;


import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.EmailTwoFactorAlreadyEnabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.EmailTwoFactorNotEnabledException;
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
import java.util.Optional;

/**
 * Implementation of EmailTwoFactorService for managing email-based 2FA.
 *
 * This service handles the complete lifecycle of email-based two-factor authentication:
 * - Enabling and disabling email 2FA for users
 * - Generating secure verification codes
 * - Verifying user-provided codes
 *
 * Email 2FA is the simplest 2FA method to implement as it:
 * - Uses existing email infrastructure
 * - Requires no external dependencies
 * - Works on any device with email access
 * - Is familiar to users
 *
 * Security considerations:
 * - Verification codes are 6 digits for good security/usability balance
 * - Codes should have short expiration times (handled by calling code)
 * - Failed attempts are tracked to prevent brute force attacks
 * - Constant-time comparison prevents timing attacks
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTwoFactorServiceImpl implements EmailTwoFactorService {

    private final TwoFactorAuthRepository twoFactorAuthRepository;
    private final UserService userService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void enable() {
        User user = userService.getAuthenticatedUser();
        log.info("Enabling email 2FA for user: {}", user.getUsername());

        // Check if email 2FA is already enabled
        if (twoFactorAuthRepository.existsByUserAndTypeAndEnabledTrue(user, TwoFactorType.EMAIL)) {
            log.warn("Email 2FA already enabled for user: {}", user.getUsername());
            throw new EmailTwoFactorAlreadyEnabledException("Email two-factor authentication is already enabled");
        }

        // Check if a disabled email 2FA configuration already exists
        Optional<TwoFactorAuth> existingEmailTwoFactor = twoFactorAuthRepository
                .findByUserAndType(user, TwoFactorType.EMAIL);

        if (existingEmailTwoFactor.isPresent()) {
            // Reactivate existing configuration
            TwoFactorAuth emailTwoFactor = existingEmailTwoFactor.get();
            log.debug("Reactivating existing email 2FA configuration (ID: {}) for user: {}",
                    emailTwoFactor.getId(), user.getUsername());

            // Disable any other primary method first
            Optional<TwoFactorAuth> existingPrimary = twoFactorAuthRepository.findByUserAndIsPrimaryTrue(user);
            if (existingPrimary.isPresent()) {
                log.debug("Demoting existing primary 2FA method: {} for user: {}",
                        existingPrimary.get().getType(), user.getUsername());
                existingPrimary.get().setIsPrimary(false);
                twoFactorAuthRepository.save(existingPrimary.get());
            }

            // Reactivate email 2FA
            emailTwoFactor.setEnabled(true);
            emailTwoFactor.setIsPrimary(true);
            emailTwoFactor.setEnabledAt(Instant.now());
            emailTwoFactor.setDisabledAt(null);
            emailTwoFactor.setFailedAttempts(0);

            TwoFactorAuth saved = twoFactorAuthRepository.save(emailTwoFactor);
            log.info("Email 2FA reactivated successfully for user: {} (2FA ID: {})",
                    user.getUsername(), saved.getId());
        } else {
            // Create new email 2FA configuration
            log.debug("Creating new email 2FA configuration for user: {}", user.getUsername());

            // Disable any existing primary method (only one can be primary at a time)
            Optional<TwoFactorAuth> existingPrimary = twoFactorAuthRepository.findByUserAndIsPrimaryTrue(user);
            if (existingPrimary.isPresent()) {
                log.debug("Demoting existing primary 2FA method: {} for user: {}",
                        existingPrimary.get().getType(), user.getUsername());
                existingPrimary.get().setIsPrimary(false);
                twoFactorAuthRepository.save(existingPrimary.get());
            }

            TwoFactorAuth emailTwoFactor = TwoFactorAuth.builder()
                    .user(user)
                    .type(TwoFactorType.EMAIL)
                    .enabled(true)
                    .isPrimary(true)
                    .enabledAt(Instant.now())
                    .failedAttempts(0)
                    .label("Email verification")
                    .build();

            // Save to database
            TwoFactorAuth saved = twoFactorAuthRepository.save(emailTwoFactor);
            log.info("Email 2FA enabled successfully for user: {} (2FA ID: {})",
                    user.getUsername(), saved.getId());
        }
    }

    @Override
    @Transactional
    public void disable() {
        User user = userService.getAuthenticatedUser();
        log.info("Disabling email 2FA for user: {}", user.getUsername());

        // Find the user's email 2FA configuration
        Optional<TwoFactorAuth> emailTwoFactor = twoFactorAuthRepository
                .findByUserAndTypeAndEnabledTrue(user, TwoFactorType.EMAIL);

        if (emailTwoFactor.isEmpty()) {
            log.warn("Attempted to disable email 2FA for user: {} but it's not enabled", user.getUsername());
            throw new EmailTwoFactorNotEnabledException("Email two-factor authentication is not enabled");
        }

        // Disable the email 2FA
        TwoFactorAuth twoFactorAuth = emailTwoFactor.get();
        twoFactorAuth.setEnabled(false);
        twoFactorAuth.setIsPrimary(false);
        twoFactorAuth.setDisabledAt(Instant.now());

        twoFactorAuthRepository.save(twoFactorAuth);
        log.info("Email 2FA disabled successfully for user: {} (2FA ID: {})",
                user.getUsername(), twoFactorAuth.getId());

        // Note: We don't automatically promote another method to primary.
        // The user will need to explicitly set a new primary method if desired.
    }

    @Override
    public String generateCode(User user) {
        log.debug("Generating verification code for user: {}", user.getUsername());

        // Verify that email 2FA is enabled for this user
        if (!twoFactorAuthRepository.existsByUserAndTypeAndEnabledTrue(user, TwoFactorType.EMAIL)) {
            log.warn("Attempted to generate code for user: {} but email 2FA is not enabled", user.getUsername());
            throw new EmailTwoFactorNotEnabledException("Email two-factor authentication is not enabled for this user");
        }

        // Generate a secure 6-digit code
        // Range: 100000 to 999999 (inclusive)
        int code = 100000 + secureRandom.nextInt(900000);
        String verificationCode = String.valueOf(code);

        log.debug("Verification code generated for user: {}", user.getUsername());
        return verificationCode;
    }

    @Override
    public boolean verifyCode(User user, String providedCode, String expectedCode) {
        log.debug("Verifying 2FA code for user: {}", user.getUsername());

        // Verify that email 2FA is enabled for this user
        Optional<TwoFactorAuth> emailTwoFactor = twoFactorAuthRepository
                .findByUserAndTypeAndEnabledTrue(user, TwoFactorType.EMAIL);

        if (emailTwoFactor.isEmpty()) {
            log.warn("Attempted to verify code for user: {} but email 2FA is not enabled", user.getUsername());
            throw new EmailTwoFactorNotEnabledException("Email two-factor authentication is not enabled for this user");
        }

        // Validate input parameters
        if (providedCode == null || expectedCode == null) {
            log.debug("Code verification failed for user: {} - null code provided", user.getUsername());
            return false;
        }

        // Trim whitespace to handle user input variations
        String trimmedProvidedCode = providedCode.trim();
        String trimmedExpectedCode = expectedCode.trim();

        // Basic format validation (should be 6 digits)
        if (!trimmedProvidedCode.matches("\\d{6}") || !trimmedExpectedCode.matches("\\d{6}")) {
            log.debug("Code verification failed for user: {} - invalid code format", user.getUsername());
            return false;
        }

        // Perform constant-time comparison to prevent timing attacks
        boolean isValid = constantTimeEquals(trimmedProvidedCode, trimmedExpectedCode);

        if (isValid) {
            log.debug("Code verification successful for user: {}", user.getUsername());

            // Reset failed attempts on successful verification
            TwoFactorAuth twoFactorAuth = emailTwoFactor.get();
            if (twoFactorAuth.getFailedAttempts() > 0) {
                twoFactorAuth.setFailedAttempts(0);
                twoFactorAuthRepository.save(twoFactorAuth);
                log.debug("Reset failed attempts counter for user: {}", user.getUsername());
            }
        } else {
            log.debug("Code verification failed for user: {} - codes do not match", user.getUsername());

            // Increment failed attempts counter
            TwoFactorAuth twoFactorAuth = emailTwoFactor.get();
            twoFactorAuth.setFailedAttempts(twoFactorAuth.getFailedAttempts() + 1);
            twoFactorAuthRepository.save(twoFactorAuth);
            log.debug("Incremented failed attempts counter to {} for user: {}",
                    twoFactorAuth.getFailedAttempts(), user.getUsername());
        }

        return isValid;
    }

    /**
     * Performs constant-time string comparison to prevent timing attacks.
     *
     * This method ensures that the comparison takes the same amount of time
     * regardless of where the strings differ, preventing attackers from
     * using timing information to guess the correct code character by character.
     *
     * @param a first string to compare
     * @param b second string to compare
     * @return true if strings are equal, false otherwise
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }

        return result == 0;
    }
}
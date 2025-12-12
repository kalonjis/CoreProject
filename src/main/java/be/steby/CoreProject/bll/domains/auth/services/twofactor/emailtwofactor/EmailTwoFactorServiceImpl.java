package be.steby.CoreProject.bll.domains.auth.services.twofactor.emailtwofactor;

import be.steby.CoreProject.bll.domains.auth.events.TwoFactorEnabledEvent;
import be.steby.CoreProject.bll.domains.auth.events.TwoFactorInitiateActivationEvent;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.EmailTwoFactorAlreadyEnabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.EmailTwoFactorNotEnabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.InvalidVerificationCodeException;
import be.steby.CoreProject.bll.domains.auth.models.EmailTwoFactorActivationBllRequest;
import be.steby.CoreProject.bll.domains.auth.models.TwoFactorActivationResult;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.exceptions.MaxAttemptsReachedException;
import be.steby.CoreProject.dal.repositories.TwoFactorAuthRepository;
import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.il.Jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
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
 * * - Rate limiting activation attempts
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
 * - Dual rate limiting: activation requests (emails) and verification attempts (codes)
 *
 * Rate limiting is implemented through two separate services:
 * - {@link EmailTwoFactorActivationAttemptService} - Limits email sending
 * - {@link EmailTwoFactorVerificationAttemptService} - Limits code verification attempts
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
    private final EmailTwoFactorActivationAttemptService emailTwoFactorActivationAttemptService;
    private final EmailTwoFactorVerificationAttemptService emailTwoFactorVerificationAttemptService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== PUBLIC API METHODS ====================

    @Override
    @Transactional
    public void enable() {
        User user = userService.getAuthenticatedUser();
        log.info("Enabling email 2FA for user: {}", user.getUsername());

        validateEmailTwoFactorNotAlreadyEnabled(user);

        // Check if a disabled email 2FA configuration already exists
        Optional<TwoFactorAuth> existingEmailTwoFactor = twoFactorAuthRepository
                .findByUserAndType(user, TwoFactorType.EMAIL);

        if (existingEmailTwoFactor.isPresent()) {
            reactivateExistingEmailTwoFactor(user, existingEmailTwoFactor.get());
        } else {
            createNewEmailTwoFactor(user);
        }

        log.info("Email 2FA enabled successfully for user: {}", user.getUsername());
    }

    @Override
    public TwoFactorActivationResult initiateActivation() {
        User user = userService.getAuthenticatedUser();
        log.info("Initiating email 2FA activation for user: {}", user.getUsername());

        // Rate limiting check for activation attempts (email sending)
        if (emailTwoFactorActivationAttemptService.hasExceededAttempts(user)) {
            log.warn("User {} exceeded email 2FA activation attempts", user.getUsername());
            throw new MaxAttemptsReachedException("Too many email 2FA activation attempts. Please try again later.");
        }

        validateEmailTwoFactorNotAlreadyEnabled(user);

        // Record activation attempt and generate activation data
        emailTwoFactorActivationAttemptService.recordAttempt(user);
        String setupVerificationCode = generateCode();

        // 🔒 SECURITY: Hash the verification code before storing in JWT
        String hashedCode = passwordEncoder.encode(setupVerificationCode);
        String activationToken = jwtUtil.generate2FAActivationToken(user, hashedCode);

        // Publish event to send verification email (plain code for email)
        TwoFactorInitiateActivationEvent event = new TwoFactorInitiateActivationEvent(
                user,
                setupVerificationCode  // Plain code sent via email
        );
        eventPublisher.publishEvent(event);

        log.info("Email 2FA activation initiated for user: {} (code hashed in token)", user.getUsername());
        return new TwoFactorActivationResult(TwoFactorType.EMAIL, activationToken);
    }

    @Override
    public void verifyAndActivateEmailTwoFactor(EmailTwoFactorActivationBllRequest request) {
        log.info("Starting email 2FA verification and activation process");

        // Validate activation token and extract claims
        Claims claims = jwtUtil.validate2FAActivationToken(request.activationToken());
        String userPublicId = claims.get("publicId", String.class);
        String hashedExpectedCode = claims.get("verificationCode", String.class);

        // Load user by publicId from token
        User user = userService.getUserByPublicId(userPublicId);
        log.debug("Processing 2FA activation for user: {}", user.getUsername());

        // Rate limiting check for verification attempts (code verification)
        if (emailTwoFactorVerificationAttemptService.hasExceededAttempts(user)) {
            log.warn("User {} exceeded email 2FA verification attempts", user.getUsername());
            throw new MaxAttemptsReachedException("Too many verification attempts. Please try again later.");
        }

        validateEmailTwoFactorNotAlreadyEnabled(user);

        // 🔒 SECURITY: Verify the provided code against the hashed code from JWT
        if (!verifyCodeAgainstHash(user, request.verificationCode(), hashedExpectedCode)) {
            log.warn("Invalid verification code provided during email 2FA activation for user: {}",
                    user.getUsername());
            // Record failed verification attempt
            emailTwoFactorVerificationAttemptService.recordAttempt(user);
            throw new InvalidVerificationCodeException("The verification code is incorrect", 400);
        }

        // Code verification successful - activate email 2FA
        createAndSaveEmailTwoFactorEntity(user);

        // Publish confirmation event and reset rate limiting
        TwoFactorEnabledEvent event = new TwoFactorEnabledEvent(user, TwoFactorType.EMAIL);
        eventPublisher.publishEvent(event);
        emailTwoFactorActivationAttemptService.resetAttempts(user);
        emailTwoFactorVerificationAttemptService.resetAttempts(user);

        log.info("Email 2FA successfully activated for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void disable() {
        User user = userService.getAuthenticatedUser();
        log.info("Disabling email 2FA for user: {}", user.getUsername());

        TwoFactorAuth emailTwoFactor = getEnabledEmailTwoFactor(user);

        emailTwoFactor.setEnabled(false);
        emailTwoFactor.setIsPrimary(false);
        emailTwoFactor.setDisabledAt(Instant.now());

        twoFactorAuthRepository.save(emailTwoFactor);
        log.info("Email 2FA disabled successfully for user: {} (2FA ID: {})",
                user.getUsername(), emailTwoFactor.getId());
    }

    @Override
    public String generateVerificationCode(User user) {
        log.debug("Generating verification code for user: {}", user.getUsername());

        validateEmailTwoFactorIsEnabled(user);
        return generateCode();
    }

    @Override
    public boolean verifyCode(User user, String providedCode, String hashedExpectedCode) {
        log.debug("Verifying 2FA code for user: {}", user.getUsername());

        TwoFactorAuth emailTwoFactor = getEnabledEmailTwoFactor(user);

        // Rate limiting check for verification attempts
        if (emailTwoFactorVerificationAttemptService.hasExceededAttempts(user)) {
            log.warn("User {} exceeded email 2FA verification attempts", user.getUsername());
            throw new MaxAttemptsReachedException("Too many verification attempts. Please try again later.");
        }

        // Always use password encoder for secure hash comparison
        boolean isValid = verifyCodeAgainstHash(user, providedCode, hashedExpectedCode);

        if (isValid) {
            log.debug("Code verification successful for user: {}", user.getUsername());
            // Reset failed attempts on successful verification
            emailTwoFactorVerificationAttemptService.resetAttempts(user);
        } else {
            log.debug("Code verification failed for user: {} - codes do not match", user.getUsername());
            // Record failed verification attempt
            emailTwoFactorVerificationAttemptService.recordAttempt(user);
        }

        return isValid;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Verifies a verification code against a hashed expected code.
     *
     * This is the single, secure method for all code verification operations.
     * Uses BCrypt password encoder for constant-time comparison and proper
     * salt handling, eliminating timing attack vulnerabilities.
     *
     * @param user the user attempting verification (for logging purposes)
     * @param providedCode the code provided by the user
     * @param hashedExpectedCode the BCrypt hashed expected code
     * @return true if codes match and are valid, false otherwise
     */
    private boolean verifyCodeAgainstHash(User user, String providedCode, String hashedExpectedCode) {
        // Validate input parameters
        if (providedCode == null || hashedExpectedCode == null) {
            log.debug("Code verification failed for user: {} - null code provided", user.getUsername());
            return false;
        }

        // Trim whitespace to handle user input variations
        String trimmedProvidedCode = providedCode.trim();

        // Basic format validation (should be 6 digits)
        if (!trimmedProvidedCode.matches("\\d{6}")) {
            log.debug("Code verification failed for user: {} - invalid code format", user.getUsername());
            return false;
        }

        // Use password encoder for secure, constant-time comparison
        boolean isValid = passwordEncoder.matches(trimmedProvidedCode, hashedExpectedCode);

        log.debug("Code verification {} for user: {}",
                isValid ? "successful" : "failed", user.getUsername());

        return isValid;
    }

    /**
     * Validates that email 2FA is not already enabled for the user.
     */
    private void validateEmailTwoFactorNotAlreadyEnabled(User user) {
        if (twoFactorAuthRepository.existsByUserAndTypeAndEnabledTrue(user, TwoFactorType.EMAIL)) {
            log.warn("Email 2FA already enabled for user: {}", user.getUsername());
            throw new EmailTwoFactorAlreadyEnabledException(
                    "Email two-factor authentication is already enabled"
            );
        }
    }

    /**
     * Validates that email 2FA is enabled for the user.
     */
    private void validateEmailTwoFactorIsEnabled(User user) {
        if (!twoFactorAuthRepository.existsByUserAndTypeAndEnabledTrue(user, TwoFactorType.EMAIL)) {
            log.warn("Attempted operation for user: {} but email 2FA is not enabled", user.getUsername());
            throw new EmailTwoFactorNotEnabledException(
                    "Email two-factor authentication is not enabled for this user"
            );
        }
    }

    /**
     * Gets the enabled email 2FA configuration for a user.
     */
    private TwoFactorAuth getEnabledEmailTwoFactor(User user) {
        return twoFactorAuthRepository
                .findByUserAndTypeAndEnabledTrue(user, TwoFactorType.EMAIL)
                .orElseThrow(() -> {
                    log.warn("Attempted operation for user: {} but email 2FA is not enabled", user.getUsername());
                    return new EmailTwoFactorNotEnabledException(
                            "Email two-factor authentication is not enabled for this user"
                    );
                });
    }

    /**
     * Demotes any existing primary 2FA method to non-primary.
     */
    private void demoteExistingPrimaryMethod(User user) {
        Optional<TwoFactorAuth> existingPrimary = twoFactorAuthRepository.findByUserAndIsPrimaryTrue(user);
        if (existingPrimary.isPresent()) {
            log.debug("Demoting existing primary 2FA method: {} for user: {}",
                    existingPrimary.get().getType(), user.getUsername());
            existingPrimary.get().setIsPrimary(false);
            twoFactorAuthRepository.save(existingPrimary.get());
        }
    }

    /**
     * Creates and saves a new email 2FA entity.
     */
    private TwoFactorAuth createAndSaveEmailTwoFactorEntity(User user) {
        demoteExistingPrimaryMethod(user);

        TwoFactorAuth emailTwoFactor = TwoFactorAuth.builder()
                .user(user)
                .type(TwoFactorType.EMAIL)
                .enabled(true)
                .isPrimary(true)
                .enabledAt(Instant.now())
                .failedAttempts(0)
                .label("Email verification")
                .build();

        TwoFactorAuth saved = twoFactorAuthRepository.save(emailTwoFactor);
        log.debug("Email 2FA entity saved successfully for user: {} (2FA ID: {})",
                user.getUsername(), saved.getId());
        return saved;
    }

    /**
     * Reactivates an existing disabled email 2FA configuration.
     */
    private void reactivateExistingEmailTwoFactor(User user, TwoFactorAuth emailTwoFactor) {
        log.debug("Reactivating existing email 2FA configuration (ID: {}) for user: {}",
                emailTwoFactor.getId(), user.getUsername());

        demoteExistingPrimaryMethod(user);

        emailTwoFactor.setEnabled(true);
        emailTwoFactor.setIsPrimary(true);
        emailTwoFactor.setEnabledAt(Instant.now());
        emailTwoFactor.setDisabledAt(null);
        emailTwoFactor.setFailedAttempts(0);

        TwoFactorAuth saved = twoFactorAuthRepository.save(emailTwoFactor);
        log.debug("Email 2FA reactivated successfully for user: {} (2FA ID: {})",
                user.getUsername(), saved.getId());
    }

    /**
     * Creates a new email 2FA configuration.
     */
    private void createNewEmailTwoFactor(User user) {
        log.debug("Creating new email 2FA configuration for user: {}", user.getUsername());
        createAndSaveEmailTwoFactorEntity(user);
    }

    /**
     * Validates verification code using secure comparison methods for plain text codes.
     * Performs comprehensive validation including null checks, format validation,
     * and constant-time comparison to prevent timing attacks.
     *
     * Note: This method is used for direct plain-text code comparison.
     * For hashed code verification, use {@link #verifyCodeAgainstHash(User, String, String)}.
     */


    /**
     * Generates a secure 6-digit verification code.
     */
    private String generateCode() {
        // Generate a secure 6-digit code
        // Range: 100000 to 999999 (inclusive)
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }
}
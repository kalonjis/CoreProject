package be.steby.CoreProject.bll.domains.auth.services.twofactor.smstwofactor;

import be.steby.CoreProject.bll.domains.auth.events.TwoFactorEnabledEvent;
import be.steby.CoreProject.bll.domains.auth.events.TwoFactorSmsActivationEvent;
import be.steby.CoreProject.bll.domains.auth.exceptions.phone.InvalidPhoneNumberException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.InvalidVerificationCodeException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.SmsTwoFactorAlreadyEnabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.SmsTwoFactorNotEnabledException;
import be.steby.CoreProject.bll.domains.auth.models.SmsTwoFactorActivationBllRequest;
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
 * Implementation of SmsTwoFactorService.
 * 
 * Handles SMS-based 2FA using phone numbers and SMS codes.
 * Similar to EmailTwoFactorService but uses SMS delivery.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsTwoFactorServiceImpl implements SmsTwoFactorService {
    
    private final TwoFactorAuthRepository twoFactorAuthRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SmsTwoFactorActivationAttemptService smsTwoFactorActivationAttemptService;
    private final SmsTwoFactorVerificationAttemptService smsTwoFactorVerificationAttemptService;
    private final JwtUtil jwtUtil;
    
    @Override
    @Transactional
    public void enable() {
        User user = userService.getAuthenticatedUser();
        log.info("Enabling SMS 2FA for user: {}", user.getUsername());
        
        // 1. Validate user has a phone number
        validatePhoneNumber(user);
        
        // 2. Check if SMS 2FA already exists
        if (twoFactorAuthRepository.existsByUserAndTypeAndEnabledTrue(user, TwoFactorType.SMS)) {
            log.warn("SMS 2FA already enabled for user: {}", user.getUsername());
            throw new SmsTwoFactorAlreadyEnabledException("SMS two-factor authentication is already enabled");
        }
        
        // 3. Disable any existing primary method (only one can be primary)
        twoFactorAuthRepository.findByUserAndIsPrimaryTrue(user)
            .ifPresent(existingPrimary -> {
                log.debug("Removing primary flag from existing method: {}", existingPrimary.getType());
                existingPrimary.setIsPrimary(false);
                twoFactorAuthRepository.save(existingPrimary);
            });
        
        // 4. Create new SMS 2FA configuration
        TwoFactorAuth smsTwoFactor = TwoFactorAuth.builder()
            .user(user)
            .type(TwoFactorType.SMS)
            .enabled(true)
            .isPrimary(true)
            .enabledAt(Instant.now())
            .failedAttempts(0)
            .label("SMS verification")
            .build();
        
        // 5. Save to database
        TwoFactorAuth saved = twoFactorAuthRepository.save(smsTwoFactor);
        log.info("SMS 2FA enabled successfully for user: {} (id: {})", 
                 user.getUsername(), saved.getId());
        
        // 6. Publish event for SMS notification (async)
        eventPublisher.publishEvent(new TwoFactorEnabledEvent(user, TwoFactorType.SMS));
        
        log.debug("SMS 2FA setup completed for user: {}", user.getUsername());
    }
    
    @Override
    @Transactional
    public void disable() {
        User user = userService.getAuthenticatedUser();
        log.info("Disabling SMS 2FA for user: {}", user.getUsername());
        
        // 1. Find SMS 2FA configuration
        TwoFactorAuth smsTwoFactor = twoFactorAuthRepository
            .findByUserAndTypeAndEnabledTrue(user, TwoFactorType.SMS)
            .orElseThrow(() -> {
                log.warn("SMS 2FA not enabled for user: {}", user.getUsername());
                return new SmsTwoFactorNotEnabledException("SMS two-factor authentication is not enabled");
            });
        
        // 2. Disable the configuration
        smsTwoFactor.setEnabled(false);
        smsTwoFactor.setIsPrimary(false);
        smsTwoFactor.setDisabledAt(Instant.now());
        
        // 3. Save changes
        twoFactorAuthRepository.save(smsTwoFactor);
        
        log.info("SMS 2FA disabled successfully for user: {}", user.getUsername());
    }
    
    @Override
    public String generateCode(User user) {
        log.debug("Generating SMS 2FA code for user: {}", user.getUsername());
        
        // 1. Verify SMS 2FA is enabled
        if (!twoFactorAuthRepository.existsByUserAndTypeAndEnabledTrue(user, TwoFactorType.SMS)) {
            throw new SmsTwoFactorNotEnabledException("SMS two-factor authentication is not enabled for this user");
        }
        
        // 2. Validate SMS number
        validatePhoneNumber(user);
        
        // 3. Generate secure 6-digit code
        int code = 100000 + secureRandom.nextInt(900000);
        String codeString = String.valueOf(code);
        
        log.debug("SMS 2FA code generated for user: {}", user.getUsername());
        return codeString;
    }
    
    @Override
    public boolean verifyCode(User user, String providedCode, String expectedCode) {
        log.debug("Verifying SMS 2FA code for user: {}", user.getUsername());
        
        // 1. Verify SMS 2FA is enabled
        if (!twoFactorAuthRepository.existsByUserAndTypeAndEnabledTrue(user, TwoFactorType.SMS)) {
            throw new SmsTwoFactorNotEnabledException("SMS two-factor authentication is not enabled for this user");
        }
        
        // 2. Perform constant-time comparison to prevent timing attacks
        boolean isValid = passwordEncoder.matches(providedCode, passwordEncoder.encode(expectedCode));
        
        log.debug("SMS 2FA code verification result for user {}: {}", user.getUsername(), isValid);
        return isValid;
    }


    @Override
    public TwoFactorActivationResult initiateActivation() {
        User user = userService.getAuthenticatedUser();
        log.info("Initiating SMS 2FA activation for user: {}", user.getUsername());

        // 1. Rate limiting check for activation attempts (SMS sending)
        if (smsTwoFactorActivationAttemptService.hasExceededAttempts(user)) {
            log.warn("User {} exceeded SMS 2FA activation attempts", user.getUsername());
            throw new MaxAttemptsReachedException("Too many SMS 2FA activation attempts. Please try again later.");
        }

        // 2. Validate SMS 2FA is not already enabled
        validateSmsTwoFactorNotAlreadyEnabled(user);

        // 3. Validate user has a verified phone number
        validatePhoneNumber(user);

        // 4. Record activation attempt and generate code
        smsTwoFactorActivationAttemptService.recordAttempt(user);
        String verificationCode = generateCode();

        // 5. Hash the verification code before storing in JWT
        String hashedCode = passwordEncoder.encode(verificationCode);
        String activationToken = jwtUtil.generate2FAActivationToken(user, hashedCode);

        // 6. Publish event to send SMS (plain code for SMS message)
        TwoFactorSmsActivationEvent event = new TwoFactorSmsActivationEvent(
                user,
                verificationCode,  // Plain code sent via SMS
                user.getPhoneNumber()
        );
        eventPublisher.publishEvent(event);

        log.info("SMS 2FA activation initiated for user: {} (code hashed in token)", user.getUsername());
        return new TwoFactorActivationResult(TwoFactorType.SMS, activationToken);
    }

    @Override
    @Transactional
    public void verifyAndActivateSmsTwoFactor(SmsTwoFactorActivationBllRequest request) {
        log.info("Starting SMS 2FA verification and activation process");

        // 1. Validate activation token and extract claims
        Claims claims = jwtUtil.validate2FAActivationToken(request.activationToken());
        String userPublicId = claims.get("publicId", String.class);
        String hashedExpectedCode = claims.get("verificationCode", String.class);

        // 2. Load user by publicId from token
        User user = userService.getUserByPublicId(userPublicId);
        log.debug("Processing SMS 2FA activation for user: {}", user.getUsername());

        // 3. Rate limiting check for verification attempts
        if (smsTwoFactorVerificationAttemptService.hasExceededAttempts(user)) {
            log.warn("User {} exceeded SMS 2FA verification attempts", user.getUsername());
            throw new MaxAttemptsReachedException("Too many verification attempts. Please try again later.");
        }

        // 4. Record verification attempt
        smsTwoFactorVerificationAttemptService.recordAttempt(user);

        // 5. Verify the provided code against hashed code
        if (!passwordEncoder.matches(request.verificationCode(), hashedExpectedCode)) {
            log.warn("Invalid SMS 2FA verification code for user: {}", user.getUsername());
            throw new InvalidVerificationCodeException("Invalid verification code");
        }

        // 6. Code is valid - activate SMS 2FA
        activateSmsTwoFactor(user);

        // 7. Clear rate limiting counters on success
        smsTwoFactorActivationAttemptService.clearAttempts(user);
        smsTwoFactorVerificationAttemptService.clearAttempts(user);

        log.info("SMS 2FA activated successfully for user: {}", user.getUsername());
    }

    // ===========================================================================
    // PRIVATE HELPER METHODS
    // ===========================================================================

    /**
     * Validates that SMS 2FA is not already enabled for the user.
     */
    private void validateSmsTwoFactorNotAlreadyEnabled(User user) {
        if (twoFactorAuthRepository.existsByUserAndTypeAndEnabledTrue(user, TwoFactorType.SMS)) {
            log.warn("SMS 2FA already enabled for user: {}", user.getUsername());
            throw new SmsTwoFactorAlreadyEnabledException("SMS two-factor authentication is already enabled");
        }
    }

    /**
     * Generates a secure 6-digit verification code.
     */
    private String generateCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Activates SMS 2FA for the user (creates or reactivates configuration).
     */
    private void activateSmsTwoFactor(User user) {
        // Check if a disabled SMS 2FA configuration already exists
        Optional<TwoFactorAuth> existingSms = twoFactorAuthRepository
                .findByUserAndType(user, TwoFactorType.SMS);

        if (existingSms.isPresent()) {
            reactivateExistingSmsTwoFactor(user, existingSms.get());
        } else {
            createNewSmsTwoFactor(user);
        }

        // Publish enabled event
        eventPublisher.publishEvent(new TwoFactorEnabledEvent(user, TwoFactorType.SMS));
    }

    /**
     * Reactivates an existing disabled SMS 2FA configuration.
     */
    private void reactivateExistingSmsTwoFactor(User user, TwoFactorAuth existing) {
        log.debug("Reactivating existing SMS 2FA for user: {}", user.getUsername());

        // Demote any existing primary method
        twoFactorAuthRepository.findByUserAndIsPrimaryTrue(user)
                .ifPresent(primary -> {
                    primary.setIsPrimary(false);
                    twoFactorAuthRepository.save(primary);
                });

        // Reactivate
        existing.setEnabled(true);
        existing.setIsPrimary(true);
        existing.setEnabledAt(Instant.now());
        existing.setDisabledAt(null);
        existing.setFailedAttempts(0);

        twoFactorAuthRepository.save(existing);
        log.info("SMS 2FA reactivated for user: {}", user.getUsername());
    }

    /**
     * Creates a new SMS 2FA configuration.
     */
    private void createNewSmsTwoFactor(User user) {
        log.debug("Creating new SMS 2FA for user: {}", user.getUsername());

        // Demote any existing primary method
        twoFactorAuthRepository.findByUserAndIsPrimaryTrue(user)
                .ifPresent(primary -> {
                    primary.setIsPrimary(false);
                    twoFactorAuthRepository.save(primary);
                });

        // Create new configuration
        TwoFactorAuth smsTwoFactor = TwoFactorAuth.builder()
                .user(user)
                .type(TwoFactorType.SMS)
                .enabled(true)
                .isPrimary(true)
                .enabledAt(Instant.now())
                .failedAttempts(0)
                .label("SMS verification")
                .build();

        twoFactorAuthRepository.save(smsTwoFactor);
        log.info("SMS 2FA created for user: {}", user.getUsername());
    }
    
    /**
     * Checks if the user has a valid SMS number for SMS.
     *
     * @param user The user to check
     *
     */
    private void validatePhoneNumber (User user) {
        if (user.getPhoneNumber() == null || user.getPhoneNumber().trim().isEmpty()) {
            log.info("SMS is null or empty");
            throw new InvalidPhoneNumberException("No SMS number configured for SMS 2FA");
        }
        if (!user.isPhoneNumberVerified()) {
            throw new InvalidPhoneNumberException("Phone number must be verified before enabling SMS 2FA");
        }

    }
}
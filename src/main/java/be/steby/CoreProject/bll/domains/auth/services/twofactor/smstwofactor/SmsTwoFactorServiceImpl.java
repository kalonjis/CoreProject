package be.steby.CoreProject.bll.domains.auth.services.twofactor.smstwofactor;

import be.steby.CoreProject.bll.domains.auth.events.TwoFactorEnabledEvent;
import be.steby.CoreProject.bll.domains.auth.exceptions.phone.InvalidPhoneNumberException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.SmsTwoFactorAlreadyEnabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.twofactor.SmsTwoFactorNotEnabledException;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dal.repositories.TwoFactorAuthRepository;
import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

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
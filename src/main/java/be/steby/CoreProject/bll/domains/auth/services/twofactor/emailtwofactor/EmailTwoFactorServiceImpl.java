package be.steby.CoreProject.bll.domains.auth.services.twofactor.emailtwofactor;

import be.steby.CoreProject.bll.domains.auth.events.TwoFactorEnabledEvent;
import be.steby.CoreProject.bll.domains.auth.exceptions.TwoFactorAlreadyEnabledException;
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
 * Implementation of EmailTwoFactorService.
 * Handles all EMAIL-specific 2FA logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTwoFactorServiceImpl implements EmailTwoFactorService {
    
    private final TwoFactorAuthRepository twoFactorAuthRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Override
    @Transactional
    public void enableEmailTwoFactor(User user) {
        // If user is null, get authenticated user
        if (user == null) {
            user = userService.getAuthenticatedUser();
        }
        
        log.info("Enabling EMAIL 2FA for user: {}", user.getUsername());
        
        // Check if EMAIL 2FA already exists
        if (twoFactorAuthRepository.existsByUserAndTypeAndEnabledTrue(user, TwoFactorType.EMAIL)) {
            log.warn("EMAIL 2FA already enabled for user: {}", user.getUsername());
            throw TwoFactorAlreadyEnabledException.forType("EMAIL");
        }
        
        // Disable any existing primary method
        disableExistingPrimaryMethod(user);
        
        // Create new EMAIL 2FA configuration
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
        log.info("EMAIL 2FA enabled successfully for user: {} (id: {})", 
                 user.getUsername(), saved.getId());
        
        // Publish event for email notification
        eventPublisher.publishEvent(new TwoFactorEnabledEvent(user, TwoFactorType.EMAIL));
        
        log.debug("EMAIL 2FA setup completed for user: {}", user.getUsername());
    }
    
    @Override
    public String generateVerificationCode() {
        StringBuilder code = new StringBuilder();
        
        for (int i = 0; i < 6; i++) {
            code.append(secureRandom.nextInt(10));
        }
        
        return code.toString();
    }
    
    @Override
    public String hashVerificationCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Verification code cannot be null or empty");
        }
        
        return passwordEncoder.encode(code);
    }
    
    @Override
    public boolean verifyCode(String providedCode, String hashedCode) {
        if (providedCode == null || hashedCode == null) {
            log.warn("Cannot verify email code: provided or hashed code is null");
            return false;
        }
        
        String normalizedCode = providedCode.trim().replaceAll("\\s+", "");
        
        if (!normalizedCode.matches("^\\d{6}$")) {
            log.warn("Invalid email verification code format provided");
            return false;
        }
        
        return passwordEncoder.matches(normalizedCode, hashedCode);
    }
    
    /**
     * Disable existing primary 2FA method for user
     */
    private void disableExistingPrimaryMethod(User user) {
        twoFactorAuthRepository.findByUserAndIsPrimaryTrue(user)
            .ifPresent(existingPrimary -> {
                log.debug("Removing primary flag from existing method: {}", existingPrimary.getType());
                existingPrimary.setIsPrimary(false);
                twoFactorAuthRepository.save(existingPrimary);
            });
    }
}
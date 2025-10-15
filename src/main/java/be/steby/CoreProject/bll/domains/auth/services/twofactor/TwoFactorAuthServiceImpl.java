package be.steby.CoreProject.bll.domains.auth.services.twofactor;

import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dal.repositories.TwoFactorAuthRepository;
import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Implementation of TwoFactorAuthService.
 * 
 * Currently supports EMAIL-based 2FA only.
 * Other methods (TOTP, SMS, WebAuthn) will be added incrementally.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthServiceImpl implements TwoFactorAuthService {
    
    private final TwoFactorAuthRepository twoFactorAuthRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    @Transactional
    public void enableEmailTwoFactor() {
        User user = userService.getAuthenticatedUser();
        log.info("Enabling EMAIL 2FA for user: {}", user.getUsername());
        
        // 1. Check if EMAIL 2FA already exists
        if (twoFactorAuthRepository.existsByUserAndTypeAndEnabledTrue(user, TwoFactorType.EMAIL)) {
            log.warn("EMAIL 2FA already enabled for user: {}", user.getUsername());
            throw new IllegalStateException("EMAIL two-factor authentication is already enabled");
        }
        
        // 2. Disable any existing primary method (only one can be primary)
        twoFactorAuthRepository.findByUserAndIsPrimaryTrue(user)
            .ifPresent(existingPrimary -> {
                log.debug("Removing primary flag from existing method: {}", existingPrimary.getType());
                existingPrimary.setIsPrimary(false);
                twoFactorAuthRepository.save(existingPrimary);
            });
        
        // 3. Create new EMAIL 2FA configuration
        TwoFactorAuth emailTwoFactor = TwoFactorAuth.builder()
            .user(user)
            .type(TwoFactorType.EMAIL)
            .enabled(true)
            .isPrimary(true)
            .enabledAt(Instant.now())
            .failedAttempts(0)
            .label("Email verification")
            .build();
        
        // 4. Save to database
        TwoFactorAuth saved = twoFactorAuthRepository.save(emailTwoFactor);
        log.info("EMAIL 2FA enabled successfully for user: {} (id: {})", 
                 user.getUsername(), saved.getId());
        
        // 5. Publish event for email notification (async)
        // TODO: Create TwoFactorEnabledEvent and listener to send confirmation email
        // eventPublisher.publishEvent(new TwoFactorEnabledEvent(user, TwoFactorType.EMAIL));
        
        log.debug("EMAIL 2FA setup completed for user: {}", user.getUsername());
    }
}
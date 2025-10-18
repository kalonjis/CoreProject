package be.steby.CoreProject.bll.domains.auth.services.twofactor;

import be.steby.CoreProject.bll.domains.auth.events.TwoFactorVerificationRequestedEvent;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.emailtwofactor.EmailTwoFactorService;
import be.steby.CoreProject.bll.domains.auth.services.twofactor.totptwofactor.TOTPTwoFactorService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.il.Jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Factory for routing 2FA operations to appropriate specialized services.
 *
 * This factory handles:
 * - 2FA initiation (during login)
 * - 2FA verification (during login completion)
 * - Code generation and routing by type
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TwoFactorVerificationFactory {

    private final EmailTwoFactorService emailTwoFactorService;
    private final TOTPTwoFactorService totpTwoFactorService;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Initiate 2FA process during login.
     * Routes to appropriate service based on user's 2FA type.
     */
    public TwoFactorInitiationResult initiateTwoFactor(User user, TwoFactorType type, HttpServletRequest request) {
        log.debug("Initiating 2FA for user: {} with type: {}", user.getUsername(), type);

        return switch (type) {
            case EMAIL -> initiateEmailTwoFactor(user, request);
            case TOTP -> initiateTOTPTwoFactor(user);
            default -> throw new UnsupportedOperationException("2FA type not supported: " + type);
        };
    }

    /**
     * Verify 2FA code during login completion.
     * Routes to appropriate service based on type.
     */
    public boolean verifyCode(String code, User user, TwoFactorType type) {
        log.debug("Verifying 2FA code for user: {} with type: {}", user.getUsername(), type);

        return switch (type) {
            case EMAIL -> {
                // EMAIL verification handled differently - via JWT hash comparison
                log.warn("EMAIL 2FA verification should use verifyEmailCode() method");
                yield false;
            }
            case TOTP -> totpTwoFactorService.verifyCode(code, user);
            default -> {
                log.error("Unknown 2FA type: {}", type);
                yield false;
            }
        };
    }

    /**
     * Verify EMAIL code against JWT hash.
     */
    public boolean verifyEmailCode(String providedCode, String hashedCode) {
        return emailTwoFactorService.verifyCode(providedCode, hashedCode);
    }

    // =========================================================================
    // PRIVATE INITIATION METHODS
    // =========================================================================

    /**
     * Initiate EMAIL 2FA - generate code, hash it, create JWT, send email
     */
    private TwoFactorInitiationResult initiateEmailTwoFactor(User user, HttpServletRequest request) {
        // Generate verification code
        String verificationCode = emailTwoFactorService.generateVerificationCode();
        String verificationCodeHash = emailTwoFactorService.hashVerificationCode(verificationCode);

        // Generate JWT token with hash
        String twoFactorToken = jwtUtil.generate2FAToken(user, verificationCodeHash, TwoFactorType.EMAIL);

        // Send verification email
        eventPublisher.publishEvent(new TwoFactorVerificationRequestedEvent(
                user, TwoFactorType.EMAIL, verificationCode, request
        ));

        // Mask email for response
        String maskedContact = maskEmail(user.getEmail());

        return new TwoFactorInitiationResult(twoFactorToken, maskedContact);
    }

    /**
     * Initiate TOTP 2FA - just create JWT, no code generation needed
     */
    private TwoFactorInitiationResult initiateTOTPTwoFactor(User user) {
        // Generate JWT token without hash (TOTP doesn't need stored code)
        String twoFactorToken = jwtUtil.generate2FAToken(user, null, TwoFactorType.TOTP);

        // Contact is authenticator app
        String maskedContact = "Authenticator App";

        return new TwoFactorInitiationResult(twoFactorToken, maskedContact);
    }

    /**
     * Mask email for security
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***.***";
        }

        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];

        String maskedLocal = localPart.length() > 2
                ? localPart.substring(0, 2) + "*".repeat(localPart.length() - 2)
                : "*".repeat(localPart.length());

        return maskedLocal + "@" + domainPart;
    }
}

/**
 * Result of 2FA initiation process
 */
record TwoFactorInitiationResult(
        String twoFactorToken,
        String maskedContact
) {}
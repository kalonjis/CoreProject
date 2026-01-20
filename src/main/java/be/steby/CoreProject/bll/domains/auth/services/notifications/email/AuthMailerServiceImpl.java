package be.steby.CoreProject.bll.domains.auth.services.notifications.email;

import be.steby.CoreProject.bll.common.exceptions.mail.MailDeliveryException;
import be.steby.CoreProject.bll.common.services.notification.mailer.BaseMailerService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.il.mail.EmailComposer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Auth domain-specific mailer service.
 *
 * Extends BaseMailerService to inherit common email utilities.
 * Handles all email notifications related to authentication operations:
 * - Two-factor authentication verification codes
 * - 2FA success notifications
 * - Security alerts for failed attempts
 * - 2FA setup confirmations
 *
 * Provides both async and sync methods:
 * - Async methods: For non-critical notifications (fire-and-forget)
 * - Sync methods: For time-sensitive codes (throws on failure)
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Service
@Slf4j
public class AuthMailerServiceImpl extends BaseMailerService implements AuthMailerService {

    @Value("${two-factor-auth.verification-code.expiration:300000}")
    private long codeExpirationMs;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    public AuthMailerServiceImpl(EmailComposer emailComposer) {
        super(emailComposer);
    }

    // =========================================================================
    // ASYNC METHODS (fire-and-forget)
    // =========================================================================

    @Override
    public void sendTwoFactorVerificationCode(User user, String verificationCode, HttpServletRequest httpRequest) {
        log.info("Sending 2FA verification code to user: {} (async)", user.getEmail());

        try {
            Context context = createTwoFactorCodeContext(user, verificationCode);

            sendEmail(
                    "Code de vérification",
                    "auth/two-factor-verification-code",
                    context,
                    user.getEmail()
            );

            log.info("2FA verification code email queued successfully for: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to queue 2FA verification code email for: {} - Error: {}",
                    user.getEmail(), e.getMessage(), e);
            // Don't rethrow - async method should not fail the main flow
        }
    }

    @Override
    public void sendTwoFactorActivationCode(User user, String verificationCode) {
        log.info("Sending 2FA activation verification code to user: {} (async)", user.getEmail());

        try {
            Context context = createTwoFactorCodeContext(user, verificationCode);

            sendEmail(
                    "Code d'activation - Authentification à deux facteurs",
                    "auth/two-factor-activation-code",
                    context,
                    user.getEmail()
            );

            log.info("2FA activation verification code email queued successfully for: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to queue 2FA activation verification code email for: {} - Error: {}",
                    user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send activation verification code email", e);
        }
    }

    @Override
    public void sendTwoFactorEnabledConfirmation(User user, TwoFactorType twoFactorType) {
        log.info("Sending 2FA enabled confirmation to user: {} (async)", user.getEmail());

        try {
            Context context = createBaseContext(user);
            context.setVariable("twoFactorType", twoFactorType.getDisplayName());

            sendEmail(
                    "Authentification à deux facteurs activée",
                    "auth/twoFactorEnabled",
                    context,
                    user.getEmail()
            );

            log.info("2FA enabled confirmation email queued successfully for: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to queue 2FA enabled confirmation email for: {} - Error: {}",
                    user.getEmail(), e.getMessage(), e);
            // Don't rethrow - confirmation email failure should not affect the flow
        }
    }

    // =========================================================================
    // SYNC METHODS (blocking, for critical messages)
    // =========================================================================

    @Override
    public void sendTwoFactorCodeSync(User user, String verificationCode, HttpServletRequest httpRequest)
            throws MailDeliveryException {
        log.info("Sending 2FA verification code to user: {} (sync)", user.getEmail());

        Context context = createTwoFactorCodeContext(user, verificationCode);

        // This will throw MailDeliveryException if delivery fails
        sendEmailSync(
                "Code de vérification",
                "auth/two-factor-verification-code",
                context,
                user.getEmail()
        );

        log.info("2FA verification code email sent successfully to: {} (sync)", user.getEmail());
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Creates context for 2FA verification code emails.
     * Shared by both async and sync methods.
     *
     * @param user the recipient user
     * @param verificationCode the 6-digit code
     * @return populated context for the email template
     */
    private Context createTwoFactorCodeContext(User user, String verificationCode) {
        LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(codeExpirationMs / 1000);

        Context context = createBaseContext(user);
        context.setVariable("verificationCode", verificationCode);
        context.setVariable("expirationTime", expirationTime.format(DATE_TIME_FORMATTER));

        return context;
    }
}
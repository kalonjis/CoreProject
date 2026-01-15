package be.steby.CoreProject.bll.domains.auth.services.notifications.email;

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
 * Authentication domain-specific mailer service.
 * Extends BaseMailerService to inherit common email utilities.
 * Handles all email notifications related to authentication operations:
 * - Two-factor authentication verification codes
 * - 2FA success notifications
 * - Security alerts for failed attempts
 * - 2FA setup confirmations
 *
 * All email operations are designed to be non-blocking and failure-tolerant.
 * Failures in email sending should not affect the core authentication flow.
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Service
@Slf4j
public class AuthMailerServiceImpl extends BaseMailerService implements AuthMailerService {


    @Value("${two-factor-auth.verification-code.expiration:300000}")
    private long codeExpirationMs;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
    //private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public AuthMailerServiceImpl(EmailComposer emailComposer) {
        super(emailComposer);
    }

    @Override
    public void sendTwoFactorVerificationCode(User user, String verificationCode, HttpServletRequest httpRequest) {
        log.info("Sending 2FA verification code to user: {}", user.getEmail());

        try {

            // Calculate expiration time
            LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(codeExpirationMs / 1000);
            int expirationMinutes = (int) (codeExpirationMs / 60000);

            // Create context using inherited method
            Context context = createBaseContext(user);

            // Add 2FA-specific variables - MINIMUM VIABLE
            context.setVariable("verificationCode", verificationCode);
            context.setVariable("expirationTime", expirationTime.format(DATE_TIME_FORMATTER));

            // Send email using inherited method
            sendEmail(
                    "Code de vérification",
                    "auth/two-factor-verification-code",
                    context,
                    user.getEmail()
            );

            log.info("2FA verification code email sent successfully to: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send 2FA verification code email to: {} - Error: {}",
                    user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send verification code email", e);
        }
    }


    @Override
    public void sendTwoFactorEnabledConfirmation(User user, TwoFactorType twoFactorType) {
        log.info("Sending 2FA enabled confirmation to user: {}", user.getEmail());

        try {
            Context context = createBaseContext(user);
            context.setVariable("twoFactorType", twoFactorType.getDisplayName());

            sendEmail(
                    "Authentification à deux facteurs activée",
                    "auth/twoFactorEnabled",
                    context,
                    user.getEmail()
            );

            log.info("2FA enabled confirmation sent successfully to: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send 2FA enabled confirmation to: {} - Error: {}",
                    user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Sends a two-factor authentication activation verification code via email.
     *
     * This method sends an email containing a 6-digit verification code during
     * the 2FA setup/activation process. The email template is specific to the
     * activation flow and includes clear instructions for completing the setup.
     *
     * This is different from login verification emails as it's part of the
     * initial setup process rather than ongoing authentication.
     *
     * @param user the user who initiated the 2FA activation
     * @param verificationCode the 6-digit code for verification
     * @throws RuntimeException if email sending fails
     */
    @Override
    public void sendTwoFactorActivationCode(User user, String verificationCode) {
        log.info("Sending 2FA activation verification code to user: {}", user.getEmail());

        try {
            // Calculate expiration time
            LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(codeExpirationMs / 1000);

            // Create context using inherited method
            Context context = createBaseContext(user);

            // Add 2FA activation-specific variables
            context.setVariable("verificationCode", verificationCode);
            context.setVariable("expirationTime", expirationTime.format(DATE_TIME_FORMATTER));

            // Send email using inherited method
            sendEmail(
                    "Code d'activation - Authentification à deux facteurs",
                    "auth/two-factor-activation-code",
                    context,
                    user.getEmail()
            );

            log.info("2FA activation verification code email sent successfully to: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send 2FA activation verification code email to: {} - Error: {}",
                    user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send activation verification code email", e);
        }
    }

}
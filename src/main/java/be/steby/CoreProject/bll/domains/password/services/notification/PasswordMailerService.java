package be.steby.CoreProject.bll.domains.password.services.notification;

import be.steby.CoreProject.bll.common.services.mailer.BaseMailerService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.utils.MailerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

/**
 * Password domain-specific mailer service.
 * 
 * <p>Extends BaseMailerService to inherit common email utilities.
 * Handles all email notifications related to password operations:
 * <ul>
 *   <li>Password reset requests with reset links</li>
 *   <li>Password reset token refresh notifications</li>
 *   <li>Password change confirmation messages</li>
 *   <li>Password security alerts</li>
 * </ul>
 *
 * <p>All password-related email operations are designed to be non-blocking and failure-tolerant.
 * Email delivery failures should not affect the core password management flow.
 */
@Service
@Slf4j
public class PasswordMailerService extends BaseMailerService {

    public PasswordMailerService(MailerUtil mailerUtil) {
        super(mailerUtil);
    }

    /**
     * Sends password reset email with reset link to user.
     * 
     * <p>This is the traditional password reset flow where users receive an email
     * containing a secure link to reset their password. The link contains a token
     * that expires after a configured period.
     *
     * @param tokenPublicId the public ID of the password reset token
     * @param user the user requesting password reset
     */
    @Async("emailExecutor")
    public void sendPasswordReset(String tokenPublicId, User user) {
        log.info("Sending password reset email to: {}", user.getEmail());

        String resetUrl = buildUrl("/password/reset", "token", tokenPublicId);

        Context context = createBaseContext(user);
        context.setVariable("url", resetUrl);
        context.setVariable("token", tokenPublicId);

        sendEmail("Password Reset Request", "passwords/NewPasswordRequest", context, user.getEmail());

        log.debug("Password reset email sent successfully to: {}", user.getEmail());
    }

    /**
     * Sends password reset token refresh email to user.
     * 
     * <p>This notification is sent when a user requests a new password reset token
     * because their previous token expired. It provides a fresh reset link while
     * invalidating the old one.
     *
     * @param newTokenPublicId the public ID of the new password reset token
     * @param user the user requesting token refresh
     */
    @Async("emailExecutor")
    public void sendPasswordResetRefresh(String newTokenPublicId, User user) {
        log.info("Sending password reset token refresh email to: {}", user.getEmail());

        String resetUrl = buildUrl("/password/reset", "token", newTokenPublicId);

        Context context = createBaseContext(user);
        context.setVariable("resetUrl", resetUrl);
        context.setVariable("newTokenPublicId", newTokenPublicId);

        sendEmail("Password Reset - New Link", "passwords/passwordResetRefresh", context, user.getEmail());

        log.debug("Password reset refresh email sent successfully to: {}", user.getEmail());
    }

    /**
     * Sends password change confirmation email to user.
     * 
     * <p>This security notification is sent whenever a user successfully changes
     * their password to confirm the action was authorized. If the user did not
     * initiate this change, they are advised to contact support immediately.
     *
     * @param user the user whose password was changed
     */
    @Async("emailExecutor")
    public void sendPasswordChangeConfirmation(User user) {
        log.info("Sending password change confirmation email to: {}", user.getEmail());

        Context context = createBaseContext(user);
        // Add any additional security information (timestamp, location, etc.)

        sendEmail("Password Changed Successfully", "passwords/passwordChangeConfirmation", context, user.getEmail());

        log.debug("Password change confirmation email sent successfully to: {}", user.getEmail());
    }

//    /**
//     * Sends password reset via SMS code notification email.
//     *
//     * <p>This notification is sent when a user requests password reset via SMS
//     * to inform them that a verification code has been sent to their phone.
//     * This serves as a security confirmation and fallback notification.
//     *
//     * @param user the user who requested SMS password reset
//     * @param maskedPhoneNumber the masked phone number for security
//     */
//    @Async("emailExecutor")
//    public void sendSmsPasswordResetNotification(User user, String maskedPhoneNumber) {
//        log.info("Sending SMS password reset notification email to: {}", user.getEmail());
//
//        Context context = createBaseContext(user);
//        context.setVariable("maskedPhoneNumber", maskedPhoneNumber);
//
//        sendEmail("Password Reset Code Sent", "passwords/smsPasswordResetNotification", context, user.getEmail());
//
//        log.debug("SMS password reset notification email sent successfully to: {}", user.getEmail());
//    }
}
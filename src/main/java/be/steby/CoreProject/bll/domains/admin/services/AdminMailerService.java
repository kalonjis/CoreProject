package be.steby.CoreProject.bll.domains.admin.services;

import be.steby.CoreProject.bll.common.services.mailer.BaseMailerService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.utils.MailerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

/**
 * Admin domain-specific mailer service.
 * Handles all email notifications related to admin operations:
 * - Password reset links sent by administrators
 */
@Service
@Slf4j
public class AdminMailerService extends BaseMailerService {

    public AdminMailerService(MailerUtil mailerUtil) {
        super(mailerUtil);
    }

    /**
     * Sends password reset link email to user (initiated by admin).
     *
     * @param tokenPublicId The public ID of the password reset token
     * @param targetUser The user who will receive the reset link
     * @param adminUser The administrator who initiated the reset
     * @param reason The reason for the password reset
     */
    @Async("emailExecutor")
    public void sendPasswordResetLink(String tokenPublicId, User targetUser, User adminUser, String reason) {
        log.info("Sending admin-initiated password reset link email to: {} (initiated by: {})",
                targetUser.getEmail(), adminUser.getUsername());

        String resetUrl = buildUrl("/auth/reset-password", "token", tokenPublicId);

        Context context = createBaseContext(targetUser);
        context.setVariable("resetUrl", resetUrl);
        context.setVariable("adminUsername", defineUsername(adminUser));
        context.setVariable("reason", reason);
        context.setVariable("tokenPublicId", tokenPublicId);

        sendEmail("Password Reset Request", "admin/passwordResetLink", context, targetUser.getEmail());

        log.debug("Admin-initiated password reset link email sent successfully to: {}", targetUser.getEmail());
    }


    /**
     * Sends temporary password email to user (initiated by admin).
     *
     * @param temporaryPassword The temporary password (plain text)
     * @param targetUser The user who will receive the temporary password
     * @param adminUser The administrator who initiated the password reset
     * @param reason The reason for the temporary password
     */
    @Async("emailExecutor")
    public void sendTemporaryPassword(String temporaryPassword, User targetUser, User adminUser, String reason) {
        log.info("Sending admin-initiated temporary password email to: {} (initiated by: {})",
                targetUser.getEmail(), adminUser.getUsername());

        Context context = createBaseContext(targetUser);
        context.setVariable("temporaryPassword", temporaryPassword);
        context.setVariable("adminUsername", defineUsername(adminUser));
        context.setVariable("reason", reason);

        sendEmail("Temporary Password - Action Required", "admin/temporaryPassword", context, targetUser.getEmail());

        log.debug("Admin-initiated temporary password email sent successfully to: {}", targetUser.getEmail());
    }



    /**
     * Sends temporary password to ALTERNATIVE email address.
     * Used when primary email is compromised.
     *
     * @param temporaryPassword The temporary password (plain text)
     * @param targetUser The user who will receive the temporary password
     * @param adminUser The administrator who initiated the password reset
     * @param reason The reason for the temporary password
     * @param alternativeEmail The alternative email address to send to
     */
    @Async("emailExecutor")
    public void sendTemporaryPasswordToAlternativeEmail(
            String temporaryPassword,
            User targetUser,
            User adminUser,
            String reason,
            String alternativeEmail) {

        log.warn("⚠️ Sending temporary password to ALTERNATIVE email: {} for user: {} (initiated by: {})",
                alternativeEmail, targetUser.getEmail(), adminUser.getUsername());

        Context context = createBaseContext(targetUser);
        context.setVariable("temporaryPassword", temporaryPassword);
        context.setVariable("adminUsername", defineUsername(adminUser));
        context.setVariable("reason", reason);
        context.setVariable("alternativeEmail", alternativeEmail);
        context.setVariable("primaryEmail", targetUser.getEmail());
        context.setVariable("securityWarning",
                "This password was sent to an alternative email address because the primary email may be compromised.");

        sendEmail(
                "⚠️ SECURITY ALERT - Temporary Password (Alternative Channel)",
                "admin/temporaryPasswordAlternative",
                context,
                alternativeEmail // ← Envoi à l'email ALTERNATIF
        );

        log.debug("✅ Temporary password sent to alternative email: {}", alternativeEmail);
    }


}
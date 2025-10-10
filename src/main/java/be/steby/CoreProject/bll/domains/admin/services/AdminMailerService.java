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
}
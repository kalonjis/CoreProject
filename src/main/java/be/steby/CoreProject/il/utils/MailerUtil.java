package be.steby.CoreProject.il.utils;

import be.steby.CoreProject.bll.exceptions.EmailSendingException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailerUtil {

    /**
     * Email address used as the sender's address for all outgoing emails.
     */
    @Value("${spring.mail.username}")
    private String appEmailAddress;

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /**
     * Sends an email with a given subject, template, context data, and recipient(s).
     * The email content is generated using a Thymeleaf template engine.
     *
     * @param subject      Subject of the email
     * @param templateName Name of the Thymeleaf template to use
     * @param context      Context data to fill the template
     * @param to           Recipients of the email
     */
    @Async
    public void sendMail(String subject, String templateName, Context context, String... to) {
        // Process the email template with the provided context

        log.info("Async task is running on thread: {}", Thread.currentThread().getName());
        String htmlContent = templateEngine.process("emails/" + templateName, context);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

            helper.setFrom(appEmailAddress);  // Set the sender's address
            helper.setTo(to);                 // Set the recipient(s)
            helper.setSubject(subject);       // Set the subject of the email
            helper.setText(htmlContent, true); // Set the email content as HTML

            log.info("Email sent successfully to: {}", (Object[]) to);  // Log pour confirmer l'envoi de l'email
            mailSender.send(mimeMessage);     // Send the email

        } catch (MessagingException e) {
            throw new EmailSendingException(e.getMessage());    // Rethrow as runtime exception if an error occurs
        }

    }
}

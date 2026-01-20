package be.steby.CoreProject.il.mail;

import be.steby.CoreProject.bll.common.exceptions.mail.MailDeliveryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Composes emails from Thymeleaf templates.
 *
 * Responsibilities:
 * - Process Thymeleaf templates to HTML
 * - Create immutable EmailMessage objects
 * - Delegate sending to SmtpMailSender (with resilience)
 *
 * Provides two sending modes:
 * - {@link #sendMail}: Asynchronous with fallback (fire-and-forget)
 * - {@link #sendMailSync}: Synchronous without fallback (throws on failure)
 *
 * This class does NOT handle:
 * - SMTP communication (delegated to SmtpMailSender)
 * - Circuit breaker logic (handled by SmtpMailSender)
 * - Retry logic (handled by SmtpMailSender)
 *
 * Architecture:
 * MailerServiceImpl → EmailComposer → SmtpMailSender → SMTP Server
 *                     (this class)    (resilience)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailComposer {

    private final TemplateEngine templateEngine;
    private final SmtpMailSender smtpMailSender;

    /**
     * Composes and sends an email asynchronously using a Thymeleaf template.
     *
     * If delivery fails, the email is queued for later retry.
     * Use for non-critical emails where delayed delivery is acceptable.
     *
     * @param subject      subject of the email
     * @param templateName name of the Thymeleaf template (without "emails/" prefix)
     * @param context      context data to fill the template
     * @param to           recipient email addresses
     */
    @Async
    public void sendMail(String subject, String templateName, Context context, String... to) {
        log.debug("Composing email '{}' for: {} (async)", subject, to);
        log.debug("Async task running on thread: {}", Thread.currentThread().getName());

        EmailMessage message = composeMessage(subject, templateName, context, to);
        smtpMailSender.send(message);

        log.debug("Email '{}' composed and sent to SmtpMailSender (async)", subject);
    }

    /**
     * Composes and sends an email synchronously using a Thymeleaf template.
     *
     * If delivery fails, throws immediately instead of queuing for retry.
     * Use for time-sensitive emails where delayed delivery is useless:
     * - 2FA verification codes
     * - Password reset tokens
     * - Any OTP-based authentication
     *
     * @param subject      subject of the email
     * @param templateName name of the Thymeleaf template (without "emails/" prefix)
     * @param context      context data to fill the template
     * @param to           recipient email addresses
     * @throws MailDeliveryException if sending fails
     */
    public void sendMailSync(String subject, String templateName, Context context, String... to)
            throws MailDeliveryException {
        log.debug("Composing email '{}' for: {} (sync)", subject, to);

        EmailMessage message = composeMessage(subject, templateName, context, to);
        smtpMailSender.sendWithoutFallback(message);

        log.debug("Email '{}' composed and sent successfully (sync)", subject);
    }

    /**
     * Composes an email message from a Thymeleaf template.
     *
     * @param subject      subject of the email
     * @param templateName name of the Thymeleaf template (without "emails/" prefix)
     * @param context      context data to fill the template
     * @param to           recipient email addresses
     * @return the composed EmailMessage
     */
    private EmailMessage composeMessage(String subject, String templateName, Context context, String... to) {
        // 1. Process template to HTML
        String htmlContent = templateEngine.process("emails/" + templateName, context);

        // 2. Create immutable email message
        return EmailMessage.of(subject, htmlContent, to);
    }
}
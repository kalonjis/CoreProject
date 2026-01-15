package be.steby.CoreProject.il.mail;

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
     * Composes and sends an email using a Thymeleaf template.
     *
     * The email content is generated from the template, then handed off
     * to SmtpMailSender which handles Circuit Breaker and Retry patterns.
     *
     * @param subject      subject of the email
     * @param templateName name of the Thymeleaf template (without "emails/" prefix)
     * @param context      context data to fill the template
     * @param to           recipient email addresses
     */
    @Async
    public void sendMail(String subject, String templateName, Context context, String... to) {
        log.debug("Composing email '{}' for: {}", subject, to);
        log.debug("Async task running on thread: {}", Thread.currentThread().getName());

        // 1. Process template to HTML
        String htmlContent = templateEngine.process("emails/" + templateName, context);

        // 2. Create immutable email message
        EmailMessage message = EmailMessage.of(subject, htmlContent, to);

        // 3. Delegate to SmtpMailSender (with Circuit Breaker + Retry)
        smtpMailSender.send(message);

        log.debug("Email '{}' composed and sent to SmtpMailSender", subject);
    }
}
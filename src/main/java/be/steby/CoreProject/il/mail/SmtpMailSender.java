package be.steby.CoreProject.il.mail;

import be.steby.CoreProject.bll.common.exceptions.mail.MailDeliveryException;
import be.steby.CoreProject.il.mail.fallback.FailedEmailHandler;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * SMTP Gateway with Circuit Breaker and Retry protection.
 *
 * This component is responsible for the actual SMTP communication.
 * It uses Resilience4j annotations to provide:
 * - Circuit Breaker: Fails fast when SMTP is down
 * - Retry: Automatic retry with exponential backoff
 * - Fallback: Delegates to FailedEmailHandler when all else fails
 *
 * Execution order:
 * 1. Method is called
 * 2. If it fails, CircuitBreaker records the failure
 * 3. Retry decides whether to retry
 * 4. After all retries exhausted (or circuit open), fallback is called
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpMailSender {

    private static final String BACKEND_NAME = "smtpBackend";

    private final JavaMailSender mailSender;
    private final FailedEmailHandler failedEmailHandler;

    @Value("${spring.mail.username}")
    private String fromAddress;

    /**
     * Sends an email with Circuit Breaker and Retry protection.
     *
     * @param message the email message to send
     * @throws MailDeliveryException if sending fails after all retries
     */
    @CircuitBreaker(name = BACKEND_NAME, fallbackMethod = "sendFallback")
    @Retry(name = BACKEND_NAME)
    public void send(EmailMessage message) {
        log.debug("Attempting to send email: subject='{}', recipients={}",
                message.subject(), message.recipients());

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(message.recipients());
            helper.setSubject(message.subject());
            helper.setText(message.htmlContent(), true);

            mailSender.send(mimeMessage);

            log.info("Email sent successfully. Subject: '{}', Recipients: {}",
                    message.subject(), message.recipients());

        } catch (MessagingException e) {
            log.error("Failed to create email message: {}", e.getMessage());
            throw new MailDeliveryException("Failed to create email: " + e.getMessage(), e);
        } catch (MailException e) {
            log.error("Failed to send email via SMTP: {}", e.getMessage());
            throw new MailDeliveryException("SMTP error: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method called when Circuit Breaker is OPEN or all retries exhausted.
     *
     * Delegates to the configured FailedEmailHandler strategy:
     * - DatabaseFailedEmailHandler: Saves to DB for scheduled retry
     * - RabbitMqFailedEmailHandler: Sends to message queue
     *
     * @param message   the email that failed to send
     * @param throwable the exception that triggered the fallback
     */
    private void sendFallback(EmailMessage message, Throwable throwable) {
        log.warn("SMTP fallback triggered. Strategy: '{}', Subject: '{}', Recipients: {}, Error: {}",
                failedEmailHandler.getStrategyName(),
                message.subject(),
                message.recipients(),
                throwable.getMessage());

        // Delegate to the configured handler (DB or RabbitMQ)
        failedEmailHandler.handle(
                message,
                throwable.getMessage(),
                throwable
        );

        // Log for monitoring/alerting
        log.info("Email queued for retry via '{}' strategy. Subject: '{}'",
                failedEmailHandler.getStrategyName(),
                message.subject());
    }
}
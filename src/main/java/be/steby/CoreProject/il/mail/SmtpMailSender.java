package be.steby.CoreProject.il.mail;

import be.steby.CoreProject.bll.common.exceptions.mail.MailDeliveryException;
import be.steby.CoreProject.il.mail.fallback.FailedEmailHandler;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
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
 * Provides two sending modes:
 * - {@link #send}: With fallback (queues for retry if delivery fails)
 * - {@link #sendWithoutFallback}: Without fallback (throws immediately on failure)
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
     * If all retries fail or circuit is open, email is queued for later retry.
     *
     * Use this for non-critical emails where delayed delivery is acceptable:
     * - Welcome emails
     * - Notification emails
     * - Marketing emails
     *
     * @param message the email message to send
     */
    @CircuitBreaker(name = BACKEND_NAME, fallbackMethod = "sendFallback")
    @Retry(name = BACKEND_NAME)
    public void send(EmailMessage message) {
        doSend(message);
    }

    /**
     * Sends an email with Circuit Breaker and Retry protection but WITHOUT fallback.
     * If delivery fails, throws immediately instead of queuing for retry.
     *
     * Use this for time-sensitive emails where delayed delivery is useless:
     * - 2FA verification codes (expire in minutes)
     * - Password reset tokens
     * - Any OTP-based authentication
     *
     * @param message the email message to send
     * @throws MailDeliveryException if sending fails after all retries or circuit is open
     */
    @CircuitBreaker(name = BACKEND_NAME, fallbackMethod = "sendWithoutFallbackFallback")
    @Retry(name = BACKEND_NAME)
    public void sendWithoutFallback(EmailMessage message) throws MailDeliveryException {
        doSend(message);
    }

    /**
     * Core email sending logic shared by both send methods.
     *
     * @param message the email message to send
     * @throws MailDeliveryException if sending fails
     */
    private void doSend(EmailMessage message) {
        log.debug("Attempting to send email: subject='{}', recipients={}",
                message.subject(), message.recipients());

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(message.recipients());
            helper.setSubject(message.subject());
            helper.setText(message.htmlContent(), true);
            if (message.replyTo() != null && !message.replyTo().isBlank()) {
                helper.setReplyTo(message.replyTo());
            }

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
     * Fallback method for {@link #send} - queues email for later retry.
     *
     * Called when Circuit Breaker is OPEN or all retries exhausted.
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

        log.info("Email queued for retry via '{}' strategy. Subject: '{}'",
                failedEmailHandler.getStrategyName(),
                message.subject());
    }

    /**
     * Fallback method for {@link #sendWithoutFallback} - throws immediately.
     *
     * Called when Circuit Breaker is OPEN or all retries exhausted.
     * Does NOT queue for retry - simply wraps and rethrows the exception.
     * This allows the caller to handle the failure (e.g., offer alternative 2FA methods).
     *
     * @param message   the email that failed to send
     * @param throwable the exception that triggered the fallback
     * @throws MailDeliveryException always thrown with details about the failure
     */
    private void sendWithoutFallbackFallback(EmailMessage message, Throwable throwable)
            throws MailDeliveryException {

        String errorMessage;

        if (throwable instanceof CallNotPermittedException) {
            errorMessage = "Email service temporarily unavailable (circuit breaker open)";
            log.warn("Circuit breaker OPEN - cannot send critical email. Subject: '{}', Recipients: {}",
                    message.subject(), message.recipients());
        } else {
            errorMessage = "Failed to send email after all retries: " + throwable.getMessage();
            log.error("Critical email delivery failed. Subject: '{}', Recipients: {}, Error: {}",
                    message.subject(), message.recipients(), throwable.getMessage());
        }

        throw new MailDeliveryException(errorMessage, throwable);
    }
}
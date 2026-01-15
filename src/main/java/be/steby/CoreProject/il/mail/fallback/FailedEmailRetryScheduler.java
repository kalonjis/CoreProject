package be.steby.CoreProject.il.mail.fallback;

import be.steby.CoreProject.dal.repositories.FailedEmailRepository;
import be.steby.CoreProject.dl.entities.FailedEmail;
import be.steby.CoreProject.dl.enums.FailedEmailStatus;
import be.steby.CoreProject.il.mail.EmailMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled job that retries failed emails stored in the database.
 *
 * IMPORTANT: This scheduler sends emails DIRECTLY via JavaMailSender,
 * bypassing the Circuit Breaker to avoid infinite fallback loops.
 *
 * Only active when using the database fallback strategy.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.mail.fallback.strategy",
        havingValue = "database",
        matchIfMissing = true
)
public class FailedEmailRetryScheduler {

    // Inject JavaMailSender directly, NOT SmtpMailSender
    private final JavaMailSender mailSender;
    private final FailedEmailRepository failedEmailRepository;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.mail.fallback.database.retry-batch-size:10}")
    private int batchSize;

    @Value("${app.mail.fallback.database.stuck-threshold-minutes:15}")
    private int stuckThresholdMinutes;

    @Value("${app.mail.fallback.database.cleanup-sent-after-days:7}")
    private int cleanupSentAfterDays;

    /**
     * Main retry job - processes failed emails ready for retry.
     */
    @Scheduled(cron = "${app.mail.fallback.database.retry-cron:0 */2 * * * *}")
    @Transactional
    public void retryFailedEmails() {
        log.debug("Starting failed email retry job...");

        List<FailedEmail> emailsToRetry = failedEmailRepository
                .findEmailsReadyForRetry(Instant.now(), batchSize);

        if (emailsToRetry.isEmpty()) {
            log.debug("No failed emails ready for retry.");
            return;
        }

        log.info("Found {} email(s) ready for retry.", emailsToRetry.size());

        for (FailedEmail failedEmail : emailsToRetry) {
            processEmail(failedEmail);
        }
    }

    /**
     * Processes a single failed email.
     */
    private void processEmail(FailedEmail failedEmail) {
        // Optimistic lock: mark as RETRYING
        int updated = failedEmailRepository.markAsRetrying(
                failedEmail.getId(),
                FailedEmailStatus.PENDING,
                Instant.now()
        );

        if (updated == 0) {
            log.debug("Email {} already being processed by another instance.", failedEmail.getId());
            return;
        }

        try {
            log.info("Retrying email ID: {}, Attempt: {}/{}",
                    failedEmail.getId(),
                    failedEmail.getRetryCount() + 1,
                    failedEmail.getMaxRetries());

            // Send directly, bypassing Circuit Breaker
            sendEmailDirectly(failedEmail);

            // Success!
            failedEmail.markAsSent();
            failedEmailRepository.save(failedEmail);

            log.info("Email {} sent successfully on retry!", failedEmail.getId());

        } catch (Exception e) {
            // Still failing - update retry count
            log.warn("Email {} retry failed: {}", failedEmail.getId(), e.getMessage());

            failedEmail.setLastFailureReason(e.getMessage());
            failedEmail.incrementRetry();
            failedEmailRepository.save(failedEmail);

            if (failedEmail.getStatus() == FailedEmailStatus.FAILED) {
                log.error("Email {} permanently failed after {} attempts. Manual intervention required.",
                        failedEmail.getId(), failedEmail.getMaxRetries());
            } else {
                log.warn("Email {} scheduled for next retry at: {}",
                        failedEmail.getId(), failedEmail.getNextRetryAt());
            }
        }
    }

    /**
     * Sends email directly using JavaMailSender.
     *
     * BYPASSES the Circuit Breaker to avoid:
     * 1. Triggering fallback again (infinite loop)
     * 2. Recording failures in Circuit Breaker metrics for retries
     *
     * @throws MessagingException if email creation fails
     * @throws MailException if SMTP send fails
     */
    private void sendEmailDirectly(FailedEmail failedEmail) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(fromAddress);
        helper.setTo(failedEmail.getRecipientsArray());
        helper.setSubject(failedEmail.getSubject());
        helper.setText(failedEmail.getHtmlContent(), true);

        // This will throw MailException if SMTP fails
        mailSender.send(mimeMessage);
    }

    /**
     * Cleanup job - removes old successfully sent emails.
     * Runs daily at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldSentEmails() {
        Instant cutoff = Instant.now().minusSeconds(cleanupSentAfterDays * 24L * 60 * 60);
        int deleted = failedEmailRepository.deleteOldSentEmails(FailedEmailStatus.SENT, cutoff);

        if (deleted > 0) {
            log.info("Cleaned up {} old sent email records.", deleted);
        }
    }

    /**
     * Recovery job - resets stuck RETRYING emails.
     * Runs every 30 minutes.
     */
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void resetStuckEmails() {
        Instant stuckSince = Instant.now().minusSeconds(stuckThresholdMinutes * 60L);
        int reset = failedEmailRepository.resetStuckEmails(stuckSince, Instant.now());

        if (reset > 0) {
            log.warn("Reset {} stuck email(s) back to PENDING status.", reset);
        }
    }
}
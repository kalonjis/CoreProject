package be.steby.CoreProject.il.sms.fallback;

import be.steby.CoreProject.bll.common.services.validation.phone.PhoneNumberFormatValidationService;
import be.steby.CoreProject.dal.repositories.FailedSmsRepository;
import be.steby.CoreProject.dl.entities.FailedSms;
import be.steby.CoreProject.dl.enums.FailedSmsStatus;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled job that retries failed SMS stored in the database.
 *
 * IMPORTANT: This scheduler sends SMS DIRECTLY via Twilio API,
 * bypassing the Circuit Breaker to avoid infinite fallback loops.
 *
 * Only active when using the database fallback strategy.
 *
 * @see DatabaseFailedSmsHandler
 * @see FailedSms
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.sms.fallback.strategy",
        havingValue = "database",
        matchIfMissing = true
)
public class FailedSmsRetryScheduler {

    private final FailedSmsRepository failedSmsRepository;
    private final PhoneNumberFormatValidationService phoneNumberFormatValidationService;

    @Value("${phone.twilio.from-number}")
    private String fromPhoneNumber;

    @Value("${phone.sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${phone.sms.simulation-mode:false}")
    private boolean simulationMode;

    @Value("${app.sms.fallback.database.retry-batch-size:10}")
    private int batchSize;

    @Value("${app.sms.fallback.database.stuck-threshold-minutes:15}")
    private int stuckThresholdMinutes;

    @Value("${app.sms.fallback.database.cleanup-sent-after-days:7}")
    private int cleanupSentAfterDays;

    /**
     * Main retry job - processes failed SMS ready for retry.
     * Runs every 2 minutes by default.
     */
    @Scheduled(cron = "${app.sms.fallback.database.retry-cron:0 */2 * * * *}")
    @Transactional
    public void retryFailedSms() {
        log.debug("Starting failed SMS retry job...");

        List<FailedSms> smsToRetry = failedSmsRepository
                .findSmsReadyForRetry(Instant.now(), batchSize);

        if (smsToRetry.isEmpty()) {
            log.debug("No failed SMS ready for retry.");
            return;
        }

        log.info("Found {} SMS ready for retry.", smsToRetry.size());

        for (FailedSms failedSms : smsToRetry) {
            processSms(failedSms);
        }
    }

    /**
     * Processes a single failed SMS.
     *
     * @param failedSms the SMS to retry
     */
    private void processSms(FailedSms failedSms) {
        // Optimistic lock: mark as RETRYING
        int updated = failedSmsRepository.markAsRetrying(
                failedSms.getId(),
                FailedSmsStatus.PENDING,
                Instant.now()
        );

        if (updated == 0) {
            log.debug("SMS {} already being processed by another instance.", failedSms.getId());
            return;
        }

        try {
            log.info("Retrying SMS ID: {}, Recipient: {}, Attempt: {}/{}",
                    failedSms.getId(),
                    failedSms.getMaskedRecipient(),
                    failedSms.getRetryCount() + 1,
                    failedSms.getMaxRetries());

            // Send directly, bypassing Circuit Breaker
            sendSmsDirectly(failedSms);

            // Success!
            failedSms.markAsSent();
            failedSmsRepository.save(failedSms);

            log.info("SMS {} sent successfully on retry!", failedSms.getId());

        } catch (Exception e) {
            // Still failing - update retry count
            log.warn("SMS {} retry failed: {}", failedSms.getId(), e.getMessage());

            failedSms.setLastFailureReason(e.getMessage());
            failedSms.incrementRetry();
            failedSmsRepository.save(failedSms);

            if (failedSms.getStatus() == FailedSmsStatus.FAILED) {
                log.error("SMS {} permanently failed after {} attempts. Manual intervention required.",
                        failedSms.getId(), failedSms.getMaxRetries());
            } else {
                log.warn("SMS {} scheduled for next retry at: {}",
                        failedSms.getId(), failedSms.getNextRetryAt());
            }
        }
    }

    /**
     * Sends SMS directly using Twilio API.
     *
     * BYPASSES the Circuit Breaker to avoid:
     * 1. Triggering fallback again (infinite loop)
     * 2. Recording failures in Circuit Breaker metrics for retries
     *
     * @param failedSms the SMS to send
     * @throws Exception if Twilio API call fails
     */
    private void sendSmsDirectly(FailedSms failedSms) throws Exception {
        if (!smsEnabled) {
            log.info("SMS disabled - simulating successful retry for SMS {}", failedSms.getId());
            return;
        }

        if (simulationMode) {
            log.info("SMS SIMULATION (retry) - TO: {} - CONTENT: '{}'",
                    failedSms.getMaskedRecipient(),
                    failedSms.getContent());
            return;
        }

        String formattedNumber = phoneNumberFormatValidationService.validateAndFormat(failedSms.getRecipient());

        Message twilioMessage = Message.creator(
                new PhoneNumber(formattedNumber),
                new PhoneNumber(fromPhoneNumber),
                failedSms.getContent()
        ).create();

        log.debug("Twilio retry response SID: {}", twilioMessage.getSid());
    }

    /**
     * Cleanup job - removes old successfully sent SMS.
     * Runs daily at 3 AM by default.
     */
    @Scheduled(cron = "${app.sms.fallback.database.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void cleanupOldSentSms() {
        log.debug("Starting SMS cleanup job...");

        Instant cutoff = Instant.now().minus(cleanupSentAfterDays, ChronoUnit.DAYS);
        int deleted = failedSmsRepository.deleteOldSentSms(FailedSmsStatus.SENT, cutoff);

        if (deleted > 0) {
            log.info("Cleaned up {} old sent SMS records.", deleted);
        } else {
            log.debug("No old SMS records to clean up.");
        }
    }

    /**
     * Recovery job - resets stuck RETRYING SMS back to PENDING.
     * Handles recovery after application crash.
     * Runs every 15 minutes by default.
     */
    @Scheduled(cron = "${app.sms.fallback.database.stuck-check-cron:0 */15 * * * *}")
    @Transactional
    public void resetStuckSms() {
        log.debug("Checking for stuck SMS...");

        Instant stuckSince = Instant.now().minus(stuckThresholdMinutes, ChronoUnit.MINUTES);
        int reset = failedSmsRepository.resetStuckSms(stuckSince, Instant.now());

        if (reset > 0) {
            log.warn("Reset {} stuck SMS back to PENDING status.", reset);
        } else {
            log.debug("No stuck SMS found.");
        }
    }
}
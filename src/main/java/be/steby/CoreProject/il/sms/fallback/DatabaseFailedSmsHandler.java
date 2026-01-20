package be.steby.CoreProject.il.sms.fallback;

import be.steby.CoreProject.dal.repositories.FailedSmsRepository;
import be.steby.CoreProject.dl.entities.FailedSms;
import be.steby.CoreProject.dl.enums.FailedSmsStatus;
import be.steby.CoreProject.il.sms.SmsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

/**
 * Database-based implementation of {@link FailedSmsHandler}.
 *
 * Stores failed SMS in the database for later retry by a scheduled job.
 * This is the simple fallback strategy suitable for small projects
 * that don't require a message broker.
 *
 * Activated when: app.sms.fallback.strategy=database
 *
 * @see FailedSms
 * @see FailedSmsRetryScheduler
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.sms.fallback.strategy",
        havingValue = "database",
        matchIfMissing = true  // Default strategy
)
public class DatabaseFailedSmsHandler implements FailedSmsHandler {

    private static final String STRATEGY_NAME = "database";
    private static final int MAX_EXCEPTION_TRACE_LENGTH = 4000;

    private final FailedSmsRepository failedSmsRepository;

    @Override
    public void handle(SmsMessage message, String reason, Throwable exception) {
        log.info("Storing failed SMS in database for retry. Recipient: {}",
                message.toString());

        try {
            FailedSms failedSms = FailedSms.builder()
                    .content(message.content())
                    .recipient(message.recipient())
                    .status(FailedSmsStatus.PENDING)
                    .retryCount(0)
                    .maxRetries(5)
                    .nextRetryAt(Instant.now().plusSeconds(60)) // First retry in 1 minute
                    .originalFailureAt(Instant.now())
                    .lastFailureReason(truncate(reason, 2000))
                    .lastExceptionTrace(extractStackTrace(exception))
                    .build();

            failedSmsRepository.save(failedSms);

            log.info("Failed SMS saved successfully. ID: {}, Recipient: {}, Next retry at: {}",
                    failedSms.getId(),
                    failedSms.getMaskedRecipient(),
                    failedSms.getNextRetryAt());

        } catch (Exception e) {
            // Last resort: log everything we can
            log.error("CRITICAL: Failed to save failed SMS to database! " +
                            "Recipient: {}, Content length: {}, Original error: {}, DB error: {}",
                    message.toString(),
                    message.content().length(),
                    reason,
                    e.getMessage(),
                    e);
        }
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    /**
     * Extracts and truncates stack trace from exception.
     *
     * @param exception the exception (may be null)
     * @return truncated stack trace string
     */
    private String extractStackTrace(Throwable exception) {
        if (exception == null) {
            return null;
        }

        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        return truncate(sw.toString(), MAX_EXCEPTION_TRACE_LENGTH);
    }

    /**
     * Truncates a string to the specified maximum length.
     *
     * @param value     the string to truncate
     * @param maxLength maximum allowed length
     * @return truncated string or original if shorter
     */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
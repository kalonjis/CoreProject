package be.steby.CoreProject.il.mail.fallback;

import be.steby.CoreProject.dal.repositories.FailedEmailRepository;
import be.steby.CoreProject.dl.entities.FailedEmail;
import be.steby.CoreProject.dl.enums.FailedEmailStatus;
import be.steby.CoreProject.il.mail.EmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

/**
 * Database-based implementation of {@link FailedEmailHandler}.
 * 
 * Stores failed emails in the database for later retry by a scheduled job.
 * This is the simple fallback strategy suitable for small projects
 * that don't require a message broker.
 * 
 * Activated when: app.mail.fallback.strategy=database
 * 
 * @see FailedEmail
 * @see FailedEmailRetryScheduler
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.mail.fallback.strategy",
        havingValue = "database",
        matchIfMissing = true  // Default strategy
)
public class DatabaseFailedEmailHandler implements FailedEmailHandler {

    private static final String STRATEGY_NAME = "database";
    private static final int MAX_EXCEPTION_TRACE_LENGTH = 4000;

    private final FailedEmailRepository failedEmailRepository;

    @Override
    public void handle(EmailMessage message, String reason, Throwable exception) {
        log.info("Storing failed email in database for retry. Subject: '{}', Recipients: {}",
                message.subject(),
                message.recipients());

        try {
            FailedEmail failedEmail = FailedEmail.builder()
                    .subject(message.subject())
                    .htmlContent(message.htmlContent())
                    .recipients(String.join(",", message.recipients()))
                    .status(FailedEmailStatus.PENDING)
                    .retryCount(0)
                    .maxRetries(5)
                    .nextRetryAt(Instant.now().plusSeconds(60)) // First retry in 1 minute
                    .originalFailureAt(Instant.now())
                    .lastFailureReason(truncate(reason, 2000))
                    .lastExceptionTrace(extractStackTrace(exception))
                    .build();

            failedEmailRepository.save(failedEmail);

            log.info("Failed email saved successfully. ID: {}, Next retry at: {}",
                    failedEmail.getId(),
                    failedEmail.getNextRetryAt());

        } catch (Exception e) {
            // Last resort: log everything we can
            log.error("CRITICAL: Failed to save failed email to database! " +
                            "Subject: '{}', Recipients: {}, Original error: {}, Save error: {}",
                    message.subject(),
                    message.recipients(),
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
     * Truncates string to maximum length.
     */
    private String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}
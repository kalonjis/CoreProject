package be.steby.CoreProject.il.mail.fallback;

import be.steby.CoreProject.il.mail.EmailMessage;

/**
 * Strategy interface for handling failed email deliveries.
 * 
 * Implementations can store failed emails in different backends:
 * - Database (simple, for small projects)
 * - RabbitMQ (scalable, for production)
 * - Other message brokers (Kafka, SQS, etc.)
 * 
 * Usage:
 * Configure the active strategy via application.yml:
 * <pre>
 * app:
 *   mail:
 *     fallback:
 *       strategy: database  # or "rabbitmq"
 * </pre>
 * 
 * @see DatabaseFailedEmailHandler
 * @see RabbitMqFailedEmailHandler
 */
public interface FailedEmailHandler {

    /**
     * Handles a failed email by storing it for later retry.
     * 
     * @param message   the email that failed to send
     * @param reason    the failure reason/exception message
     * @param exception the original exception (may be null)
     */
    void handle(EmailMessage message, String reason, Throwable exception);

    /**
     * Returns the strategy name for logging purposes.
     * 
     * @return strategy identifier (e.g., "database", "rabbitmq")
     */
    String getStrategyName();
}
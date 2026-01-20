package be.steby.CoreProject.il.sms.fallback;

import be.steby.CoreProject.il.sms.SmsMessage;

/**
 * Strategy interface for handling failed SMS deliveries.
 *
 * Implementations can store failed SMS in different backends:
 * - Database (simple, for small projects)
 * - RabbitMQ (scalable, for production)
 * - Other message brokers (Kafka, SQS, etc.)
 *
 * Usage:
 * Configure the active strategy via application.yml:
 * <pre>
 * app:
 *   sms:
 *     fallback:
 *       strategy: database  # or "rabbitmq"
 * </pre>
 *
 * @see DatabaseFailedSmsHandler
 */
public interface FailedSmsHandler {

    /**
     * Handles a failed SMS by storing it for later retry.
     *
     * @param message   the SMS that failed to send
     * @param reason    the failure reason/exception message
     * @param exception the original exception (may be null)
     */
    void handle(SmsMessage message, String reason, Throwable exception);

    /**
     * Returns the strategy name for logging purposes.
     *
     * @return strategy identifier (e.g., "database", "rabbitmq")
     */
    String getStrategyName();
}
package be.steby.CoreProject.dl.enums;

/**
 * Status of a failed SMS in the retry queue.
 *
 * Lifecycle:
 * PENDING → RETRYING → SENT (success)
 *                   → PENDING (retry later)
 *                   → FAILED (max retries exceeded)
 *
 * @see be.steby.CoreProject.dl.entities.FailedSms
 */
public enum FailedSmsStatus {

    /**
     * SMS is waiting to be retried.
     * Initial state after fallback saves the SMS.
     */
    PENDING,

    /**
     * SMS is currently being processed by the retry scheduler.
     * Used as optimistic lock to prevent duplicate processing.
     */
    RETRYING,

    /**
     * SMS was successfully sent on retry.
     * Terminal state - can be cleaned up after retention period.
     */
    SENT,

    /**
     * SMS permanently failed after exhausting all retry attempts.
     * Terminal state - requires manual intervention or investigation.
     */
    FAILED
}
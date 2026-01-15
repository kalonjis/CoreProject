package be.steby.CoreProject.dl.enums;

/**
 * Status lifecycle for failed emails awaiting retry.
 * 
 * Lifecycle flow:
 * <pre>
 * PENDING ──► RETRYING ──► SENT ✓
 *                │
 *                └──► FAILED (max retries exceeded)
 * </pre>
 * 
 * A scheduled job processes emails in PENDING status
 * when their nextRetryAt timestamp has passed.
 */
public enum FailedEmailStatus {

    /**
     * Email is waiting to be retried.
     * Initial state when an email fails to send.
     */
    PENDING,

    /**
     * Email is currently being processed by the retry job.
     * Prevents concurrent processing of the same email.
     */
    RETRYING,

    /**
     * Email was successfully sent on a retry attempt.
     * Terminal success state.
     */
    SENT,

    /**
     * Email failed after maximum retry attempts.
     * Terminal failure state - requires manual intervention.
     */
    FAILED
}
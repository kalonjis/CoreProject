package be.steby.CoreProject.il.mail;

import be.steby.CoreProject.bll.common.exceptions.InvalidArgumentException;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;

/**
 * Immutable email message ready to be sent.
 * 
 * This record contains the fully processed email data (HTML already rendered).
 * It is serializable, making it suitable for:
 * - Fallback persistence to database
 * - Queue systems (RabbitMQ, etc.)
 * - Retry mechanisms
 * 
 * Separation of Concerns:
 * - MailerService: Business logic, decides WHAT to send
 * - MailerUtil: Template processing, creates EmailMessage
 * - SmtpMailSender: Delivery with resilience, sends EmailMessage
 */
public record EmailMessage(
        String subject,
        String htmlContent,
        String replyTo,
        String[] recipients,
        Instant createdAt
    ) implements Serializable {

    /**
     * Compact constructor with validation.
     *
     * <p>{@code replyTo} is optional — {@code null} means the mail client will use
     * the system {@code From} address as the reply target.</p>
     */
    public EmailMessage {
        if (subject == null || subject.isBlank()) {
            throw new InvalidArgumentException("Email subject cannot be null or blank");
        }
        if (htmlContent == null || htmlContent.isBlank()) {
            throw new InvalidArgumentException("Email content cannot be null or blank");
        }
        if (recipients == null || recipients.length == 0) {
            throw new InvalidArgumentException("Email must have at least one recipient");
        }
        // Defensive copy of array
        recipients = Arrays.copyOf(recipients, recipients.length);
    }

    /**
     * Factory method — no Reply-To (system From address used).
     */
    public static EmailMessage of(String subject, String htmlContent, String... recipients) {
        return new EmailMessage(subject, htmlContent, null, recipients, Instant.now());
    }

    /**
     * Factory method with explicit Reply-To address.
     *
     * <p>Use for human-initiated emails (e.g., CRM outreach) where replies should
     * go to a specific person rather than the system no-reply address.</p>
     *
     * @param replyTo the address replies should be directed to (e.g., commercial's email)
     */
    public static EmailMessage withReplyTo(String subject, String htmlContent, String replyTo, String... recipients) {
        return new EmailMessage(subject, htmlContent, replyTo, recipients, Instant.now());
    }

    /**
     * Returns a copy of recipients (defensive).
     */
    @Override
    public String[] recipients() {
        return Arrays.copyOf(recipients, recipients.length);
    }

    @Override
    public String toString() {
        return "EmailMessage{" +
                "subject='" + subject + '\'' +
                ", recipients=" + Arrays.toString(recipients) +
                ", createdAt=" + createdAt +
                ", contentLength=" + htmlContent.length() +
                '}';
    }
}
package be.steby.CoreProject.il.sms;

import be.steby.CoreProject.bll.common.exceptions.InvalidArgumentException;

import java.io.Serializable;
import java.time.Instant;

/**
 * Immutable SMS message ready to be sent.
 *
 * This record contains the fully processed SMS data.
 * It is serializable, making it suitable for:
 * - Fallback persistence to database
 * - Queue systems (RabbitMQ, etc.)
 * - Retry mechanisms
 *
 * Separation of Concerns:
 * - Domain Services: Business logic, decides WHAT to send
 * - TwilioSmsSender: Delivery with resilience, sends SmsMessage
 *
 * Note: Unlike EmailMessage, SMS has a single recipient (Twilio API constraint)
 * and no subject (SMS are plain text messages).
 */
public record SmsMessage(
        String content,
        String recipient,
        Instant createdAt
) implements Serializable {

    private static final int MAX_SMS_LENGTH = 1600; // Twilio limit for concatenated SMS

    /**
     * Compact constructor with validation.
     */
    public SmsMessage {
        if (content == null || content.isBlank()) {
            throw new InvalidArgumentException("SMS content cannot be null or blank");
        }
        if (content.length() > MAX_SMS_LENGTH) {
            throw new InvalidArgumentException("SMS content exceeds maximum length of " + MAX_SMS_LENGTH + " characters");
        }
        if (recipient == null || recipient.isBlank()) {
            throw new InvalidArgumentException("SMS recipient cannot be null or blank");
        }
    }

    /**
     * Factory method for convenience.
     *
     * @param content   the SMS text content
     * @param recipient the recipient phone number (E.164 format recommended)
     * @return a new SmsMessage instance
     */
    public static SmsMessage of(String content, String recipient) {
        return new SmsMessage(content, recipient, Instant.now());
    }

    /**
     * Returns the number of SMS segments this message will use.
     * Single SMS: up to 160 chars, Multi-part: 153 chars per segment.
     *
     * @return the number of SMS segments
     */
    public int getSegmentCount() {
        int length = content.length();
        if (length <= 160) {
            return 1;
        }
        return (length + 152) / 153; // Ceiling division
    }

    @Override
    public String toString() {
        return "SmsMessage{" +
                "recipient='" + maskRecipient() + '\'' +
                ", contentLength=" + content.length() +
                ", segments=" + getSegmentCount() +
                ", createdAt=" + createdAt +
                '}';
    }

    /**
     * Masks the recipient phone number for secure logging.
     */
    private String maskRecipient() {
        if (recipient.length() < 6) {
            return "****";
        }
        return recipient.substring(0, 6) + "****";
    }
}
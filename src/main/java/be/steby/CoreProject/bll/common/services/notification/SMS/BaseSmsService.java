package be.steby.CoreProject.bll.common.services.notification.sms;

import be.steby.CoreProject.bll.common.exceptions.phone.SmsSendingException;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.sms.SmsMessage;
import be.steby.CoreProject.il.sms.TwilioSmsSender;
import lombok.RequiredArgsConstructor;

/**
 * Base class for all domain-specific SMS services.
 * Provides common utilities for SMS sending such as username formatting,
 * message templating, and phone number validation.
 *
 * This class follows the same pattern as BaseMailerService but for SMS operations.
 * Domain-specific SMS services should extend this class to inherit common functionality.
 *
 * Provides two sending modes:
 * - {@link #sendSms}: With fallback (queues for retry if delivery fails)
 * - {@link #sendSmsSync}: Without fallback (throws immediately on failure)
 *
 * Architecture:
 * DomainSmsService → BaseSmsService → TwilioSmsSender → Twilio API
 *                    (this class)     (resilience)
 *
 * @author Steby Team
 * @since 2.0.0
 */
@RequiredArgsConstructor
public abstract class BaseSmsService {

    protected final TwilioSmsSender twilioSmsSender;

    /**
     * Formats a message using the configured template.
     * Replaces placeholders in the template with actual values.
     *
     * Template examples:
     * - "Your code: {code}" → "Your code: 123456"
     * - "Alert: {message}" → "Alert: Suspicious login detected"
     *
     * @param template     The message template
     * @param placeholders Key-value pairs for placeholder replacement
     * @return The formatted message
     */
    protected String formatMessage(String template, String... placeholders) {
        String result = template;

        // Replace placeholders in pairs (key, value)
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String placeholder = "{" + placeholders[i] + "}";
            String value = placeholders[i + 1];
            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * Sends an SMS with Circuit Breaker and Retry protection.
     * If delivery fails, the SMS is queued for later retry.
     *
     * Use for non-critical SMS where delayed delivery is acceptable:
     * - Security notifications
     * - Marketing SMS
     * - General alerts
     *
     * @param message     The SMS content to send
     * @param phoneNumber The recipient's phone number
     */
    protected void sendSms(String message, String phoneNumber) {
        SmsMessage smsMessage = SmsMessage.of(message, phoneNumber);
        twilioSmsSender.send(smsMessage);
    }

    /**
     * Sends an SMS to multiple recipients with Circuit Breaker and Retry protection.
     * Each recipient receives a separate SMS (Twilio API constraint).
     *
     * @param message      The SMS content to send
     * @param phoneNumbers The recipients' phone numbers
     */
    protected void sendSms(String message, String... phoneNumbers) {
        for (String phoneNumber : phoneNumbers) {
            sendSms(message, phoneNumber);
        }
    }

    /**
     * Sends an SMS synchronously without fallback.
     * If delivery fails, throws immediately instead of queuing for retry.
     *
     * Use for time-sensitive SMS where delayed delivery is useless:
     * - 2FA verification codes (expire in minutes)
     * - Password reset codes
     * - Any OTP-based authentication
     *
     * @param message     The SMS content to send
     * @param phoneNumber The recipient's phone number
     * @throws SmsSendingException if sending fails after all retries or circuit is open
     */
    protected void sendSmsSync(String message, String phoneNumber) throws SmsSendingException {
        SmsMessage smsMessage = SmsMessage.of(message, phoneNumber);
        twilioSmsSender.sendWithoutFallback(smsMessage);
    }

    /**
     * Masks phone number for secure logging.
     * Shows only the first few digits and country code.
     *
     * @param phoneNumber The phone number to mask
     * @return Masked phone number for logging
     */
    protected String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 6) {
            return "****";
        }

        // Keep first 6 characters (usually country code + few digits), mask the rest
        int visibleLength = Math.min(6, phoneNumber.length() - 4);
        String visiblePart = phoneNumber.substring(0, visibleLength);
        String maskedPart = "X".repeat(phoneNumber.length() - visibleLength);

        return visiblePart + maskedPart;
    }

    /**
     * Validates that a user has a valid phone number for SMS operations.
     * This is a common validation that most domain services will need.
     *
     * @param user The user to validate
     * @return true if user has a valid phone number, false otherwise
     */
    protected boolean hasValidPhoneNumber(User user) {
        return user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty();
    }

    /**
     * Validates that a user has a verified phone number for SMS operations.
     * More strict validation for sensitive operations like 2FA.
     *
     * @param user The user to validate
     * @return true if user has a verified phone number, false otherwise
     */
    protected boolean hasVerifiedPhoneNumber(User user) {
        return hasValidPhoneNumber(user) && user.isPhoneNumberVerified();
    }
}
package be.steby.CoreProject.bll.common.services.notification.sms;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.utils.SmsUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

/**
 * Base class for all domain-specific SMS services.
 * Provides common utilities for SMS sending such as username formatting,
 * message templating, and time formatting.
 *
 * This class follows the same pattern as BaseMailerService but for SMS operations.
 * Domain-specific SMS services should extend this class to inherit common functionality.
 *
 * @author Steby Team
 * @since 2.0.0
 */
@RequiredArgsConstructor
public abstract class BaseSmsService {

    protected final SmsUtil smsUtil;


    /**
     * Formats a message using the configured template.
     * Replaces placeholders in the template with actual values.
     *
     * Template examples:
     * - "Your code: {code}" → "Your code: 123456"
     * - "Alert: {message}" → "Alert: Suspicious login detected"
     *
     * @param template The message template
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
     * Sends an SMS using the SMS utility.
     * This is the main method that domain services should use for sending SMS.
     *
     * @param message The SMS content to send
     * @param phoneNumber The recipient's notification number
     */
    protected void sendSms(String message, String phoneNumber) {
        smsUtil.sendSms(message, phoneNumber);
    }

    /**
     * Sends an SMS to multiple recipients.
     *
     * @param message The SMS content to send
     * @param phoneNumbers The recipients' notification numbers
     */
    protected void sendSms(String message, String... phoneNumbers) {
        smsUtil.sendSms(message, phoneNumbers);
    }



    /**
     * Masks notification number for secure logging.
     * Shows only the first few digits and country code.
     *
     * @param phoneNumber The notification number to mask
     * @return Masked notification number for logging
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
     * Validates that a user has a valid notification number for SMS operations.
     * This is a common validation that most domain services will need.
     *
     * @param user The user to validate
     * @return true if user has a valid notification number, false otherwise
     */
    protected boolean hasValidPhoneNumber(User user) {
        return user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty();
    }

    /**
     * Validates that a user has a verified notification number for SMS operations.
     * More strict validation for sensitive operations like 2FA.
     *
     * @param user The user to validate
     * @return true if user has a verified notification number, false otherwise
     */
    protected boolean hasVerifiedPhoneNumber(User user) {
        return hasValidPhoneNumber(user) && user.isPhoneNumberVerified();
    }


}
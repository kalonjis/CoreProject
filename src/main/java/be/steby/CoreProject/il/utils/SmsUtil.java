package be.steby.CoreProject.il.utils;

import be.steby.CoreProject.bll.common.exceptions.phone.InvalidPhoneNumberFormatException;
import be.steby.CoreProject.bll.common.exceptions.phone.SmsSendingException;
import be.steby.CoreProject.bll.common.exceptions.phone.SmsMessageException;
import be.steby.CoreProject.bll.common.services.validation.phone.PhoneNumberFormatValidationService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Low-level SMS utility for sending text messages via Twilio.
 *
 * This utility is responsible ONLY for:
 * - Twilio SDK initialization
 * - Basic SMS sending with SMS number validation
 * - SMS length validation and warnings
 * - Async SMS delivery
 *
 * This class follows KISS & SoC principles:
 * - No business logic (templates, formatting, etc.)
 * - No domain-specific knowledge
 * - Single responsibility: send SMS messages
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SmsUtil {

    private final PhoneNumberFormatValidationService phoneNumberFormatValidationService;

    // SMS length constants
    private static final int SMS_SINGLE_LENGTH = 160;
    private static final int SMS_SEGMENT_LENGTH = 153; // Multi-part SMS segment size

    @Value("${phone.twilio.account-sid}")
    private String accountSid;

    @Value("${phone.twilio.auth-token}")
    private String authToken;

    @Value("${phone.twilio.from-number}")
    private String fromPhoneNumber;

    @Value("${phone.sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${phone.sms.simulation-mode:false}")
    private boolean simulationMode;

    /**
     * Initialize Twilio SDK with credentials after bean construction.
     */
    @PostConstruct
    public void initTwilio() {
        if (smsEnabled) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio initialized successfully with from number: {}", fromPhoneNumber);
        } else {
            log.info("SMS sending is disabled");
        }
    }

    /**
     * Sends an SMS message to a single recipient.
     *
     * This is the core SMS sending method. All other SMS operations
     * should ultimately use this method.
     *
     * @param message The SMS content to send
     * @param phoneNumber The recipient's SMS number
     * @throws SmsSendingException if SMS sending fails
     * @throws InvalidPhoneNumberFormatException if SMS number is invalid
     * @throws SmsMessageException if message content is invalid
     */
    @Async("smsExecutor")
    public void sendSms(String message, String phoneNumber) {
        log.debug("Sending SMS to: {}", maskPhoneNumber(phoneNumber));

        validateMessage(message);

        if (!smsEnabled) {
            log.info("SMS disabled - would send message to: {}", maskPhoneNumber(phoneNumber));
            return;
        }

        if (simulationMode) {
            simulateSms(message, phoneNumber);
            return;
        }

        sendViaTwilio(message, phoneNumber);
    }

    /**
     * Sends an SMS message to multiple recipients.
     *
     * @param message The SMS content to send
     * @param phoneNumbers The recipients' SMS numbers
     * @throws SmsSendingException if any SMS sending fails
     * @throws InvalidPhoneNumberFormatException if any SMS number is invalid
     * @throws SmsMessageException if message content is invalid
     */
    @Async("smsExecutor")
    public void sendSms(String message, String... phoneNumbers) {
        log.debug("Sending SMS to {} recipients", phoneNumbers.length);

        for (String phoneNumber : phoneNumbers) {
            sendSms(message, phoneNumber);
        }
    }

    /**
     * Validates SMS message content and logs warnings for long messages.
     *
     * @param message The message to validate
     * @throws SmsMessageException if message is null or empty
     */
    private void validateMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new SmsMessageException("SMS message cannot be null or empty");
        }

        int messageLength = message.length();

        if (messageLength <= SMS_SINGLE_LENGTH) {
            log.debug("SMS message length: {} characters (single SMS)", messageLength);
        } else {
            int segments = calculateSegmentCount(messageLength);
            log.warn("SMS message length: {} characters - will be split into {} segments. " +
                    "Consider shortening message to reduce costs.", messageLength, segments);
        }
    }

    /**
     * Calculates the number of SMS segments required for a message.
     *
     * @param messageLength The length of the message
     * @return The number of SMS segments required
     */
    private int calculateSegmentCount(int messageLength) {
        if (messageLength <= SMS_SINGLE_LENGTH) {
            return 1;
        }
        return (messageLength + SMS_SEGMENT_LENGTH - 1) / SMS_SEGMENT_LENGTH; // Ceiling division
    }

    /**
     * Simulates SMS sending for development/testing purposes.
     *
     * @param message The message that would be sent
     * @param phoneNumber The SMS number that would receive the message
     * @throws InvalidPhoneNumberFormatException if SMS number is invalid
     */
    private void simulateSms(String message, String phoneNumber) {
        String formattedNumber = phoneNumberFormatValidationService.validateAndFormat(phoneNumber);
        String displayNumber = phoneNumberFormatValidationService.formatForDisplay(formattedNumber);

        log.info("SMS SIMULATION - TO: {} - MESSAGE: '{}'", displayNumber, message);

        // Simulate some processing time (ignore interruption, it's just simulation)
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("SMS simulation sleep interrupted (non-critical)");
        }
    }

    /**
     * Sends SMS via Twilio API.
     *
     * @param message The message to send
     * @param phoneNumber The recipient's SMS number
     * @throws SmsSendingException if Twilio API call fails
     * @throws InvalidPhoneNumberFormatException if SMS number is invalid
     */
    private void sendViaTwilio(String message, String phoneNumber) {
        String formattedNumber = phoneNumberFormatValidationService.validateAndFormat(phoneNumber);
        String displayNumber = phoneNumberFormatValidationService.formatForDisplay(formattedNumber);

        log.debug("Sending SMS via Twilio to: {}", displayNumber);

        try {
            Message twilioMessage = Message.creator(
                    new PhoneNumber(formattedNumber),
                    new PhoneNumber(fromPhoneNumber),
                    message
            ).create();

            log.info("SMS sent successfully - TO: {} - SID: {}", displayNumber, twilioMessage.getSid());

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", maskPhoneNumber(phoneNumber), e.getMessage());
            throw new SmsSendingException("Failed to send SMS to " + maskPhoneNumber(phoneNumber) + ": " + e.getMessage(), e);
        }
    }

    /**
     * Masks SMS number for secure logging.
     * Shows only the first few digits and country code.
     *
     * @param phoneNumber The SMS number to mask
     * @return Masked SMS number for logging
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 6) {
            return "****";
        }

        // Keep first 6 characters (usually country code + few digits), mask the rest
        int visibleLength = Math.min(6, phoneNumber.length() - 4);
        String visiblePart = phoneNumber.substring(0, visibleLength);
        String maskedPart = "X".repeat(phoneNumber.length() - visibleLength);

        return visiblePart + maskedPart;
    }
}
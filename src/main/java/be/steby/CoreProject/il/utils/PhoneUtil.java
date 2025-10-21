package be.steby.CoreProject.il.utils;

import be.steby.CoreProject.bll.common.exceptions.phone.InvalidPhoneNumberFormatException;
import be.steby.CoreProject.bll.common.exceptions.phone.SmsSendingException;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class PhoneUtil {

    private final PhoneNumberFormatValidationService phoneNumberFormatValidationService;

    /**
     * Twilio account SID for authentication.
     */
    @Value("${phone.twilio.account-sid}")
    private String accountSid;

    /**
     * Twilio auth token for authentication.
     */
    @Value("${phone.twilio.auth-token}")
    private String authToken;

    /**
     * Phone number used as the sender for all outgoing SMS.
     */
    @Value("${phone.twilio.from-number}")
    private String fromPhoneNumber;

    /**
     * Flag to enable/disable SMS sending.
     */
    @Value("${phone.sms.enabled:true}")
    private boolean smsEnabled;

    /**
     * Flag to enable simulation mode (for development).
     */
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
     * Sends an SMS with a given message to specified recipient(s).
     * The SMS is sent asynchronously using Twilio SDK.
     *
     * @param message   Content of the SMS
     * @param to        Recipients of the SMS
     */
    @Async
    public void sendSms(String message, String... to) {
        log.info("Async SMS task is running on thread: {}", Thread.currentThread().getName());

        if (!smsEnabled) {
            log.info("SMS disabled - would send message '{}' to: {}", message, (Object[]) to);
            return;
        }

        if (simulationMode) {
            log.info("SMS SIMULATION MODE - simulating send to: {}", (Object[]) to);
            for (String phoneNumber : to) {
                try {
                    String formattedNumber = phoneNumberFormatValidationService.validateAndFormat(phoneNumber);
                    log.info("SMS SIMULATED successfully to: {} - Message: {}",
                            phoneNumberFormatValidationService.formatForDisplay(formattedNumber),
                            message);
                } catch (InvalidPhoneNumberFormatException e) {
                    log.error("Invalid phone number format {}: {}", phoneNumber, e.getMessage());
                    throw new SmsSendingException("Invalid phone number format: " + e.getMessage());
                }
            }
            return;
        }

        for (String phoneNumber : to) {
            try {
                // Valider et formater le numéro de téléphone
                String formattedNumber = phoneNumberFormatValidationService.validateAndFormat(phoneNumber);
                log.debug("Sending SMS to formatted number: {}", formattedNumber);

                Message twilioMessage = Message.creator(
                        new PhoneNumber(formattedNumber),
                        new PhoneNumber(fromPhoneNumber),
                        message
                ).create();

                log.info("SMS sent successfully to: {} - SID: {}",
                        phoneNumberFormatValidationService.formatForDisplay(formattedNumber),
                        twilioMessage.getSid());

            } catch (InvalidPhoneNumberFormatException e) {
                log.error("Invalid phone number format {}: {}", phoneNumber, e.getMessage());
                throw new SmsSendingException("Invalid phone number format: " + e.getMessage());
            } catch (Exception e) {
                log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
                throw new SmsSendingException("Failed to send SMS to " + phoneNumber + ": " + e.getMessage());
            }
        }
    }

    /**
     * Sends an SMS using a template with placeholders.
     * Template placeholders are replaced with actual values.
     *
     * @param template  SMS template with placeholders (e.g., "Code: {code}")
     * @param message   Actual message content to replace {message} placeholder
     * @param to        Recipients of the SMS
     */
    @Async
    public void sendTemplatedSms(String template, String message, String... to) {
        String processedMessage = template.replace("{message}", message);
        sendSms(processedMessage, to);
    }

    /**
     * Sends a 2FA code SMS using the configured template.
     *
     * @param code  The verification code to send
     * @param to    Recipients of the SMS
     */
    @Async
    public void send2FACode(String code, String... to) {
        // Using the template from phone.yml: "Votre code de vérification : {code}"
        String template = "Votre code de vérification : {code}";
        String message = template.replace("{code}", code);
        sendSms(message, to);
    }
}
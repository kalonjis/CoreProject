package be.steby.CoreProject.bll.domains.profile.services.notification;

import be.steby.CoreProject.bll.common.exceptions.phone.PhoneException;
import be.steby.CoreProject.bll.domains.profile.events.PhoneVerificationInitiatedEvent;
import be.steby.CoreProject.il.utils.SmsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for sending SMS verification notifications via SMS.
 * Extends BasePhoneService to inherit common SMS utilities.
 * 
 * This service is responsible for:
 * - Sending SMS verification codes via SMS
 * - Formatting verification messages
 * - Handling SMS sending errors
 */
@Slf4j
@Service
public class SmsNotificationService extends be.steby.CoreProject.bll.common.services.notification.sms.BaseSmsService {

    public SmsNotificationService(SmsUtil smsUtil) {
        super(smsUtil);
    }

    /**
     * Sends SMS verification code via SMS.
     * Uses the phone number and verification code from the event.
     * 
     * @param event The SMS verification initiated event containing phone number and code
     * @throws PhoneException if SMS sending fails
     */
    public void sendPhoneVerification(PhoneVerificationInitiatedEvent event) {
        log.info("Sending phone number verification SMS to: {}", maskPhoneNumber(event.phoneNumber()));
        
        try {
            // Format the verification message
            String message = formatVerificationMessage(event.verificationCode());
            
            // Use inherited method from BasePhoneService
            sendSms(message, event.phoneNumber());
            
            log.info("Phone verification SMS sent successfully to: {}", 
                    maskPhoneNumber(event.phoneNumber()));
            
        } catch (Exception e) {
            log.error("Failed to send SMS verification SMS to: {}",
                     maskPhoneNumber(event.phoneNumber()), e);
            throw new PhoneException("Failed to send verification SMS", e);
        }
    }

    /**
     * Formats the verification message for SMS.
     * Creates a user-friendly message containing the verification code.
     * 
     * @param verificationCode The 6-digit verification code
     * @return Formatted SMS message
     */
    private String formatVerificationMessage(String verificationCode) {
        return String.format("Your verification code: %s", verificationCode);
    }

}
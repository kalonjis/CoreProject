package be.steby.CoreProject.bll.domains.profile.services.notification;

import be.steby.CoreProject.bll.common.exceptions.phone.PhoneException;
import be.steby.CoreProject.bll.common.services.notification.phone.BasePhoneService;
import be.steby.CoreProject.bll.domains.profile.events.PhoneVerificationInitiatedEvent;
import be.steby.CoreProject.il.utils.PhoneUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for sending phone verification notifications via SMS.
 * Extends BasePhoneService to inherit common phone utilities.
 * 
 * This service is responsible for:
 * - Sending phone verification codes via SMS
 * - Formatting verification messages
 * - Handling SMS sending errors
 */
@Slf4j
@Service
public class PhoneNotificationService extends BasePhoneService {

    public PhoneNotificationService(PhoneUtil phoneUtil) {
        super(phoneUtil);
    }

    /**
     * Sends phone verification code via SMS.
     * Uses the phone number and verification code from the event.
     * 
     * @param event The phone verification initiated event containing phone and code
     * @throws PhoneException if SMS sending fails
     */
    public void sendPhoneVerification(PhoneVerificationInitiatedEvent event) {
        log.info("Sending phone verification SMS to: {}", maskPhoneNumber(event.phoneNumber()));
        
        try {
            // Format the verification message
            String message = formatVerificationMessage(event.verificationCode());
            
            // Use inherited method from BasePhoneService
            sendSms(message, event.phoneNumber());
            
            log.info("Phone verification SMS sent successfully to: {}", 
                    maskPhoneNumber(event.phoneNumber()));
            
        } catch (Exception e) {
            log.error("Failed to send phone verification SMS to: {}", 
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

    /**
     * Masks phone number for logging purposes.
     * Shows first few digits and country code, masks the rest.
     * 
     * @param phoneNumber The phone number to mask
     * @return Masked phone number safe for logging
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 6) {
            return "****";
        }
        
        int maskLength = Math.min(4, phoneNumber.length() - 4);
        String visiblePart = phoneNumber.substring(0, phoneNumber.length() - maskLength);
        String maskedPart = "X".repeat(maskLength);
        
        return visiblePart + maskedPart;
    }
}
package be.steby.CoreProject.bll.domains.profile.listeners;

import be.steby.CoreProject.bll.domains.profile.events.PhoneVerificationInitiatedEvent;
import be.steby.CoreProject.bll.domains.profile.services.notification.PhoneNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for profile-related notification events.
 * Handles events related to user profile notifications such as phone verification.
 * 
 * This listener processes:
 * - Phone verification initiated events
 * - Future profile-related email events
 * - Other profile notification events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileNotificationListener {

    private final PhoneNotificationService phoneNotificationService;

    /**
     * Handles phone verification initiated events.
     * Sends SMS verification code to the user's phone number.
     * 
     * @param event The phone verification initiated event
     */
    @Async
    @EventListener
    public void handle(PhoneVerificationInitiatedEvent event) {
        log.debug("Received PhoneVerificationInitiatedEvent for phone: {}", 
                 maskPhoneNumber(event.phoneNumber()));
        
        try {
            phoneNotificationService.sendPhoneVerification(event);
            log.debug("Successfully processed PhoneVerificationInitiatedEvent");
        } catch (Exception e) {
            log.error("Failed to process PhoneVerificationInitiatedEvent for phone: {}", 
                     maskPhoneNumber(event.phoneNumber()), e);
        }
    }

    /**
     * Masks phone number for secure logging.
     * 
     * @param phoneNumber The phone number to mask
     * @return Masked phone number
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
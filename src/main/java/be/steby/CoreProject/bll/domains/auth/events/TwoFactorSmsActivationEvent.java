
package be.steby.CoreProject.bll.domains.auth.events;

import be.steby.CoreProject.dl.entities.User;

/**
 * Event published when SMS 2FA activation is initiated.
 *
 * This event triggers sending the verification code via SMS to the user's
 * phone number. The code is sent in plain text for the SMS message.
 *
 * Published by: SmsTwoFactorServiceImpl.initiateActivation()
 * Handled by: TwoFactorNotificationListener
 *
 * @param user The user initiating SMS 2FA activation
 * @param verificationCode The plain 6-digit verification code to send
 * @param phoneNumber The phone number to send the SMS to
 *
 * @author Steby Team
 * @since 2.0.0
 */
public record TwoFactorSmsActivationEvent(
        User user,
        String verificationCode,
        String phoneNumber
) {
    /**
     * Validates event data.
     */
    public TwoFactorSmsActivationEvent {
        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }
        if (verificationCode == null || verificationCode.isBlank()) {
            throw new IllegalArgumentException("Verification code is required");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }
    }
}